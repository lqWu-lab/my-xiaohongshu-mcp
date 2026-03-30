package com.lqwu.mcp.server.xiaohongshu.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 图片下载工具类
 */
@Slf4j
@Service
public class ImageDownloader {

    /**
     * 下载图片
     * @param imageUrl 图片 URL
     * @param savePath 保存路径
     * @return 是否下载成功
     */
    public boolean downloadImage(String imageUrl, String savePath) {
        try {
            // 1.创建保存目录
            Path saveDir = Paths.get(savePath).getParent();
            if (saveDir != null && !Files.exists(saveDir)) {
                Files.createDirectories(saveDir);
            }

            // 2.建立连接
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            // 3.检查响应状态
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                log.warn("Failed to download image: HTTP {}", connection.getResponseCode());
                return false;
            }

            // 4.读取和写入文件
            try (InputStream inputStream = connection.getInputStream();
                 OutputStream outputStream = new FileOutputStream(savePath)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            log.debug("Downloaded image successfully: {}", savePath);
            return true;
        } catch (Exception e) {
            log.warn("Failed to download image: {} - {}", imageUrl, e.getMessage());
            return false;
        }
    }

    /**
     * 下载图片到默认目录
     * @param imageUrl 图片 URL
     * @param fileName 文件名
     * @return 保存路径
     */
    public String downloadImage(String imageUrl, String directory, String fileName) {
        String savePath = Paths.get(directory, fileName).toString();
        if (downloadImage(imageUrl, savePath)) {
            return savePath;
        }
        return null;
    }
}

