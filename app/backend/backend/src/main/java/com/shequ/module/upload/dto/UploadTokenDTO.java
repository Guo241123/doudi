package com.shequ.module.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * #26 获取直传签名入参（接口文档 2.5）。
 */
@Data
public class UploadTokenDTO {

    @NotBlank(message = "文件类型不能为空")
    @Pattern(regexp = "IMAGE|VIDEO", message = "文件类型不合法")
    private String fileType;

    @NotBlank(message = "业务场景不能为空")
    @Pattern(regexp = "AVATAR|ITEM|POST", message = "业务场景不合法")
    private String biz;
}
