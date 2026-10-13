package com.shequ.module.forum.controller;

import com.shequ.common.auth.AuthInterceptor;
import com.shequ.common.result.CursorResult;
import com.shequ.common.result.Result;
import com.shequ.module.forum.dto.CommentPublishDTO;
import com.shequ.module.forum.dto.PostPublishDTO;
import com.shequ.module.forum.service.ForumService;
import com.shequ.module.forum.vo.CommentVO;
import com.shequ.module.forum.vo.FeedItemVO;
import com.shequ.module.forum.vo.LikeResultVO;
import com.shequ.module.forum.vo.MyPostItemVO;
import com.shequ.module.forum.vo.PostDetailVO;
import com.shequ.module.forum.vo.PostPublishVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 论坛接口（接口文档 2.6 #27～#35）。
 * <p>
 * #27 信息流、#29 详情、#32 评论列表游客可读（GET，带 token 则回填 liked / isMine / favorited）；
 * 其余写接口需登录，身份取自登录态，不存在 userId 入参。
 * 帖子为"先发后审"：发帖 / 评论命中敏感词返回 40002，未命中立即展示。
 * 论坛管理（巡查 #52）属后台，待 admin 模块落地后实现。
 */
@Tag(name = "论坛")
@RestController
@RequestMapping("/api/v1/forum")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;

    @Operation(summary = "#27 信息流")
    @GetMapping("/feed")
    public Result<CursorResult<FeedItemVO>> feed(
            @RequestParam(defaultValue = "0") int cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestAttribute(value = AuthInterceptor.ATTR_USER_ID, required = false) Long userId) {
        return Result.ok(forumService.feed(cursor, size, userId));
    }

    @Operation(summary = "#28 发帖")
    @PostMapping("/posts")
    public Result<PostPublishVO> publish(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                         @Valid @RequestBody PostPublishDTO dto) {
        return Result.ok(forumService.publish(userId, dto));
    }

    @Operation(summary = "#29 帖子详情")
    @GetMapping("/posts/{id}")
    public Result<PostDetailVO> detail(
            @PathVariable long id,
            @RequestAttribute(value = AuthInterceptor.ATTR_USER_ID, required = false) Long userId) {
        return Result.ok(forumService.detail(id, userId));
    }

    @Operation(summary = "#30 帖子点赞")
    @PostMapping("/posts/{id}/like")
    public Result<LikeResultVO> likePost(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                         @PathVariable long id) {
        return Result.ok(forumService.likePost(userId, id));
    }

    @Operation(summary = "#30 取消帖子点赞")
    @DeleteMapping("/posts/{id}/like")
    public Result<LikeResultVO> unlikePost(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                           @PathVariable long id) {
        return Result.ok(forumService.unlikePost(userId, id));
    }

    @Operation(summary = "#31 收藏帖子")
    @PostMapping("/posts/{id}/favorite")
    public Result<Void> favorite(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                 @PathVariable long id) {
        forumService.favorite(userId, id);
        return Result.ok();
    }

    @Operation(summary = "#31 取消收藏帖子")
    @DeleteMapping("/posts/{id}/favorite")
    public Result<Void> unfavorite(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                   @PathVariable long id) {
        forumService.unfavorite(userId, id);
        return Result.ok();
    }

    @Operation(summary = "#32 评论列表")
    @GetMapping("/posts/{id}/comments")
    public Result<CursorResult<CommentVO>> comments(
            @PathVariable long id,
            @RequestParam(defaultValue = "0") int cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestAttribute(value = AuthInterceptor.ATTR_USER_ID, required = false) Long userId) {
        return Result.ok(forumService.comments(id, cursor, size, userId));
    }

    @Operation(summary = "#33 发表评论 / 回复")
    @PostMapping("/posts/{id}/comments")
    public Result<CommentVO> comment(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                     @PathVariable long id,
                                     @Valid @RequestBody CommentPublishDTO dto) {
        return Result.ok(forumService.comment(userId, id, dto));
    }

    @Operation(summary = "#34 评论点赞")
    @PostMapping("/comments/{id}/like")
    public Result<LikeResultVO> likeComment(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                            @PathVariable long id) {
        return Result.ok(forumService.likeComment(userId, id));
    }

    @Operation(summary = "#34 取消评论点赞")
    @DeleteMapping("/comments/{id}/like")
    public Result<LikeResultVO> unlikeComment(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                              @PathVariable long id) {
        return Result.ok(forumService.unlikeComment(userId, id));
    }

    @Operation(summary = "#35 删除自己的帖子")
    @DeleteMapping("/posts/{id}")
    public Result<Void> delete(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                               @PathVariable long id) {
        forumService.delete(userId, id);
        return Result.ok();
    }

    @Operation(summary = "我发布的帖子（联调期新增接口）")
    @GetMapping("/my-posts")
    public Result<CursorResult<MyPostItemVO>> myPosts(
            @RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
            @RequestParam(defaultValue = "0") int cursor,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(forumService.myPosts(userId, cursor, size));
    }

    @Operation(summary = "我收藏的帖子（联调期新增接口）")
    @GetMapping("/my-favorites")
    public Result<CursorResult<MyPostItemVO>> myFavorites(
            @RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
            @RequestParam(defaultValue = "0") int cursor,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(forumService.myFavorites(userId, cursor, size));
    }

}
