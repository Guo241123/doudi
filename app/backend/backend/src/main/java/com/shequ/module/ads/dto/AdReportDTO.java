package com.shequ.module.ads.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 广告上报请求体（接口文档 #45：单条或 items[] 批量）。
 * <p>
 * 两种形态的字段约束必须一致：批量形态靠内部类 {@link Item} 上的注解校验，
 * 单条形态则靠本类的顶层注解 + {@link #isPayloadPresent()}（否则单条形态传任意垃圾都能入库）。
 */
@Data
public class AdReportDTO {

    /** 单条形态 */
    @Positive(message = "adId 不合法")
    private Long adId;

    @Pattern(regexp = "TRADE_BANNER|TASK_BANNER|FEED|DETAIL|INTERSTITIAL",
            message = "广告位不合法")
    private String slot;

    @Pattern(regexp = "IMPRESSION|CLICK", message = "action 不合法")
    private String action;

    /** 批量形态：传了 items 则忽略单条字段 */
    @jakarta.validation.Valid
    private List<Item> items;

    /** 两种形态至少有一种填完整，否则视为空上报。 */
    @AssertTrue(message = "至少提供一组上报数据（adId + slot + action，或 items）")
    public boolean isPayloadPresent() {
        if (items != null && !items.isEmpty()) {
            return true;
        }
        return adId != null && StringUtils.hasText(slot) && StringUtils.hasText(action);
    }

    public List<Item> itemsOrSingle() {
        if (items != null && !items.isEmpty()) {
            return items;
        }
        List<Item> single = new ArrayList<>(1);
        single.add(new Item(adId, slot, action));
        return single;
    }

    @Data
    public static class Item {

        @NotNull(message = "adId 不能为空")
        private Long adId;

        @NotBlank(message = "slot 不能为空")
        @Pattern(regexp = "TRADE_BANNER|TASK_BANNER|FEED|DETAIL|INTERSTITIAL",
                message = "广告位不合法")
        private String slot;

        @NotBlank(message = "action 不能为空")
        @Pattern(regexp = "IMPRESSION|CLICK", message = "action 不合法")
        private String action;

        public Item() {
        }

        public Item(Long adId, String slot, String action) {
            this.adId = adId;
            this.slot = slot;
            this.action = action;
        }

    }

}
