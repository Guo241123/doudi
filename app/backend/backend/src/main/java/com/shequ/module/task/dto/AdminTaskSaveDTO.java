package com.shequ.module.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台任务发布 / 编辑请求体（接口文档 #49）。
 */
@Data
public class AdminTaskSaveDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 50, message = "标题不能超过 50 字")
    private String title;

    @NotBlank(message = "简述不能为空")
    @Size(max = 200, message = "简述不能超过 200 字")
    private String summary;

    @NotBlank(message = "报酬说明不能为空")
    @Size(max = 100, message = "报酬说明不能超过 100 字")
    private String rewardText;

    /** 截止时间，必填 */
    @NotNull(message = "截止时间不能为空")
    private LocalDateTime deadline;

    /** 附件 fileKey 数组（可空 = 无附件） */
    @Size(max = 9, message = "附件最多 9 个")
    private List<String> attachments;

}
