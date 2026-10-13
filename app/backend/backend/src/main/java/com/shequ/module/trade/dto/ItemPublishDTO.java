package com.shequ.module.trade.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发布 / 编辑二手请求体（接口文档 2.3 #20 / #21 参数一致）。
 * <p>
 * images 与 videoKey 互斥、至少其一，服务层再校验（Bean Validation 表达不了"至少其一"）。
 */
@Data
public class ItemPublishDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 30, message = "标题不能超过 30 字")
    private String title;

    /** 图片 fileKey 数组，最多 9 张；与 videoKey 互斥 */
    @Size(max = 9, message = "图片最多 9 张")
    private List<String> images;

    /** 视频 fileKey；有视频时 images 必须为空 */
    private String videoKey;

    @NotBlank(message = "物品介绍不能为空")
    @Size(max = 500, message = "物品介绍不能超过 500 字")
    private String description;

    @NotNull(message = "价格不能为空")
    @Min(value = 1, message = "价格必须大于 0")
    private Integer priceFen;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    /** 第三方联系方式（选填，仅存档，任何接口不返回） */
    @Size(max = 100, message = "联系方式不能超过 100 字")
    private String contact;

    @NotNull(message = "是否可议价不能为空")
    private Boolean bargainable;

}
