package com.shequ.module.trade.vo;

import lombok.Data;

/**
 * 物品卖家简要信息（接口文档 2.3 #19 的 seller 字段）。
 * <p>
 * 只暴露展示所需的最小字段，不含手机号等隐私信息。
 */
@Data
public class SellerVO {

    private Long userId;

    private String nickname;

    /** 头像完整 URL */
    private String avatar;

}
