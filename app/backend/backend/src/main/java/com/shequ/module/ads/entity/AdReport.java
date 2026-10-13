package com.shequ.module.ads.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 广告曝光 / 点击记录实体（接口文档 #45）。
 */
@Data
@TableName("t_ad_report")
public class AdReport {

    public static final String ACTION_IMPRESSION = "IMPRESSION";
    public static final String ACTION_CLICK = "CLICK";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adId;

    /** TRADE_BANNER / TASK_BANNER / FEED / DETAIL / INTERSTITIAL */
    private String slot;

    /** IMPRESSION / CLICK */
    private String action;

    /** 用户 ID（游客为空） */
    private Long userId;

    private LocalDateTime createTime;

}
