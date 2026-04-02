package com.lqwu.mcp.server.xiaohongshu.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.SameSiteAttribute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
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
     * 检查Cookie文件是否存在
     */
    public boolean isCookieFileExists() {
        Path cookiePath = Paths.get(getCookiesFilePath());
        return Files.exists(cookiePath);
    }

    /**
     * 从配置文件中的Cookie字符串生成Cookie文件
     * 支持格式: "name1=value1; name2=value2" 或 JSON 格式
     * @param cookieString 从application.yml读取的cookie字符串
     * @return 是否成功生成
     */
    public boolean generateCookieFileFromConfig(String cookieString) {
        if (cookieString == null || cookieString.trim().isEmpty()) {
            log.warn("配置的Cookie为空，无法生成Cookie文件");
            return false;
        }

        if ("填写你的cookie".equals(cookieString.trim())) {
            log.warn("Cookie配置为默认值'填写你的cookie'，请先配置有效的Cookie");
            return false;
        }

        try {
            List<Cookie> cookies = parseCookieString(cookieString);
            if (cookies.isEmpty()) {
                log.warn("解析Cookie字符串结果为空");
                return false;
            }

            saveCookies(cookies);
            log.info("已从配置生成Cookie文件，共 {} 个Cookie", cookies.size());
            return true;
        } catch (Exception e) {
            log.error("从配置生成Cookie文件失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 解析Cookie字符串为Cookie对象列表
     * 支持格式:
     * 1. "name1=value1; name2=value2" (HTTP Header格式)
     * 2. JSON格式: [{"name":"x","value":"y","domain":".xiaohongshu.com"}]
     */
    private List<Cookie> parseCookieString(String cookieString) {
        List<Cookie> cookies = new ArrayList<>();
        String trimmed = cookieString.trim();

        // 尝试解析JSON格式
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            try {
                return parseJsonCookies(trimmed);
            } catch (Exception e) {
                log.debug("不是JSON格式，尝试按分号分隔解析");
            }
        }

        // 按分号分隔解析 (HTTP Header格式)
        String[] pairs = trimmed.split(";");
        for (String pair : pairs) {
            String[] kv = pair.trim().split("=", 2);
            if (kv.length == 2) {
                String name = kv[0].trim();
                String value = kv[1].trim();

                // 创建Cookie对象
                Cookie cookie = new Cookie(name, value);
                cookie.domain = ".xiaohongshu.com";
                cookie.path = "/";
                // 设置过期时间为一年后 (Playwright Cookie.expires 是 Double 类型，表示秒级时间戳)
                cookie.expires = (double) (System.currentTimeMillis() / 1000 + 365L * 24 * 60 * 60);

                cookies.add(cookie);
            }
        }

        return cookies;
    }

    /**
     * 解析JSON格式的Cookie字符串
     */
    private List<Cookie> parseJsonCookies(String jsonString) throws Exception {
        List<Cookie> cookies = new ArrayList<>();

        if (jsonString.startsWith("[")) {
            // 数组格式
            List<?> cookieList = objectMapper.readValue(jsonString, List.class);
            for (Object obj : cookieList) {
                if (obj instanceof java.util.Map) {
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) obj;
                    String name = getStringValue(map, "name");
                    String value = getStringValue(map, "value");

                    if (name != null && value != null) {
                        Cookie cookie = new Cookie(name, value);
                        cookie.domain = getStringValue(map, "domain", ".xiaohongshu.com");
                        cookie.path = getStringValue(map, "path", "/");

                        // expires 是 Double 类型
                        long expiresLong = getLongValue(map, "expires", 0);
                        if (expiresLong > 0) {
                            cookie.expires = (double) expiresLong;
                        }

                        cookie.httpOnly = getBooleanValue(map, "httpOnly", false);
                        cookie.secure = getBooleanValue(map, "secure", true);

                        // sameSite 是 SameSiteAttribute 枚举类型
                        String sameSiteStr = getStringValue(map, "sameSite", "Lax");
                        cookie.sameSite = parseSameSite(sameSiteStr);

                        cookies.add(cookie);
                    }
                }
            }
        }

        return cookies;
    }

    /**
     * 解析 SameSite 字符串为枚举
     */
    private SameSiteAttribute parseSameSite(String sameSite) {
        if (sameSite == null) {
            return SameSiteAttribute.LAX;
        }
        return switch (sameSite.toUpperCase()) {
            case "STRICT" -> SameSiteAttribute.STRICT;
            case "NONE" -> SameSiteAttribute.NONE;
            default -> SameSiteAttribute.LAX;
        };
    }

    private String getStringValue(java.util.Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private String getStringValue(java.util.Map<?, ?> map, String key, String defaultValue) {
        String value = getStringValue(map, key);
        return value != null ? value : defaultValue;
    }

    private long getLongValue(java.util.Map<?, ?> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return defaultValue;
    }

    private boolean getBooleanValue(java.util.Map<?, ?> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }
        return defaultValue;
    }

    /**
     * 保存Cookie到文件
     */
    public void saveCookies(List<Cookie> cookies) {
        try {
            String cookiePath = getCookiesFilePath();
            String cookiesJson = objectMapper.writeValueAsString(cookies);
            Files.writeString(Paths.get(cookiePath), cookiesJson);
            log.debug("Cookies saved to: {}", cookiePath);
        } catch (Exception e) {
            log.warn("Failed to save cookies: {}", e.getMessage());
        }
    }

    /**
     * 从文件加载Cookie
     */
    public List<Cookie> loadCookies() {
        try {
            String cookiePath = getCookiesFilePath();
            Path path = Paths.get(cookiePath);
            if (!Files.exists(path)) {
                return new ArrayList<>();
            }
            String cookiesJson = Files.readString(path);
            return objectMapper.readValue(cookiesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Cookie.class));
        } catch (Exception e) {
            log.warn("Failed to load cookies: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 删除Cookie文件
     */
    public void removeCookies() {
        try {
            String cookiePath = getCookiesFilePath();
            Files.deleteIfExists(Paths.get(cookiePath));
            log.info("Cookies file removed");
        } catch (Exception e) {
            log.warn("Failed to remove cookies: {}", e.getMessage());
        }
    }
}
