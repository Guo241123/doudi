package com.shequ.module.analytics.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 埋点批量上报请求体（接口文档 #46）。
 */
@Data
public class EventsReportDTO {

    @NotEmpty(message = "events 不能为空")
    @Size(max = 50, message = "单次最多上报 50 条事件")
    @Valid
    private List<EventItem> events;

    @Data
    public static class EventItem {

        @NotBlank(message = "事件名不能为空")
        @Pattern(regexp = "APP_LAUNCH|PAGE_VIEW|AD_IMPRESSION|AD_CLICK|PUBLISH",
                message = "事件名不合法")
        private String name;

        /** 事件属性（如 page=forum），可空 */
        private Map<String, Object> props;

        /** 事件发生时间（客户端时间，可空 = 服务端时间兜底） */
        private String time;

        /** 设备号：未登录时必填（服务端校验） */
        private String deviceId;

    }

}
