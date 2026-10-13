package com.shequ.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务表实体（数据库设计 三、4）。
 * <p>
 * 任务无"接单"概念，状态只有发布中 / 已下架，不存在流转字段。
 */
@Data
@TableName("t_task")
public class Task {

    /** 发布中 */
    public static final int STATUS_PUBLISHED = 0;

    /** 已下架 */
    public static final int STATUS_OFF_SHELF = 1;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布管理员 ID */
    private Long adminId;

    /** 标题，≤50 字 */
    private String title;

    /** 简述，≤200 字 */
    private String summary;

    /** 报酬 / 积分 / 线下结算说明（纯文字，平台无积分体系） */
    private String rewardText;

    /** 截止时间 */
    private LocalDateTime deadline;

    /** 状态，取值见本类常量 */
    private Integer status;

    /** 创建时间，对外即发布时间 */
    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 软删除：0 正常 / 1 已删除 */
    private Integer isDeleted;

}
