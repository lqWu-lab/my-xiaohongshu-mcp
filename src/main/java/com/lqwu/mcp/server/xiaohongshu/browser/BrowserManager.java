package com.lqwu.mcp.server.xiaohongshu.browser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqwu.mcp.server.xiaohongshu.config.BrowserConfig;
import com.lqwu.mcp.server.xiaohongshu.util.CookieManager;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 浏览器管理器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrowserManager {

    private final CookieManager cookieManager;
    private final ObjectMapper objectMapper;
    
    // 管理浏览器实例
    private final Map<String, PageWrapper> browserPool = new HashMap<>();

    /**
     * 新建浏览器
     * @param config 浏览器配置
     * @return 页面包装器
     */
    public PageWrapper newBrowser(BrowserConfig config) {
        // 1.配置 Playwright
        Playwright playwright = Playwright.create();
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(config.isHeadless());
        // 2.设置浏览器路径
        if (config.getBinPath() != null && !config.getBinPath().isEmpty()) {
            launchOptions.setExecutablePath(Paths.get(config.getBinPath()));
        }
        // 3.处理代理
        String proxy = System.getenv("XHS_PROXY");
        if (proxy != null && !proxy.isEmpty()) {
            // 使用 Playwright 的 Proxy 选项
            launchOptions.setProxy(new com.microsoft.playwright.options.Proxy(proxy));
            log.info("Using proxy: {}" , maskProxyCredentials(proxy));
        }
        // 4.启动浏览器
        Browser browser = playwright.chromium().launch(launchOptions);
        Page page = browser.newPage();
        // 5.加载 Cookies
        loadCookies(page);
            
        // 6.包装并管理资源
        String sessionId = generateSessionId();
        PageWrapper wrapper = new PageWrapper(playwright, browser, page, sessionId);
        browserPool.put(sessionId, wrapper);
            
        log.info("Created new browser session: {}", sessionId);
        return wrapper;
    }

    /**
     * 加载 Cookies
     * @param page 页面
     */
    private void loadCookies(Page page) {
        // 1.获取 Cookies 文件路径
        String cookiePath = cookieManager.getCookiesFilePath();
        try {
            // 2.检查文件是否存在
            if (!Files.exists(Paths.get(cookiePath))) {
                log.debug("Cookie file not found, skipping load");
                return;
            }
            // 3.读取 cookie 文件内容
            String cookiesJson = Files.readString(Paths.get(cookiePath));
            if (cookiesJson.isEmpty()) {
                log.debug("Cookie file is empty, skipping load");
                return;
            }
            // 4.解析 JSON 为 Map 列表，然后转换为 Cookie
            List<Map<String, Object>> cookieMaps = objectMapper.readValue(
                    cookiesJson,
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            
            // 将 Map 转换为 Playwright Cookie 对象
            List<Cookie> cookies = cookieMaps.stream()
                    .map(this::convertToCookie)
                    .collect(Collectors.toList());
            
            // 5.加载 cookies 到页面上下文
            page.context().addCookies(cookies);
            log.debug("Loaded {} cookies from file successfully", cookies.size());
        } catch (Exception e) {
            log.warn("Failed to load cookies: {}" , e.getMessage());
        }
    }

    /**
     * 将 Map 转换为 Playwright Cookie 对象
     */
    private Cookie convertToCookie(Map<String, Object> map) {
        Cookie cookie = new Cookie((String) map.get("name"), (String) map.get("value"));
        
        String domain = (String) map.get("domain");
        if (domain != null) {
            cookie.setDomain(domain);
        }
        
        String path = (String) map.get("path");
        if (path != null) {
            cookie.setPath(path);
        }
        
        Double expires = (Double) map.get("expires");
        if (expires != null) {
            cookie.setExpires(expires);
        }
        
        Boolean httpOnly = (Boolean) map.get("httpOnly");
        if (httpOnly != null) {
            cookie.setHttpOnly(httpOnly);
        }
        
        Boolean secure = (Boolean) map.get("secure");
        if (secure != null) {
            cookie.setSecure(secure);
        }
        
        return cookie;
    }

    /**
     * 关闭指定会话的浏览器
     * @param sessionId 会话 ID
     */
    public void closeBrowser(String sessionId) {
        PageWrapper wrapper = browserPool.remove(sessionId);
        if (wrapper != null) {
            wrapper.close();
            log.info("Closed browser session: {}", sessionId);
        }
    }
    
    /**
     * 关闭所有浏览器
     */
    public void closeAll() {
        browserPool.values().forEach(wrapper -> {
            wrapper.close();
            log.info("Closed all browser session: {}", wrapper.getSessionId());
        });
        browserPool.clear();
    }
    
    /**
     * 生成会话 ID
     */
    private String generateSessionId() {
        byte[] randomBytes = new byte[16];
        new SecureRandom().nextBytes(randomBytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return "session_" + System.currentTimeMillis() + "_" + randomPart;
    }
    
    /**
     * 掩码代理 URL 中的用户信息
     * @param proxyURL 代理 URL
     * @return 掩码后的代理 URL
     */
    public static String maskProxyCredentials(String proxyURL) {
        try {
            URI uri = new URI(proxyURL);
            if (uri.getUserInfo() != null) {
                // 替换用户信息为***
                String masked = proxyURL.replace(uri.getUserInfo(), "***:***");
                return masked;
            }
        } catch (URISyntaxException e) {
            log.debug("掩码代理失败：{}" , e.getMessage());
            // 解析失败，返回原 URL
        }
        return proxyURL;
    }
}
