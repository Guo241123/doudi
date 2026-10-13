package com.shequ.module.task.vo;

import lombok.Data;

/**
 * 任务附件（接口文档 2.2 #16 的 attachments 元素）。
 */
@Data
public class AttachmentVO {

    /** IMAGE / VIDEO */
    private String type;

    /** 完整可访问 URL */
    private String url;

}
