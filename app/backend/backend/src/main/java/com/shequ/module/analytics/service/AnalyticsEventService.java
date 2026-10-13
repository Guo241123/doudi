package com.shequ.module.analytics.service;

import com.shequ.module.analytics.entity.AnalyticsEvent;

import java.util.List;

/**
 * 埋点事件接收服务（实现注记 9：内存队列批量落表，1 万用户量级无需 MQ）。
 */
public interface AnalyticsEventService {

    /**
     * 事件入队（异步批量落库）；入队即视为成功，失败静默（前端不提示）。
     */
    void enqueue(List<AnalyticsEvent> events);

}
