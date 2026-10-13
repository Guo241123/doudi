package com.shequ.module.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 二手物品表实体（数据库设计 四、6）。
 * <p>
 * 状态见 {@link ItemStatus}；contact 为第三方联系方式，仅存档，任何接口都不返回。
 */
@Data
@TableName("t_trade_item")
public class TradeItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 卖家用户 ID */
    private Long userId;

    /** 分类 ID */
    private Long categoryId;

    /** 标题，≤30 字 */
    private String title;

    /** 物品介绍，≤500 字 */
    private String description;

    /** 价格（分），>0 */
    private Integer priceFen;

    /** 是否可议价：0 否 / 1 是 */
    private Integer bargainable;

    /** 第三方联系方式（选填，接口层永不返回） */
    private String contact;

    /** 状态，取值见 {@link ItemStatus} */
    private Integer status;

    /** 拒绝原因（审核不通过时） */
    private String rejectReason;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 自动下架时间 = 发布时间 + N 天（定时任务扫描） */
    private LocalDateTime expireAt;

    /** 创建时间，对外即发布时间 */
    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 软删除：0 正常 / 1 已删除 */
    private Integer isDeleted;

}
