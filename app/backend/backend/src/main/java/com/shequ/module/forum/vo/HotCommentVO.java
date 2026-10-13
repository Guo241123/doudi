package com.shequ.module.forum.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 高赞评论预览（接口文档 2.6 #27 hotComments[]）：只带昵称，前端弱化小字样式。
 */
@Data
public class HotCommentVO {

    private Long commentId;

    private Author author;

    private String content;

    public HotCommentVO(Long commentId, String nickname, String content) {
        this.commentId = commentId;
        this.author = new Author(nickname);
        this.content = content;
    }

    /** 仅昵称的作者对象。 */
    @Data
    @AllArgsConstructor
    public static class Author {

        private String nickname;

    }

}
