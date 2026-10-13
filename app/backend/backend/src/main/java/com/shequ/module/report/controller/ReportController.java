package com.shequ.module.report.controller;

import com.shequ.common.auth.AuthInterceptor;
import com.shequ.common.result.Result;
import com.shequ.module.report.dto.ReportSubmitDTO;
import com.shequ.module.report.service.ReportService;
import com.shequ.module.report.vo.ReportMetaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 举报接口（接口文档 #41/#42，App 端"···"按钮入口）。
 */
@Tag(name = "举报")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "#41 举报页元数据")
    @GetMapping("/meta")
    public Result<ReportMetaVO> meta() {
        return Result.ok(reportService.meta());
    }

    @Operation(summary = "#42 提交举报")
    @PostMapping
    public Result<Void> submit(HttpServletRequest request, @Valid @RequestBody ReportSubmitDTO dto) {
        long userId = userId(request);
        reportService.submit(userId, dto);
        return Result.ok();
    }

    private long userId(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return value == null ? 0L : (long) value;
    }

}
