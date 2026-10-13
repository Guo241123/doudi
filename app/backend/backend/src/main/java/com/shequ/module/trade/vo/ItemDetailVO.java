package com.shequ.module.trade.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物品详情（接口文档 2.3 #19）：#18 全部字段 + 轮播 / 介绍 / 卖家 / 会话与免责标识。
 * <p>
 * 序列化红线：不含联系方式字段。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ItemDetailVO extends ItemCardVO {

    /** 图片 URL 数组（按 sort 升序） */
    private List<String> images;

    /** 视频 URL（无则空） */
    private String videoUrl;

    private String description;

    /** 发布时间 */
    private LocalDateTime publishTime;

    private SellerVO seller;

    /** 是否可与卖家发起会话（登录且非本人且在售） */
    private Boolean canChat;

    /** 免责声明文案（★ 后端下发） */
    private String disclaimer;

    /** 是否本人发布 */
    @JsonProperty("isMine")
    private Boolean isMine;

}
