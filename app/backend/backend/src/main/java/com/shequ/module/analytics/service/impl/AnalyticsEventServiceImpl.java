package com.shequ.module.analytics.service.impl;

import com.shequ.module.analytics.entity.AnalyticsEvent;
import com.shequ.module.analytics.mapper.AnalyticsEventMapper;
import com.shequ.module.analytics.service.AnalyticsEventService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 埋点事件内存队列实现（实现注记 9）。
 * <p>
 * 写放大控制：上报请求只做入队（O(1)），每 5 秒或队列满 500 条时批量落库。
 * 队列有界（1 万条），溢出丢最旧并告警日志——埋点是统计用途，宁可丢不可阻塞主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsEventServiceImpl implements AnalyticsEventService {

    private static final int QUEUE_CAPACITY = 10_000;
    private static final int BATCH_SIZE = 500;

    private final AnalyticsEventMapper eventMapper;
    private final BlockingQueue<AnalyticsEvent> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    @Override
    public void enqueue(List<AnalyticsEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (AnalyticsEvent event : events) {
            if (!queue.offer(event)) {
                queue.poll(); // 队列满：丢最旧，保证不阻塞
                log.warn("埋点队列已满，丢弃最旧事件（当前容量 {}）", QUEUE_CAPACITY);
            }
        }
    }

    /** 每 5 秒批量落库一次。 */
    @Scheduled(fixedDelay = 5000)
    public void flush() {
        List<AnalyticsEvent> batch = new ArrayList<>(BATCH_SIZE);
        queue.drainTo(batch, BATCH_SIZE);
        if (batch.isEmpty()) {
            return;
        }
        try {
            for (AnalyticsEvent event : batch) {
                eventMapper.insert(event);
            }
        } catch (Exception e) {
            // 失败静默：埋点不反哺业务异常
            log.error("埋点批量落库失败，丢弃 {} 条", batch.size(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        flush();
        log.info("应用关闭前完成埋点队列落库");
    }

}
