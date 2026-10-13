package com.shequ.module.forum.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 评论的回复目标（接口文档 2.6 #32 replyTo）。
 */
@Data
@AllArgsConstructor
public class ReplyToVO {

    private Long commentId;

    private String nickname;

}
