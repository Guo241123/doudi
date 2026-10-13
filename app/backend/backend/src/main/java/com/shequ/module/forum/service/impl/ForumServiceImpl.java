package com.shequ.module.forum.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shequ.common.exception.BusinessException;
import com.shequ.common.favorite.Favorite;
import com.shequ.common.favorite.FavoriteService;
import com.shequ.common.like.LikeRecord;
import com.shequ.common.like.LikeService;
import com.shequ.common.media.Media;
import com.shequ.common.media.MediaService;
import com.shequ.common.result.CursorResult;
import com.shequ.common.result.ErrorCode;
import com.shequ.common.sensitive.SensitiveWordService;
import com.shequ.common.storage.FileUrlResolver;
import com.shequ.module.forum.dto.CommentPublishDTO;
import com.shequ.module.forum.dto.PostPublishDTO;
import com.shequ.module.forum.entity.Comment;
import com.shequ.module.forum.entity.Post;
import com.shequ.module.forum.entity.PostStatus;
import com.shequ.module.forum.mapper.CommentMapper;
import com.shequ.module.forum.mapper.PostMapper;
import com.shequ.module.forum.service.ForumService;
import com.shequ.module.forum.vo.CommentVO;
import com.shequ.module.forum.vo.FeedItemVO;
import com.shequ.module.forum.vo.HotCommentVO;
import com.shequ.module.forum.vo.LikeResultVO;
import com.shequ.module.forum.vo.MediaVO;
import com.shequ.module.forum.vo.MyPostItemVO;
import com.shequ.module.forum.vo.PostDetailVO;
import com.shequ.module.forum.vo.PostPublishVO;
import com.shequ.module.forum.vo.ReplyToVO;
import com.shequ.module.report.service.BlacklistService;
import com.shequ.module.user.entity.User;
import com.shequ.module.user.service.UserService;
import com.shequ.module.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 论坛服务实现（接口文档 2.6 #27～#35）。
 * <p>
 * 分页口径与其它模块一致：{@code WHERE id < ? ORDER BY id DESC LIMIT size+1}。
 * 列表页作者 / 媒体 / 点赞态 / 高赞评论全部批量装配，避免 N+1。
 * <p>
 * 帖子删除用 {@code status=DELETED} 标记（数据库设计 §8 的 idx_status_id 即为此设计），
 * 所有可见性查询统一走 {@link #VISIBLE_STATUSES}，不会泄露已删帖子。
 */
@Service
@RequiredArgsConstructor
public class ForumServiceImpl implements ForumService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    /** 可见帖子状态：已发布、公告（已删除不在其中） */
    private static final List<Integer> VISIBLE_STATUSES =
            List.of(PostStatus.PUBLISHED.code(), PostStatus.ANNOUNCEMENT.code());

    /** 热评前置条数（需求 4.4.4：点赞数最高的前 3 条排最前并带"热评"标识） */
    private static final int HOT_COMMENT_LIMIT = 3;

    /** 信息流卡片上的高赞评论预览条数（需求 4.4.1：点赞数最高的 1～2 条） */
    private static final int HOT_PREVIEW_LIMIT = 2;

    private static final String ITEM_TYPE_POST = "POST";

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final MediaService mediaService;
    private final FavoriteService favoriteService;
    private final LikeService likeService;
    private final SensitiveWordService sensitiveWordService;
    private final UserService userService;
    private final BlacklistService blacklistService;
    private final FileUrlResolver fileUrlResolver;

    // ------------------------------------------------------------------ #27 信息流

    @Override
    public CursorResult<FeedItemVO> feed(int cursor, int size, Long viewerId) {
        int pageSize = normalizeSize(size);
        boolean firstPage = cursor <= 0;

        // 首页把最新一条公告拎出来固定置顶（其余公告仍按时间正常混排）
        Post pinned = firstPage ? latestAnnouncement() : null;
        // 实现注记 3 三口过滤之一（信息流）：黑名单双方内容互不可见，公告也不例外
        Set<Long> hidden = hiddenIds(viewerId);
        if (pinned != null && hidden.contains(pinned.getUserId())) {
            pinned = null;
        }
        Long pinnedId = pinned == null ? null : pinned.getId();

        List<Post> posts = postMapper.selectList(Wrappers.<Post>lambdaQuery()
                .in(Post::getStatus, VISIBLE_STATUSES)
                .ne(pinnedId != null, Post::getId, pinnedId)
                .notIn(!hidden.isEmpty(), Post::getUserId, hidden)
                .lt(cursor > 0, Post::getId, cursor)
                .orderByDesc(Post::getId)
                .last("LIMIT " + (pageSize + 1)));

        boolean hasMore = posts.size() > pageSize;
        List<Post> page = hasMore ? new ArrayList<>(posts.subList(0, pageSize)) : posts;

        List<Post> all = new ArrayList<>(page.size() + 1);
        if (pinned != null) {
            all.add(pinned);
        }
        all.addAll(page);

        // 批量装配：作者 + 高赞评论作者 → 一次取用户
        List<Long> postIds = all.stream().map(Post::getId).toList();
        List<Comment> hotComments = postIds.isEmpty()
                ? List.of()
                : commentMapper.selectTopLikedByPostIds(postIds, HOT_PREVIEW_LIMIT);

        Set<Long> userIds = new HashSet<>();
        for (Post post : all) {
            userIds.add(post.getUserId());
        }
        for (Comment comment : hotComments) {
            userIds.add(comment.getUserId());
        }
        Map<Long, User> users = loadUsers(userIds);
        Map<Long, List<MediaVO>> medias = mediaMap(postIds);
        Map<Long, List<HotCommentVO>> hotMap = hotCommentMap(hotComments, users);
        Set<Long> likedIds = likedPostIds(viewerId, all);

        List<FeedItemVO> list = new ArrayList<>(all.size());
        for (Post post : all) {
            FeedItemVO vo = new FeedItemVO();
            vo.setItemType(ITEM_TYPE_POST);
            vo.setPinned(post == pinned);
            vo.setId(post.getId());
            vo.setAuthor(authorOf(post.getUserId(), users));
            vo.setPublishTime(post.getCreateTime());
            vo.setContent(post.getContent());
            vo.setMedia(medias.getOrDefault(post.getId(), List.of()));
            vo.setLikeCount(post.getLikeCount());
            vo.setCommentCount(post.getCommentCount());
            vo.setLiked(likedIds.contains(post.getId()));
            vo.setHotComments(hotMap.getOrDefault(post.getId(), List.of()));
            vo.setIsAnnouncement(Objects.equals(post.getStatus(), PostStatus.ANNOUNCEMENT.code()));
            list.add(vo);
        }
        return new CursorResult<>(list, nextCursor(page), hasMore);
    }

    // ------------------------------------------------------------------ #28 发帖

    @Override
    @Transactional
    public PostPublishVO publish(long userId, PostPublishDTO dto) {
        List<String> imageKeys = imageKeys(dto);
        validateMedia(imageKeys, dto.getVideoKey());
        sensitiveWordService.check(dto.getContent());

        LocalDateTime now = LocalDateTime.now();
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(dto.getContent());
        post.setStatus(PostStatus.PUBLISHED.code());
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setCreateTime(now);
        post.setUpdateTime(now);
        postMapper.insert(post);

        mediaService.saveAll(buildMedias(post.getId(), imageKeys, dto.getVideoKey(), now));
        return new PostPublishVO(post.getId());
    }

    // ------------------------------------------------------------------ #29 帖子详情

    @Override
    public PostDetailVO detail(long id, Long viewerId) {
        Post post = requireVisiblePost(id);
        // 实现注记 3 之一（详情口）：作者与 viewer 互拉黑时帖子不可见（#29 要求 40401）
        if (viewerId != null && blacklistService.blockedBetween(viewerId, post.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在或已删除");
        }

        List<Comment> hotComments = commentMapper.selectTopLikedByPostIds(List.of(id), HOT_PREVIEW_LIMIT);
        Set<Long> userIds = new HashSet<>();
        userIds.add(post.getUserId());
        for (Comment comment : hotComments) {
            userIds.add(comment.getUserId());
        }
        Map<Long, User> users = loadUsers(userIds);

        PostDetailVO vo = new PostDetailVO();
        vo.setId(post.getId());
        vo.setAuthor(authorOf(post.getUserId(), users));
        vo.setPublishTime(post.getCreateTime());
        vo.setContent(post.getContent());
        vo.setMedia(toMediaVOs(mediaService.listByBiz(Media.BIZ_POST, List.of(id))));
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setLiked(viewerId != null && likeService.isLiked(viewerId, LikeRecord.TARGET_POST, id));
        vo.setHotComments(toHotCommentVOs(hotComments, users));
        vo.setIsAnnouncement(Objects.equals(post.getStatus(), PostStatus.ANNOUNCEMENT.code()));
        vo.setIsMine(viewerId != null && Objects.equals(post.getUserId(), viewerId));
        vo.setCanReport(true);
        vo.setFavorited(viewerId != null && favoriteService.isFavorited(viewerId, Favorite.TARGET_POST, id));
        return vo;
    }

    // ------------------------------------------------------------------ #30 帖子点赞 / 取消

    @Override
    @Transactional
    public LikeResultVO likePost(long userId, long id) {
        requireVisiblePost(id);
        if (likeService.like(userId, LikeRecord.TARGET_POST, id)) {
            postMapper.update(null, Wrappers.<Post>lambdaUpdate()
                    .setSql("like_count = like_count + 1")
                    .eq(Post::getId, id));
        }
        return new LikeResultVO(postMapper.selectById(id).getLikeCount(), true);
    }

    @Override
    @Transactional
    public LikeResultVO unlikePost(long userId, long id) {
        requireVisiblePost(id);
        if (likeService.unlike(userId, LikeRecord.TARGET_POST, id)) {
            postMapper.update(null, Wrappers.<Post>lambdaUpdate()
                    .setSql("like_count = GREATEST(like_count - 1, 0)")
                    .eq(Post::getId, id));
        }
        return new LikeResultVO(postMapper.selectById(id).getLikeCount(), false);
    }

    // ------------------------------------------------------------------ #31 帖子收藏 / 取消

    @Override
    public void favorite(long userId, long postId) {
        requireVisiblePost(postId);
        favoriteService.favorite(userId, Favorite.TARGET_POST, postId);
    }

    @Override
    public void unfavorite(long userId, long postId) {
        favoriteService.unfavorite(userId, Favorite.TARGET_POST, postId);
    }

    // ------------------------------------------------------------------ #32 评论列表

    @Override
    public CursorResult<CommentVO> comments(long postId, int cursor, int size, Long viewerId) {
        requireVisiblePost(postId);
        int pageSize = normalizeSize(size);

        // 首页把点赞数最高的前 3 条拎出来前置并置 hot；其余按时间倒序
        List<Comment> hot = cursor <= 0 ? hotCommentsOfPost(postId) : List.of();
        // 实现注记 3 之一（评论列表口）：黑名单双方的评论不展示
        Set<Long> hidden = hiddenIds(viewerId);
        hot = filterHiddenComments(hot, hidden);
        Set<Long> hotIds = new HashSet<>();
        for (Comment comment : hot) {
            hotIds.add(comment.getId());
        }

        List<Comment> rows = commentMapper.selectList(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getPostId, postId)
                .notIn(!hotIds.isEmpty(), Comment::getId, hotIds)
                .notIn(!hidden.isEmpty(), Comment::getUserId, hidden)
                .lt(cursor > 0, Comment::getId, cursor)
                .orderByDesc(Comment::getId)
                .last("LIMIT " + (pageSize + 1)));

        boolean hasMore = rows.size() > pageSize;
        List<Comment> page = hasMore ? new ArrayList<>(rows.subList(0, pageSize)) : rows;

        List<Comment> all = new ArrayList<>(hot.size() + page.size());
        all.addAll(hot);
        all.addAll(page);

        // 批量装配作者与"被回复评论"的作者
        Set<Long> userIds = new HashSet<>();
        for (Comment comment : all) {
            userIds.add(comment.getUserId());
        }
        Set<Long> replyIds = new HashSet<>();
        for (Comment comment : all) {
            if (comment.getReplyToId() != null) {
                replyIds.add(comment.getReplyToId());
            }
        }
        Map<Long, Comment> replyMap = replyIds.isEmpty() ? Map.of() : mapById(commentMapper.selectBatchIds(replyIds));
        for (Comment target : replyMap.values()) {
            userIds.add(target.getUserId());
        }
        Map<Long, User> users = loadUsers(userIds);
        Set<Long> likedIds = likedCommentIds(viewerId, all);

        List<CommentVO> list = new ArrayList<>(all.size());
        for (Comment comment : hot) {
            list.add(toCommentVO(comment, true, users, replyMap, likedIds));
        }
        for (Comment comment : page) {
            list.add(toCommentVO(comment, false, users, replyMap, likedIds));
        }
        return new CursorResult<>(list, nextCursorOfComments(page), hasMore);
    }

    // ------------------------------------------------------------------ #33 发表评论 / 回复

    @Override
    @Transactional
    public CommentVO comment(long userId, long postId, CommentPublishDTO dto) {
        Post post = requireVisiblePost(postId);
        // #43 拉黑效果：双方任一方向拉黑，对方无法评论
        if (blacklistService.blockedBetween(userId, post.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "因拉黑关系，暂时无法评论");
        }
        sensitiveWordService.check(dto.getContent());

        // 顶层评论：前端历史上用 0 表示"不是回复"，0 / 负数一律视为不回复，避免误判成评论 ID
        Long replyToId = dto.getReplyToCommentId();
        if (replyToId != null && replyToId > 0) {
            Comment target = commentMapper.selectById(replyToId);
            if (target == null || !Objects.equals(target.getPostId(), postId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "被回复的评论不存在");
            }
        } else {
            replyToId = null;
        }

        LocalDateTime now = LocalDateTime.now();
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setReplyToId(replyToId);
        comment.setLikeCount(0);
        comment.setCreateTime(now);
        comment.setUpdateTime(now);
        commentMapper.insert(comment);

        postMapper.update(null, Wrappers.<Post>lambdaUpdate()
                .setSql("comment_count = comment_count + 1")
                .eq(Post::getId, postId));

        // 组装返回体（作者 + 被回复评论的昵称）
        Map<Long, Comment> replyMap = Map.of();
        Set<Long> userIds = new HashSet<>();
        userIds.add(userId);
        if (replyToId != null) {
            Comment target = commentMapper.selectById(replyToId);
            if (target != null) {
                replyMap = Map.of(target.getId(), target);
                userIds.add(target.getUserId());
            }
        }
        Map<Long, User> users = loadUsers(userIds);
        return toCommentVO(comment, false, users, replyMap, Set.of());
    }

    // ------------------------------------------------------------------ #34 评论点赞 / 取消

    @Override
    @Transactional
    public LikeResultVO likeComment(long userId, long commentId) {
        requireComment(commentId);
        if (likeService.like(userId, LikeRecord.TARGET_COMMENT, commentId)) {
            commentMapper.update(null, Wrappers.<Comment>lambdaUpdate()
                    .setSql("like_count = like_count + 1")
                    .eq(Comment::getId, commentId));
        }
        return new LikeResultVO(commentMapper.selectById(commentId).getLikeCount(), true);
    }

    @Override
    @Transactional
    public LikeResultVO unlikeComment(long userId, long commentId) {
        requireComment(commentId);
        if (likeService.unlike(userId, LikeRecord.TARGET_COMMENT, commentId)) {
            commentMapper.update(null, Wrappers.<Comment>lambdaUpdate()
                    .setSql("like_count = GREATEST(like_count - 1, 0)")
                    .eq(Comment::getId, commentId));
        }
        return new LikeResultVO(commentMapper.selectById(commentId).getLikeCount(), false);
    }

    // ------------------------------------------------------------------ #35 删除帖子

    @Override
    public void delete(long userId, long id) {
        Post post = postMapper.selectById(id);
        if (post == null || !VISIBLE_STATUSES.contains(post.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在或已删除");
        }
        if (!Objects.equals(post.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅可删除自己发布的帖子");
        }
        post.setStatus(PostStatus.DELETED.code());
        post.setUpdateTime(LocalDateTime.now());
        postMapper.updateById(post);
    }

    // ------------------------------------------------------------------ 我的发布 / 我的收藏（联调期新增接口）

    @Override
    public CursorResult<MyPostItemVO> myPosts(long userId, int cursor, int size) {
        int pageSize = normalizeSize(size);
        List<Post> posts = postMapper.selectList(Wrappers.<Post>lambdaQuery()
                .eq(Post::getUserId, userId)
                .in(Post::getStatus, VISIBLE_STATUSES)
                .lt(cursor > 0, Post::getId, cursor)
                .orderByDesc(Post::getId)
                .last("LIMIT " + (pageSize + 1)));

        boolean hasMore = posts.size() > pageSize;
        List<Post> page = hasMore ? new ArrayList<>(posts.subList(0, pageSize)) : posts;
        Map<Long, List<MediaVO>> medias = mediaMap(page.stream().map(Post::getId).toList());

        List<MyPostItemVO> list = new ArrayList<>(page.size());
        for (Post post : page) {
            list.add(toMyItem(post, medias.getOrDefault(post.getId(), List.of()), false));
        }
        return new CursorResult<>(list, nextCursor(page), hasMore);
    }

    @Override
    public CursorResult<MyPostItemVO> myFavorites(long userId, int cursor, int size) {
        int pageSize = normalizeSize(size);
        CursorResult<Favorite> favPage = favoriteService.page(userId, Favorite.TARGET_POST, cursor, pageSize);

        List<Long> postIds = favPage.getList().stream().map(Favorite::getTargetId).toList();
        Map<Long, Post> posts = postIds.isEmpty() ? Map.of()
                : postMapper.selectBatchIds(postIds).stream()
                        .collect(Collectors.toMap(Post::getId, p -> p));
        Map<Long, List<MediaVO>> medias = mediaMap(postIds);

        List<MyPostItemVO> list = new ArrayList<>(favPage.getList().size());
        for (Favorite fav : favPage.getList()) {
            Post post = posts.get(fav.getTargetId());
            boolean unavailable = post == null || !VISIBLE_STATUSES.contains(post.getStatus());
            list.add(toMyItem(post, medias.getOrDefault(fav.getTargetId(), List.of()), unavailable));
        }
        // 游标按收藏记录 id 翻页（与二手收藏口径一致），无数据时为 0
        return new CursorResult<>(list, favPage.getList().isEmpty() ? 0 : favPage.getNextCursor(), favPage.isHasMore());
    }

    // ------------------------------------------------------------------ 私有方法

    /** 双向黑名单并集；游客返回空集合（不过滤）。 */
    private Set<Long> hiddenIds(Long viewerId) {
        return viewerId == null ? Set.of() : blacklistService.hiddenUserIds(viewerId);
    }

    /** 热评前置里的黑名单过滤（服务层过滤，量小可接受）。 */
    private List<Comment> filterHiddenComments(List<Comment> comments, Set<Long> hidden) {
        if (hidden.isEmpty() || comments.isEmpty()) {
            return comments;
        }
        List<Comment> kept = new ArrayList<>(comments.size());
        for (Comment comment : comments) {
            if (!hidden.contains(comment.getUserId())) {
                kept.add(comment);
            }
        }
        return kept;
    }

    private MyPostItemVO toMyItem(Post post, List<MediaVO> media, boolean unavailable) {
        MyPostItemVO vo = new MyPostItemVO();
        if (post != null) {
            vo.setId(post.getId());
            vo.setContent(post.getContent());
            vo.setPublishTime(post.getCreateTime());
            vo.setLikeCount(post.getLikeCount());
            vo.setCommentCount(post.getCommentCount());
        }
        vo.setMedia(media);
        vo.setUnavailable(unavailable);
        return vo;
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private long nextCursor(List<? extends Post> page) {
        return page.isEmpty() ? 0 : page.get(page.size() - 1).getId();
    }

    private long nextCursorOfComments(List<Comment> page) {
        return page.isEmpty() ? 0 : page.get(page.size() - 1).getId();
    }

    /** 最新一条公告（无则 null）。 */
    private Post latestAnnouncement() {
        return postMapper.selectOne(Wrappers.<Post>lambdaQuery()
                .eq(Post::getStatus, PostStatus.ANNOUNCEMENT.code())
                .orderByDesc(Post::getId)
                .last("LIMIT 1"));
    }

    /** 某帖点赞数最高的前 N 条评论（仅统计点赞数 > 0 的，避免 0 赞评论被标为热评）。 */
    private List<Comment> hotCommentsOfPost(long postId) {
        return commentMapper.selectList(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getPostId, postId)
                .gt(Comment::getLikeCount, 0)
                .orderByDesc(Comment::getLikeCount)
                .orderByAsc(Comment::getId)
                .last("LIMIT " + HOT_COMMENT_LIMIT));
    }

    /** 校验帖子存在且未删除，否则 40401。 */
    private Post requireVisiblePost(long id) {
        Post post = postMapper.selectById(id);
        if (post == null || !VISIBLE_STATUSES.contains(post.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在或已删除");
        }
        return post;
    }

    private void requireComment(long id) {
        if (commentMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在或已删除");
        }
    }

    private Map<Long, User> loadUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, User> map = new HashMap<>(userIds.size());
        for (User user : userService.listByIds(userIds)) {
            map.put(user.getId(), user);
        }
        return map;
    }

    private Map<Long, Comment> mapById(List<Comment> comments) {
        Map<Long, Comment> map = new HashMap<>(comments.size());
        for (Comment comment : comments) {
            map.put(comment.getId(), comment);
        }
        return map;
    }

    /** 作者对象：registerTime 仅本人资料返回，作者信息不下发。 */
    private UserVO authorOf(Long userId, Map<Long, User> users) {
        User user = users.get(userId);
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setGender(user.getGender());
        vo.setBio(user.getBio());
        return vo;
    }

    /** 批量取帖子媒体，按 biz_id 分组（listByBiz 已按 sort 升序）。 */
    private Map<Long, List<MediaVO>> mediaMap(Collection<Long> postIds) {
        Map<Long, List<MediaVO>> map = new HashMap<>();
        if (postIds.isEmpty()) {
            return map;
        }
        for (Media media : mediaService.listByBiz(Media.BIZ_POST, postIds)) {
            map.computeIfAbsent(media.getBizId(), key -> new ArrayList<>()).add(toMediaVO(media));
        }
        return map;
    }

    private List<MediaVO> toMediaVOs(List<Media> medias) {
        List<MediaVO> list = new ArrayList<>(medias.size());
        for (Media media : medias) {
            list.add(toMediaVO(media));
        }
        return list;
    }

    private MediaVO toMediaVO(Media media) {
        String type = Objects.equals(media.getMediaType(), Media.TYPE_VIDEO) ? "VIDEO" : "IMAGE";
        return new MediaVO(type, resolveUrl(media.getFileKey()));
    }

    private List<HotCommentVO> toHotCommentVOs(List<Comment> comments, Map<Long, User> users) {
        List<HotCommentVO> list = new ArrayList<>(comments.size());
        for (Comment comment : comments) {
            User user = users.get(comment.getUserId());
            list.add(new HotCommentVO(comment.getId(), user == null ? null : user.getNickname(), comment.getContent()));
        }
        return list;
    }

    private Map<Long, List<HotCommentVO>> hotCommentMap(List<Comment> comments, Map<Long, User> users) {
        Map<Long, List<HotCommentVO>> map = new HashMap<>();
        for (Comment comment : comments) {
            User user = users.get(comment.getUserId());
            map.computeIfAbsent(comment.getPostId(), key -> new ArrayList<>())
                    .add(new HotCommentVO(comment.getId(), user == null ? null : user.getNickname(), comment.getContent()));
        }
        return map;
    }

    private CommentVO toCommentVO(Comment comment, boolean hot, Map<Long, User> users,
                                  Map<Long, Comment> replyMap, Set<Long> likedIds) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setAuthor(authorOf(comment.getUserId(), users));
        vo.setContent(comment.getContent());
        vo.setLikeCount(comment.getLikeCount());
        vo.setLiked(likedIds.contains(comment.getId()));
        vo.setHot(hot);
        vo.setCreateTime(comment.getCreateTime());
        if (comment.getReplyToId() != null) {
            Comment target = replyMap.get(comment.getReplyToId());
            if (target != null) {
                User targetAuthor = users.get(target.getUserId());
                vo.setReplyTo(new ReplyToVO(target.getId(), targetAuthor == null ? null : targetAuthor.getNickname()));
            }
        }
        return vo;
    }

    /** 批量取帖子点赞态；游客返回空集合（liked 恒 false）。 */
    private Set<Long> likedPostIds(Long viewerId, List<Post> posts) {
        if (viewerId == null || posts.isEmpty()) {
            return Set.of();
        }
        return likeService.likedTargetIds(viewerId, LikeRecord.TARGET_POST,
                posts.stream().map(Post::getId).toList());
    }

    private Set<Long> likedCommentIds(Long viewerId, List<Comment> comments) {
        if (viewerId == null || comments.isEmpty()) {
            return Set.of();
        }
        return likeService.likedTargetIds(viewerId, LikeRecord.TARGET_COMMENT,
                comments.stream().map(Comment::getId).toList());
    }

    /** 图片 fileKey 列表：过滤空串，最多 9 张（超过由 @Size 拦截）。 */
    private List<String> imageKeys(PostPublishDTO dto) {
        List<String> keys = new ArrayList<>();
        if (dto.getImages() != null) {
            for (String key : dto.getImages()) {
                if (StringUtils.hasText(key)) {
                    keys.add(key);
                }
            }
        }
        return keys;
    }

    /** images 与 videoKey 互斥、至少其一（annotation 表达不了，放这里）。 */
    private void validateMedia(List<String> imageKeys, String videoKey) {
        boolean hasVideo = StringUtils.hasText(videoKey);
        boolean hasImages = !imageKeys.isEmpty();
        if (!hasImages && !hasVideo) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图片与视频至少上传其一");
        }
        if (hasImages && hasVideo) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图片与视频不能同时上传");
        }
        if (imageKeys.size() > 9) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图片最多 9 张");
        }
    }

    /** 组装媒体行：有视频则只写 1 条视频，否则按顺序写图片。 */
    private List<Media> buildMedias(long postId, List<String> imageKeys, String videoKey, LocalDateTime now) {
        List<Media> medias = new ArrayList<>();
        int sort = 0;
        if (StringUtils.hasText(videoKey)) {
            medias.add(newMedia(postId, Media.TYPE_VIDEO, videoKey, sort, now));
        } else {
            for (String key : imageKeys) {
                medias.add(newMedia(postId, Media.TYPE_IMAGE, key, sort++, now));
            }
        }
        return medias;
    }

    private Media newMedia(long postId, int mediaType, String fileKey, int sort, LocalDateTime now) {
        Media media = new Media();
        media.setBizType(Media.BIZ_POST);
        media.setBizId(postId);
        media.setMediaType(mediaType);
        media.setFileKey(fileKey);
        media.setSort(sort);
        // 内容安全服务尚未接入，先置机审通过，避免帖子因机审状态被拦
        media.setAuditStatus(1);
        media.setCreateTime(now);
        media.setUpdateTime(now);
        return media;
    }

    /** fileKey → 可访问 URL，统一由 {@link FileUrlResolver} 决定（本地存储 → /files/**；OSS → endpoint）。 */
    private String resolveUrl(String fileKey) {
        return fileUrlResolver.resolve(fileKey);
    }

}
