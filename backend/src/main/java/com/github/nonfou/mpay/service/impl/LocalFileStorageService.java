package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Value("${mpay.storage.upload-dir:uploads}")
    private String uploadDir;

    @Value("${mpay.storage.base-url:/uploads}")
    private String baseUrl;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            log.info("文件存储目录初始化完成: {}", rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("无法创建文件存储目录", e);
        }
    }

    @Override
    public String store(InputStream inputStream, String originalFilename, String contentType) {
        // 验证文件类型
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT,
                    "不支持的文件类型: " + extension + "，允许: " + ALLOWED_EXTENSIONS);
        }

        // 生成存储路径: /uploads/qrcode/2025/01/xxx.png
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String relativePath = "qrcode/" + datePath + "/" + filename;

        try {
            Path targetDir = rootLocation.resolve("qrcode/" + datePath);
            Files.createDirectories(targetDir);

            Path targetFile = rootLocation.resolve(relativePath);
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);

            log.info("文件上传成功: {}", targetFile);
            return baseUrl + "/" + relativePath;
        } catch (IOException e) {
            log.error("文件存储失败", e);
            throw new BusinessException(ErrorCode.SERVER_ERROR, "文件存储失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String filePath) {
        if (filePath == null || !filePath.startsWith(baseUrl)) {
            return;
        }

        String relativePath = filePath.substring(baseUrl.length() + 1);
        try {
            Path file = rootLocation.resolve(relativePath);
            Files.deleteIfExists(file);
            log.info("文件删除成功: {}", file);
        } catch (IOException e) {
            log.error("文件删除失败: {}", filePath, e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
