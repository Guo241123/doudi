package com.shequ.module.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发布帖子请求体（接口文档 2.6 #28）。
 * <p>
 * images 与 videoKey 互斥、至少其一，服务层再校验（Bean Validation 表达不了"至少其一"）。
 */
@Data
public class PostPublishDTO {

    @NotBlank(message = "文字内容不能为空")
    @Size(max = 1000, message = "文字内容不能超过 1000 字")
    private String content;

    /** 图片 fileKey 数组，最多 9 张；与 videoKey 互斥 */
    @Size(max = 9, message = "图片最多 9 张")
    private List<String> images;

    /** 视频 fileKey；有视频时 images 必须为空 */
    private String videoKey;

}
