package com.shequ.module.forum.vo;

import com.shequ.module.user.vo.UserVO;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论项（接口文档 2.6 #32 list 每项）。
 */
@Data
public class CommentVO {

    private Long id;

    private UserVO author;

    private String content;

    /** 回复目标：{commentId, nickname}（仅二级回复有） */
    private ReplyToVO replyTo;

    private Integer likeCount;

    /** 当前用户是否已赞（游客恒 false） */
    private Boolean liked;

    /** ★是否热评（点赞数最高前 3 条排最前并置 true） */
    private Boolean hot;

    private LocalDateTime createTime;

}
