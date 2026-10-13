package com.shequ.module.analytics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shequ.common.auth.AuthInterceptor;
import com.shequ.common.result.Result;
import com.shequ.module.analytics.dto.EventsReportDTO;
import com.shequ.module.analytics.entity.AnalyticsEvent;
import com.shequ.module.analytics.service.AnalyticsEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 埋点上报接口（接口文档 #46，游客带设备号也统计；失败静默）。
 * MVP 红线五事件：APP_LAUNCH / PAGE_VIEW / AD_IMPRESSION / AD_CLICK / PUBLISH。
 */
@Slf4j
@Tag(name = "埋点")
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private static final DateTimeFormatter[] FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    private final AnalyticsEventService analyticsEventService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "#46 埋点批量上报")
    @PostMapping("/events")
    public Result<Void> report(HttpServletRequest request, @Valid @RequestBody EventsReportDTO dto) {
        Long userId = userId(request);
        List<AnalyticsEvent> events = new ArrayList<>(dto.getEvents().size());
        for (EventsReportDTO.EventItem item : dto.getEvents()) {
            // 未登录必须带设备号（接口文档 #46：游客带设备号）
            if (userId == null && (item.getDeviceId() == null || item.getDeviceId().isBlank())) {
                continue; // 静默丢弃不合规事件，不影响其余事件
            }
            AnalyticsEvent event = new AnalyticsEvent();
            event.setUserId(userId);
            event.setDeviceId(item.getDeviceId());
            event.setName(item.getName());
            event.setProps(toJson(item.getProps()));
            event.setEventTime(parseTime(item.getTime()));
            event.setCreateTime(LocalDateTime.now());
            events.add(event);
        }
        analyticsEventService.enqueue(events);
        return Result.ok();
    }

    private Long userId(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return value == null ? null : (long) value;
    }

    private String toJson(java.util.Map<String, Object> props) {
        if (props == null || props.isEmpty()) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(props);
            return json.length() > 500 ? json.substring(0, 500) : json;
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseTime(String time) {
        if (time == null || time.isBlank()) {
            return LocalDateTime.now();
        }
        for (DateTimeFormatter format : FORMATS) {
            try {
                return LocalDateTime.parse(time, format);
            } catch (Exception ignored) {
                // 试下一种格式
            }
        }
        return LocalDateTime.now();
    }

}
