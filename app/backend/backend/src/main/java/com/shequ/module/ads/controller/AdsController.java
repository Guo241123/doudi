package com.shequ.module.ads.controller;

import com.shequ.common.auth.AuthInterceptor;
import com.shequ.common.result.Result;
import com.shequ.module.ads.dto.AdReportDTO;
import com.shequ.module.ads.entity.AdReport;
import com.shequ.module.ads.service.AdReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 广告曝光 / 点击上报（接口文档 #45，游客也统计；失败静默不提示用户）。
 * 支持单条与 items[] 批量两种形态。
 */
@Tag(name = "广告")
@RestController
@RequestMapping("/api/v1/ads")
@RequiredArgsConstructor
public class AdsController {

    private final AdReportService adReportService;

    @Operation(summary = "#45 广告曝光 / 点击上报")
    @PostMapping("/report")
    public Result<Void> report(HttpServletRequest request, @Valid @RequestBody AdReportDTO dto) {
        Long userId = userId(request);
        List<AdReportDTO.Item> items = dto.itemsOrSingle();
        List<AdReport> reports = new ArrayList<>(items.size());
        for (AdReportDTO.Item item : items) {
            AdReport report = new AdReport();
            report.setAdId(item.getAdId());
            report.setSlot(item.getSlot());
            report.setAction(item.getAction());
            report.setUserId(userId);
            report.setCreateTime(LocalDateTime.now());
            reports.add(report);
        }
        adReportService.enqueue(reports);
        return Result.ok();
    }

    private Long userId(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return value == null ? null : (long) value;
    }

}
