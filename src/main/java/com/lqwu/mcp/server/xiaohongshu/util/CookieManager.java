package com.lqwu.mcp.server.xiaohongshu.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.options.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Cookie 管理工具类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CookieManager {

    private final ObjectMapper objectMapper;
    private static final String COOKIES_FILE_NAME = "cookies.json";

    /**
     * 获取Cookie文件路径
     */
    public String getCookiesFilePath() {
        // 1.默认存储在用户目录下
        Path userHome = Paths.get(System.getProperty("user.home"));
        Path cookiesPath = userHome.resolve(".xiaohongshu-mcp").resolve(COOKIES_FILE_NAME);
        // 2.确保目录存在
        cookiesPath.getParent().toFile().mkdirs();
        return cookiesPath.toString();
    }

    /**
     * 保存Cookie到文件
     */
    public void saveCookies(List<Cookie> cookies) {
        try {
            String cookiePath = getCookiesFilePath();
            String cookiesJson = objectMapper.writeValueAsString(cookies);
            Files.writeString(Paths.get(cookiePath), cookiesJson);
        } catch (Exception e) {
            log.warn("Failed to save cookies: {}" , e.getMessage());
        }
    }
    
}
