package com.github.nonfou.mpay.service;

import java.io.InputStream;

/**
 * 文件存储服务
 */
public interface FileStorageService {

    /**
     * 存储文件
     * @param inputStream 文件输入流
     * @param originalFilename 原始文件名
     * @param contentType 内容类型
     * @return 文件访问路径
     */
    String store(InputStream inputStream, String originalFilename, String contentType);

    /**
     * 删除文件
     * @param filePath 文件路径
     */
    void delete(String filePath);
}
