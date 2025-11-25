package com.github.nonfou.mpay.controller;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.common.response.ApiResponse;
import com.github.nonfou.mpay.entity.PayChannelEntity;
import com.github.nonfou.mpay.repository.PayChannelRepository;
import com.github.nonfou.mpay.service.FileStorageService;
import java.io.IOException;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 二维码上传接口 - P1 功能
 */
@RestController
@RequestMapping("/api/channels")
public class ChannelUploadController {

    private final FileStorageService fileStorageService;
    private final PayChannelRepository channelRepository;

    public ChannelUploadController(FileStorageService fileStorageService,
            PayChannelRepository channelRepository) {
        this.fileStorageService = fileStorageService;
        this.channelRepository = channelRepository;
    }

    /**
     * 上传二维码图片
     * POST /api/channels/{id}/qrcode/upload
     */
    @PostMapping("/{id}/qrcode/upload")
    public ApiResponse<Map<String, String>> uploadQrcode(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "请选择要上传的文件");
        }

        // 验证文件大小 (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "文件大小不能超过5MB");
        }

        // 验证通道存在
        PayChannelEntity channel = channelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "通道不存在: " + id));

        try {
            // 存储文件
            String fileUrl = fileStorageService.store(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType());

            // 删除旧的二维码图片（如果是图片类型）
            if ("1".equals(channel.getType()) && channel.getQrcode() != null) {
                fileStorageService.delete(channel.getQrcode());
            }

            // 更新通道二维码
            channel.setQrcode(fileUrl);
            channel.setType("1"); // 1 表示图片类型
            channelRepository.save(channel);

            return ApiResponse.success(Map.of(
                    "url", fileUrl,
                    "channelId", String.valueOf(id)
            ));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "文件上传失败: " + e.getMessage());
        }
    }
}
