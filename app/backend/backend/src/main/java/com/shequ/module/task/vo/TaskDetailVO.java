package com.shequ.module.task.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 任务详情（接口文档 2.2 #16）：#15 全部字段 + 附件 + 风险提示。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskDetailVO extends TaskItemVO {

    /** 图片 / 视频附件，按 sort 升序 */
    private List<AttachmentVO> attachments;

    /** 风险提示文案（★ 后端下发，见 application.yml 的 task.risk-notice） */
    private String riskNotice;

}
