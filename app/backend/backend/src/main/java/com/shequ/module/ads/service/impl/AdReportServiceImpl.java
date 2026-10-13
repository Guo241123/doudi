package com.shequ.module.ads.service.impl;

import com.shequ.module.ads.entity.AdReport;
import com.shequ.module.ads.mapper.AdReportMapper;
import com.shequ.module.ads.service.AdReportService;
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
 * 广告上报内存队列（与埋点同一思路，实现注记 9）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdReportServiceImpl implements AdReportService {

    private static final int QUEUE_CAPACITY = 10_000;
    private static final int BATCH_SIZE = 500;

    private final AdReportMapper adReportMapper;
    private final BlockingQueue<AdReport> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    @Override
    public void enqueue(List<AdReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return;
        }
        for (AdReport report : reports) {
            if (!queue.offer(report)) {
                queue.poll();
                log.warn("广告上报队列已满，丢弃最旧记录");
            }
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void flush() {
        List<AdReport> batch = new ArrayList<>(BATCH_SIZE);
        queue.drainTo(batch, BATCH_SIZE);
        if (batch.isEmpty()) {
            return;
        }
        try {
            for (AdReport report : batch) {
                adReportMapper.insert(report);
            }
        } catch (Exception e) {
            log.error("广告上报批量落库失败，丢弃 {} 条", batch.size(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        flush();
    }

}
