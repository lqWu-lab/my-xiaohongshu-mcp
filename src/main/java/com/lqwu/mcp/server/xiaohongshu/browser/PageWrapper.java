package com.lqwu.mcp.server.xiaohongshu.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 页面包装类 - 用于管理浏览器资源生命周期
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public class PageWrapper {
    
    private final Playwright playwright;
    private final Browser browser;
    private final Page page;
    private final String sessionId;
    
    /**
     * 关闭所有资源
     */
    public void close() {
        try {
            // 按顺序关闭：Page -> Browser -> Playwright
            if (page != null) {
                page.close();
            }
            if (browser != null) {
                browser.close();
            }
            if (playwright != null) {
                playwright.close();
            }
            log.debug("Session {} resources closed successfully", sessionId);
        } catch (Exception e) {
            log.warn("Failed to close session {} resources: {}", sessionId, e.getMessage());
        }
    }
}
