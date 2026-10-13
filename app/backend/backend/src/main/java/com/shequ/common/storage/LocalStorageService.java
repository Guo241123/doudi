package com.shequ.common.storage;

import com.shequ.common.exception.BusinessException;
import com.shequ.common.result.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地磁盘存储：把直传的字节按 fileKey 落盘到 {@link StorageProperties#getLocalDir()}。
 * <p>
 * 这不是"假实现"——它是一套可用的本地存储后端，只是把小区的 4C8G 单机磁盘当成 OSS 用。
 * 上线前切 provider=oss 后本类不再参与，业务代码与前端都无需改动。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalStorageService {

    private final StorageProperties properties;

    @PostConstruct
    public void init() {
        if (!properties.isLocal()) {
            return;
        }
        try {
            Files.createDirectories(root());
            log.info("[Storage] 本地存储已启用，根目录={}，静态访问前缀=/files/**", root());
        } catch (IOException e) {
            throw new UncheckedIOException("本地存储目录创建失败：" + root(), e);
        }
    }

    public Path root() {
        return Paths.get(properties.getLocalDir()).toAbsolutePath().normalize();
    }

    /** 写入文件；路径越界（如 ../）直接拒绝，防目录穿越。 */
    public void store(String fileKey, byte[] content) {
        Path target = safeResolve(fileKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "文件写入失败");
        }
    }

    private Path safeResolve(String fileKey) {
        Path base = root();
        Path target = base.resolve(fileKey).normalize();
        if (!target.startsWith(base)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件路径非法");
        }
        return target;
    }
}
