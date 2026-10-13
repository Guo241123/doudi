package com.shequ.module.upload.service.impl;

import com.shequ.common.exception.BusinessException;
import com.shequ.common.result.ErrorCode;
import com.shequ.common.storage.FileUrlResolver;
import com.shequ.common.storage.LocalStorageService;
import com.shequ.common.storage.StorageProperties;
import com.shequ.module.upload.dto.UploadTokenDTO;
import com.shequ.module.upload.service.UploadService;
import com.shequ.module.upload.vo.UploadTokenVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * 直传服务实现（接口文档 #26）。
 * <p>
 * provider=local 时，uploadUrl 指向本服务的 {@code PUT /api/v1/upload/put}，用 URL 上的
 * HMAC 签名代替 OSS 的预签名（前端只带 headers、不带 Authorization，所以不能用登录态鉴权）。
 * 换成真 OSS 时只需把 uploadUrl 换成 OSS 预签名地址，前端与业务代码都不动。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final Map<String, String> LOCAL_HEADERS = Map.of("Content-Type", "application/octet-stream");
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final StorageProperties storageProperties;
    private final LocalStorageService localStorageService;
    private final FileUrlResolver fileUrlResolver;

    @Value("${jwt.secret}")
    private String signSecret;

    @Override
    public UploadTokenVO createToken(UploadTokenDTO dto) {
        if (!storageProperties.isLocal()) {
            // 诚实报错，避免线上误以为能上传：OSS 接入时在这里换成预签名实现
            throw new BusinessException(ErrorCode.SERVER_ERROR, "OSS 直传尚未接入，请将 storage.provider 设为 local");
        }
        String fileKey = buildFileKey(dto.getBiz(), dto.getFileType());
        long expireAt = System.currentTimeMillis() / 1000 + storageProperties.getTokenTtlSeconds();

        String base = fileUrlResolver.currentBaseUrl();
        if (!StringUtils.hasText(base)) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "无法确定服务对外地址，请配置 storage.public-base-url");
        }
        String uploadUrl = base + "/api/v1/upload/put"
                + "?fileKey=" + fileKey
                + "&expireAt=" + expireAt
                + "&signature=" + sign(fileKey, expireAt);

        return new UploadTokenVO(uploadUrl, fileKey, LOCAL_HEADERS);
    }

    @Override
    public void receive(String fileKey, long expireAt, String signature, byte[] content) {
        if (!storageProperties.isLocal()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (expireAt < System.currentTimeMillis() / 1000) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "上传凭据已过期，请重新获取");
        }
        if (!StringUtils.hasText(signature) || !MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                sign(fileKey, expireAt).getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "上传凭据校验失败");
        }
        if (content == null || content.length == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传内容为空");
        }
        localStorageService.store(fileKey, content);
    }

    /** {biz}/{yyyy}/{MM}/{dd}/{uuid}{ext}：按业务与日期分目录，便于按前缀清理。 */
    private String buildFileKey(String biz, String fileType) {
        String ext = "VIDEO".equals(fileType) ? ".mp4" : ".jpg";
        return biz.toLowerCase() + "/" + LocalDate.now().format(DATE_PATH) + "/"
                + UUID.randomUUID().toString().replace("-", "") + ext;
    }

    private String sign(String fileKey, long expireAt) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] raw = mac.doFinal((fileKey + "|" + expireAt).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "签名计算失败");
        }
    }
}
