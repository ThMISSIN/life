package com.life.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地磁盘存储（OSS 不可用时的兜底实现）
 * 文件保存到 {user.dir}/uploads/{yyyy}/{MM}/{objectName}，
 * 由 WebMvcConfiguration 的 /uploads/** 静态映射对外提供访问。
 * 通过 life.storage.type=oss 可切回阿里云 OSS。
 */
@Component
@Slf4j
public class LocalStorageUtil {

    @Value("${life.local-storage.base-path:uploads}")
    private String basePath;

    /**
     * @param bytes      文件字节
     * @param objectName 对象名（UUID+后缀）
     * @return 访问 URL（相对路径 /uploads/...，开发环境由 vite 代理到后端静态映射）
     */
    public String upload(byte[] bytes, String objectName) {
        try {
            Path root = Paths.get(basePath).toAbsolutePath();
            String[] parts = objectName.split("/");
            Path dir = root;
            // objectName 若带 yyyy/MM 前缀则按目录分层
            for (int i = 0; i < parts.length - 1; i++) {
                dir = dir.resolve(parts[i]);
            }
            Files.createDirectories(dir);
            Path target = dir.resolve(parts[parts.length - 1]);
            Files.write(target, bytes);
            String url = "/uploads/" + objectName;
            log.info("文件保存到本地: {}", target);
            return url;
        } catch (Exception e) {
            log.error("本地文件保存失败: {}", e.getMessage());
            throw new RuntimeException("文件保存失败: " + e.getMessage(), e);
        }
    }
}
