package com.shequ.module.ads.service;

import com.shequ.module.ads.entity.AdReport;

import java.util.List;

/**
 * 广告上报服务（接口文档 #45：失败静默、服务端异步写）。
 */
public interface AdReportService {

    /**
     * 上报入队（异步批量落库）；失败静默不提示用户。
     */
    void enqueue(List<AdReport> reports);

}
