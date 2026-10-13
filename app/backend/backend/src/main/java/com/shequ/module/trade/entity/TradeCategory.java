package com.shequ.module.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 二手交易分类表实体（数据库设计 四、5）。
 * <p>
 * 由 Web 后台配置增删改与排序（需求 4.2.1），App 端只读。
 */
@Data
@TableName("t_trade_category")
public class TradeCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名，≤6 字 */
    private String name;

    /** 排序（交易页 2×4 八宫格） */
    private Integer sort;

    /** 图标 fileKey */
    private String iconKey;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 软删除：0 正常 / 1 已删除 */
    private Integer isDeleted;

}
