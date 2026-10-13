package com.shequ.module.task.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务信息流列表项（接口文档 2.2 #15）。
 * <p>
 * 不返回任务状态，也不返回任何接单/人数信息（需求 4.3.2 明确"不展示"）。
 */
@Data
public class TaskItemVO {

    private Long id;

    private String title;

    private String summary;

    /** 报酬 / 积分说明（管理员填写的纯文字） */
    private String rewardText;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 截止时间 */
    private LocalDateTime deadline;

    /** 封面图（取附件中第一张图片，无则空） */
    private String coverUrl;

}
