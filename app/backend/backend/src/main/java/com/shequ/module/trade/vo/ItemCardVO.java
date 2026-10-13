package com.shequ.module.trade.vo;

import lombok.Data;

/**
 * 物品卡片（接口文档 2.3 #18 / #23 / #25 共用的基础结构）。
 * <p>
 * 序列化红线：不得出现卖家联系方式字段（contact 仅存档）。
 */
@Data
public class ItemCardVO {

    private Long id;

    private String title;

    /** 封面图或视频首帧 */
    private String coverUrl;

    /** 价格（分） */
    private Integer priceFen;

    private Boolean bargainable;

    /** 状态：ON_SALE 在售 / SOLD 已售（公开列表只出现这两种） */
    private String status;

    /** 当前登录用户是否已收藏（未登录恒 false） */
    private Boolean favorited;

}
