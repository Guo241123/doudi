package com.shequ.module.forum.vo;

import com.shequ.module.user.vo.UserVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 信息流项（接口文档 2.6 #27 list 每项）。
 * <p>
 * 本轮仅有 {@code itemType=POST}；广告位（itemType=AD + ad 对象）待 ads 模块落地后再插入。
 */
@Data
public class FeedItemVO {

    /** POST 帖子 / AD 广告（本轮恒为 POST） */
    private String itemType;

    /** ★公告置顶标记（仅最新一条公告为 true，固定在序列最前） */
    private Boolean pinned;

    private Long id;

    private UserVO author;

    private LocalDateTime publishTime;

    private String content;

    private List<MediaVO> media;

    private Integer likeCount;

    private Integer commentCount;

    /** 当前用户是否已赞（游客恒 false） */
    private Boolean liked;

    /** ★高赞评论预览，前 1～2 条 */
    private List<HotCommentVO> hotComments;

    private Boolean isAnnouncement;

}
