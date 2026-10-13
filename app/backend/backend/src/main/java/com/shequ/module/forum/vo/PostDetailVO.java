package com.shequ.module.forum.vo;

import com.shequ.module.user.vo.UserVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子详情（接口文档 2.6 #29）：#27 帖子字段 + isMine / canReport，
 * 另补 favorited（收藏入口在详情页，#31）。
 */
@Data
public class PostDetailVO {

    private Long id;

    private UserVO author;

    private LocalDateTime publishTime;

    private String content;

    private List<MediaVO> media;

    private Integer likeCount;

    private Integer commentCount;

    private Boolean liked;

    private List<HotCommentVO> hotComments;

    private Boolean isAnnouncement;

    /** ★是否本人发布 */
    private Boolean isMine;

    /** ★是否可举报（恒 true，详情页举报入口） */
    private Boolean canReport;

    /** 当前用户是否已收藏（游客恒 false） */
    private Boolean favorited;

}
