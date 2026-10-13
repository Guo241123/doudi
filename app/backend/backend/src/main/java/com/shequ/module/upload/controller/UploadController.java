package com.shequ.module.upload.controller;

import com.shequ.common.result.Result;
import com.shequ.module.upload.dto.UploadTokenDTO;
import com.shequ.module.upload.service.UploadService;
import com.shequ.module.upload.vo.UploadTokenVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 媒体直传接口（接口文档 2.5 #26）。
 * <p>
 * 两个端点：
 * <ul>
 *   <li>{@code POST /token}：需登录，换取直传凭据</li>
 *   <li>{@code PUT /put}：直传目标，用 URL 上的 HMAC 签名鉴权（前端只带 headers 不带 Authorization，
 *       故在 {@code AuthInterceptor} 里放行，由签名兜底）</li>
 * </ul>
 */
@Slf4j
@Tag(name = "媒体直传")
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @Operation(summary = "#26 获取直传签名")
    @PostMapping("/token")
    public Result<UploadTokenVO> token(@Valid @RequestBody UploadTokenDTO dto) {
        return Result.ok(uploadService.createToken(dto));
    }

    @Operation(summary = "直传落盘（provider=local 时的上传目标）")
    @PutMapping("/put")
    public Result<Void> put(@RequestParam String fileKey,
                            @RequestParam long expireAt,
                            @RequestParam String signature,
                            HttpServletRequest request) throws IOException {
        // 直接读原始字节流：前端以二进制 body 发送，不走 JSON 消息转换
        uploadService.receive(fileKey, expireAt, signature, request.getInputStream().readAllBytes());
        return Result.ok();
    }
}
