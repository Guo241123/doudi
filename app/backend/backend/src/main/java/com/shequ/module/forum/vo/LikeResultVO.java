package com.shequ.module.forum.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 点赞 / 取消点赞返回（接口文档 2.6 #30 / #34）：前端直接刷新计数。
 */
@Data
@AllArgsConstructor
public class LikeResultVO {

    private Integer likeCount;

    private Boolean liked;

}
