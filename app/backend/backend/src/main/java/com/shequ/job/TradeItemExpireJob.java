package com.shequ.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shequ.module.trade.entity.ItemStatus;
import com.shequ.module.trade.entity.TradeItem;
import com.shequ.module.trade.mapper.TradeItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 二手物品超期自动下架（数据库设计 四、6：expire_at = 发布时间 + N 天）。
 * <p>
 * 只扫"在售且已过期"的行（走 idx_expire(status, expire_at)），直接置为已下架。
 * 间隔与天数可配：trade.expire-check-cron / trade.expire-days。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeItemExpireJob {

    private final TradeItemMapper tradeItemMapper;

    @Scheduled(cron = "${trade.expire-check-cron:0 0/10 * * * ?}")
    public void offShelfExpired() {
        LocalDateTime now = LocalDateTime.now();
        int rows = tradeItemMapper.update(null, Wrappers.<TradeItem>lambdaUpdate()
                .set(TradeItem::getStatus, ItemStatus.OFF_SHELF.code())
                .set(TradeItem::getUpdateTime, now)
                .eq(TradeItem::getStatus, ItemStatus.ON_SALE.code())
                .lt(TradeItem::getExpireAt, now));
        if (rows > 0) {
            log.info("超期自动下架二手物品 {} 件", rows);
        }
    }

}
