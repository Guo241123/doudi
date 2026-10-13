package com.shequ.module.forum.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * "我的发布 / 我的收藏"帖子列表项（接口文档联调期新增，见需求审查报告留痕）。
 * unavailable=true 表示原帖已删除或不可见（收藏列表置灰展示）。
 */
@Data
public class MyPostItemVO {

    private Long id;

    private String content;

    private LocalDateTime publishTime;

    private List<MediaVO> media;

    private Integer likeCount;

    private Integer commentCount;

    /** 原帖是否已删除 / 不可见（仅收藏列表会出现 true） */
    private Boolean unavailable;

}
