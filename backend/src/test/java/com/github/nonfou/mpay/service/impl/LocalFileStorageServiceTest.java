package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LocalFileStorageService 单元测试
 * 测试本地文件存储服务
 */
@DisplayName("LocalFileStorageService 文件存储服务测试")
class LocalFileStorageServiceTest {

    private LocalFileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new LocalFileStorageService();
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(fileStorageService, "baseUrl", "/uploads");
        fileStorageService.init();
    }

    @Nested
    @DisplayName("文件上传测试")
    class StoreTests {

        @Test
        @DisplayName("上传成功 - PNG 文件")
        void store_shouldSucceed_forPngFile() throws IOException {
            // Given
            byte[] content = "fake png content".getBytes();
            InputStream inputStream = new ByteArrayInputStream(content);

            // When
            String url = fileStorageService.store(inputStream, "test.png", "image/png");

            // Then
            assertTrue(url.startsWith("/uploads/qrcode/"));
            assertTrue(url.endsWith(".png"));
        }

        @Test
        @DisplayName("上传成功 - JPG 文件")
        void store_shouldSucceed_forJpgFile() {
            // Given
            byte[] content = "fake jpg content".getBytes();
            InputStream inputStream = new ByteArrayInputStream(content);

            // When
            String url = fileStorageService.store(inputStream, "photo.jpg", "image/jpeg");

            // Then
            assertTrue(url.startsWith("/uploads/qrcode/"));
            assertTrue(url.endsWith(".jpg"));
        }

        @Test
        @DisplayName("上传成功 - JPEG 文件")
        void store_shouldSucceed_forJpegFile() {
            // Given
            byte[] content = "fake jpeg content".getBytes();
            InputStream inputStream = new ByteArrayInputStream(content);

            // When
            String url = fileStorageService.store(inputStream, "photo.jpeg", "image/jpeg");

            // Then
            assertTrue(url.endsWith(".jpeg"));
        }

        @Test
        @DisplayName("上传成功 - GIF 文件")
        void store_shouldSucceed_forGifFile() {
            // Given
            byte[] content = "fake gif content".getBytes();
            InputStream inputStream = new ByteArrayInputStream(content);

            // When
            String url = fileStorageService.store(inputStream, "animation.gif", "image/gif");

            // Then
            assertTrue(url.endsWith(".gif"));
        }

        @Test
        @DisplayName("上传成功 - WebP 文件")
        void store_shouldSucceed_forWebpFile() {
            // Given
            byte[] content = "fake webp content".getBytes();
            InputStream inputStream = new ByteArrayInputStream(content);

            // When
            String url = fileStorageService.store(inputStream, "image.webp", "image/webp");

            // Then
            assertTrue(url.endsWith(".webp"));
        }

        @Test
        @DisplayName("上传成功 - 文件实际存储到磁盘")
        void store_shouldSaveFileToStorage() throws IOException {
            // Given
            byte[] content = "test file content".getBytes();
            InputStream inputStream = new ByteArrayInputStream(content);

            // When
            String url = fileStorageService.store(inputStream, "test.png", "image/png");

            // Then
            String relativePath = url.substring("/uploads/".length());
            Path storedFile = tempDir.resolve(relativePath);
            assertTrue(Files.exists(storedFile));
            assertArrayEquals(content, Files.readAllBytes(storedFile));
        }

        @Test
        @DisplayName("上传成功 - 生成唯一文件名")
        void store_shouldGenerateUniqueFilename() {
            // Given
            byte[] content = "content".getBytes();

            // When
            String url1 = fileStorageService.store(new ByteArrayInputStream(content), "test.png", "image/png");
            String url2 = fileStorageService.store(new ByteArrayInputStream(content), "test.png", "image/png");

            // Then
            assertNotEquals(url1, url2);
        }

        @Test
        @DisplayName("上传失败 - 不支持的文件类型 PDF")
        void store_shouldThrowException_forUnsupportedPdfType() {
            // Given
            byte[] content = "pdf content".getBytes();
            InputStream inputStream = new ByteArrayInputStream(content);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> fileStorageService.store(inputStream, "document.pdf", "application/pdf"));

            assertEquals(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("不支持的文件类型"));
        }

        @Test
        @DisplayName("上传失败 - 不支持的文件类型 TXT")
        void store_shouldThrowException_forUnsupportedTxtType() {
            // Given
            byte[] content = "text content".getBytes();
            InputStream inputStream = new ByteArrayInputStream(content);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> fileStorageService.store(inputStream, "readme.txt", "text/plain"));

            assertEquals(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        }

        @Test
        @DisplayName("上传失败 - 不支持的文件类型 EXE")
        void store_shouldThrowException_forExeFile() {
            // Given
            byte[] content = "executable content".getBytes();
            InputStream inputStream = new ByteArrayInputStream(content);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> fileStorageService.store(inputStream, "program.exe", "application/octet-stream"));

            assertEquals(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        }

        @Test
        @DisplayName("上传成功 - 大小写不敏感的扩展名")
        void store_shouldSucceed_forUpperCaseExtension() {
            // Given
            byte[] content = "content".getBytes();
            InputStream inputStream = new ByteArrayInputStream(content);

            // When
            String url = fileStorageService.store(inputStream, "IMAGE.PNG", "image/png");

            // Then
            assertTrue(url.endsWith(".png"));
        }
    }

    @Nested
    @DisplayName("文件删除测试")
    class DeleteTests {

        @Test
        @DisplayName("删除成功 - 文件被删除")
        void delete_shouldRemoveFile_whenExists() throws IOException {
            // Given
            byte[] content = "content to delete".getBytes();
            String url = fileStorageService.store(new ByteArrayInputStream(content), "test.png", "image/png");

            String relativePath = url.substring("/uploads/".length());
            Path storedFile = tempDir.resolve(relativePath);
            assertTrue(Files.exists(storedFile));

            // When
            fileStorageService.delete(url);

            // Then
            assertFalse(Files.exists(storedFile));
        }

        @Test
        @DisplayName("删除忽略 - null 路径")
        void delete_shouldIgnore_whenPathIsNull() {
            // When & Then - 不抛出异常
            assertDoesNotThrow(() -> fileStorageService.delete(null));
        }

        @Test
        @DisplayName("删除忽略 - 不匹配的路径前缀")
        void delete_shouldIgnore_whenPathNotStartsWithBaseUrl() {
            // When & Then - 不抛出异常
            assertDoesNotThrow(() -> fileStorageService.delete("/other/path/file.png"));
        }

        @Test
        @DisplayName("删除忽略 - 文件不存在")
        void delete_shouldIgnore_whenFileNotExists() {
            // When & Then - 不抛出异常
            assertDoesNotThrow(() -> fileStorageService.delete("/uploads/qrcode/2025/01/nonexistent.png"));
        }
    }

    @Nested
    @DisplayName("初始化测试")
    class InitTests {

        @Test
        @DisplayName("初始化成功 - 创建存储目录")
        void init_shouldCreateUploadDirectory() {
            // Given
            LocalFileStorageService newService = new LocalFileStorageService();
            Path newDir = tempDir.resolve("new-uploads");
            ReflectionTestUtils.setField(newService, "uploadDir", newDir.toString());
            ReflectionTestUtils.setField(newService, "baseUrl", "/uploads");

            // When
            newService.init();

            // Then
            assertTrue(Files.exists(newDir));
        }

        @Test
        @DisplayName("初始化成功 - 目录已存在不报错")
        void init_shouldSucceed_whenDirectoryAlreadyExists() throws IOException {
            // Given
            LocalFileStorageService newService = new LocalFileStorageService();
            Path existingDir = tempDir.resolve("existing-uploads");
            Files.createDirectories(existingDir);
            ReflectionTestUtils.setField(newService, "uploadDir", existingDir.toString());
            ReflectionTestUtils.setField(newService, "baseUrl", "/uploads");

            // When & Then
            assertDoesNotThrow(() -> newService.init());
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("文件名无扩展名")
        void store_shouldThrowException_forFilenameWithoutExtension() {
            // Given
            byte[] content = "content".getBytes();
            InputStream inputStream = new ByteArrayInputStream(content);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> fileStorageService.store(inputStream, "noextension", "image/png"));

            assertEquals(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        }

        @Test
        @DisplayName("空扩展名")
        void store_shouldThrowException_forEmptyExtension() {
            // Given
            byte[] content = "content".getBytes();
            InputStream inputStream = new ByteArrayInputStream(content);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> fileStorageService.store(inputStream, "file.", "image/png"));

            assertEquals(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        }

        @Test
        @DisplayName("多个点的文件名")
        void store_shouldSucceed_forFilenameWithMultipleDots() {
            // Given
            byte[] content = "content".getBytes();
            InputStream inputStream = new ByteArrayInputStream(content);

            // When
            String url = fileStorageService.store(inputStream, "file.backup.2025.png", "image/png");

            // Then
            assertTrue(url.endsWith(".png"));
        }
    }
}
