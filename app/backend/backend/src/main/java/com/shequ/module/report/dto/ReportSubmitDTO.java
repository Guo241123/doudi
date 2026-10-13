package com.shequ.module.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交举报请求体（接口文档 #42）。
 */
@Data
public class ReportSubmitDTO {

    @NotBlank(message = "目标类型不能为空")
    @Pattern(regexp = "ITEM|POST|COMMENT|USER", message = "目标类型不合法")
    private String targetType;

    @NotNull(message = "目标 ID 不能为空")
    private Long targetId;

    @NotBlank(message = "举报原因不能为空")
    @Pattern(regexp = "SPAM|PORN|FRAUD|COPYRIGHT|OTHER", message = "举报原因不合法")
    private String reason;

    @Size(max = 200, message = "补充说明不能超过 200 字")
    private String description;

}
