package com.shequ.module.forum.service;

import com.shequ.common.result.CursorResult;
import com.shequ.module.forum.dto.CommentPublishDTO;
import com.shequ.module.forum.dto.PostPublishDTO;
import com.shequ.module.forum.vo.CommentVO;
import com.shequ.module.forum.vo.FeedItemVO;
import com.shequ.module.forum.vo.LikeResultVO;
import com.shequ.module.forum.vo.MyPostItemVO;
import com.shequ.module.forum.vo.PostDetailVO;
import com.shequ.module.forum.vo.PostPublishVO;

/**
 * 论坛服务（接口文档 2.6 #27～#35）。
 * <p>
 * 游客可读：#27 信息流、#29 详情、#32 评论列表（viewerId 为空即游客，liked 恒 false）；
 * 写接口（发帖 / 点赞 / 收藏 / 评论 / 删帖）一律需登录，userId 取自登录态。
 */
public interface ForumService {

    /** #27 信息流（最新公告置顶 + 帖子按 id 倒序混排）。 */
    CursorResult<FeedItemVO> feed(int cursor, int size, Long viewerId);

    /** #28 发帖（先发后审：敏感词命中抛 40002，未命中立即发布）。 */
    PostPublishVO publish(long userId, PostPublishDTO dto);

    /** #29 帖子详情（含 isMine / canReport / favorited）。 */
    PostDetailVO detail(long id, Long viewerId);

    /** #30 帖子点赞（幂等）。 */
    LikeResultVO likePost(long userId, long id);

    /** #30 取消帖子点赞（幂等）。 */
    LikeResultVO unlikePost(long userId, long id);

    /** #31 收藏帖子（幂等）。 */
    void favorite(long userId, long postId);

    /** #31 取消收藏帖子（幂等）。 */
    void unfavorite(long userId, long postId);

    /** #32 评论列表（热评前置：点赞数最高前 3 条排最前并置 hot=true）。 */
    CursorResult<CommentVO> comments(long postId, int cursor, int size, Long viewerId);

    /** #33 发表评论 / 回复（敏感词命中抛 40002）。 */
    CommentVO comment(long userId, long postId, CommentPublishDTO dto);

    /** #34 评论点赞（幂等，同用户同评论仅计一次）。 */
    LikeResultVO likeComment(long userId, long commentId);

    /** #34 取消评论点赞（幂等）。 */
    LikeResultVO unlikeComment(long userId, long commentId);

    /** #35 删除自己的帖子（软删除，仅本人）。 */
    void delete(long userId, long id);

    /** 我发布的帖子列表（联调期新增接口，游标分页，口径同信息流可见状态）。 */
    CursorResult<MyPostItemVO> myPosts(long userId, int cursor, int size);

    /** 我收藏的帖子列表（联调期新增接口；原帖已删/不可见时 unavailable=true）。 */
    CursorResult<MyPostItemVO> myFavorites(long userId, int cursor, int size);

}
