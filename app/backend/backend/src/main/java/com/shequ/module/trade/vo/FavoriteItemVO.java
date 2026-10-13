package com.shequ.module.trade.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 我的收藏列表项（接口文档 2.3 #25）：#18 卡片结构 + 不可用标记。
 * <p>
 * 已售 / 已下架的物品仍返回，但置 {@code unavailable = true} 供前端置灰。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FavoriteItemVO extends ItemCardVO {

    /** 是否已不可交易（非在售状态） */
    private Boolean unavailable;

}
