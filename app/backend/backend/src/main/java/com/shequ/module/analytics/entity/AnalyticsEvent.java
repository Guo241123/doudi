package com.shequ.module.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 埋点事件实体（接口文档 #46）。
 * 批量落表无主键业务依赖，写入走内存队列聚合。
 */
@Data
@TableName("t_analytics_event")
public class AnalyticsEvent {

    /** 事件名五件套（README 开发红线：必须随 MVP 带上） */
    public static final String APP_LAUNCH = "APP_LAUNCH";
    public static final String PAGE_VIEW = "PAGE_VIEW";
    public static final String AD_IMPRESSION = "AD_IMPRESSION";
    public static final String AD_CLICK = "AD_CLICK";
    public static final String PUBLISH = "PUBLISH";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID（未登录为空） */
    private Long userId;

    /** 设备号（未登录必填） */
    private String deviceId;

    /** 事件名，取值见本类常量 */
    private String name;

    /** 事件属性 JSON 字符串（≤500） */
    private String props;

    /** 事件发生时间（客户端上报） */
    private LocalDateTime eventTime;

    /** 落库时间 */
    private LocalDateTime createTime;

}
