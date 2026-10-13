package com.shequ.module.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表评论 / 回复请求体（接口文档 2.6 #33）。
 */
@Data
public class CommentPublishDTO {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 200, message = "评论内容不能超过 200 字")
    private String content;

    /** 回复某条评论（二级，可空） */
    private Long replyToCommentId;

}
