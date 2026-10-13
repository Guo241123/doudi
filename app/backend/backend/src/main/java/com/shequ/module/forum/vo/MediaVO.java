package com.shequ.module.forum.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 帖子媒体项（接口文档 2.6 #27 media[]）：type 只暴露 IMAGE / VIDEO，前端按类型渲染。
 */
@Data
@AllArgsConstructor
public class MediaVO {

    /** IMAGE / VIDEO */
    private String type;

    private String url;

}
