package com.shequ.common.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置。
 * <p>
 * 项目约定 fileKey → URL 的解析统一走 {@link FileUrlResolver}，各业务模块不再各自拼 OSS 地址。
 * 开发/联调期 {@code provider=local}：文件落本地磁盘、由 {@code /files/**} 静态暴露，
 * 不需要任何第三方服务即可跑通"上传→发布→展示"整条链路；上线前把 provider 换成 oss 并配好
 * {@code oss.*} 即可，业务代码无需改动。
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** 存储后端：local（本地磁盘）/ oss（阿里云 OSS） */
    private String provider = "local";

    /** 本地存储根目录（provider=local 时生效） */
    private String localDir = "./data/upload";

    /**
     * 对外访问基址（如 https://cdn.example.com）。留空则按当前请求的 Host 推导，
     * 这样真机调试时自动拿到局域网 IP，不需要手工改配置。
     */
    private String publicBaseUrl = "";

    /** 直传签名有效期（秒） */
    private long tokenTtlSeconds = 600L;

    public boolean isLocal() {
        return "local".equalsIgnoreCase(provider);
    }
}
