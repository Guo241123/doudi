package com.shequ.module.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报表实体（接口文档 #41/#42；处理入口在后台 #54）。
 */
@Data
@TableName("t_report")
public class Report {

    /** 目标类型：二手物品 */
    public static final String TARGET_ITEM = "ITEM";
    /** 目标类型：帖子 */
    public static final String TARGET_POST = "POST";
    /** 目标类型：评论 */
    public static final String TARGET_COMMENT = "COMMENT";
    /** 目标类型：用户 */
    public static final String TARGET_USER = "USER";

    /** 待处理 */
    public static final int STATUS_PENDING = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reporterId;

    /** ITEM / POST / COMMENT / USER */
    private String targetType;

    private Long targetId;

    /** SPAM / PORN / FRAUD / COPYRIGHT / OTHER */
    private String reason;

    /** 补充说明 ≤200 字 */
    private String description;

    /** 0 待处理 / 1 已处理 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 软删除：0 正常 / 1 已删除 */
    private Integer isDeleted;

}
