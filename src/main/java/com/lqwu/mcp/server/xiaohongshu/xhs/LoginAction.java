package com.lqwu.mcp.server.xiaohongshu.xhs;

import com.lqwu.mcp.server.xiaohongshu.browser.PageWrapper;
import com.lqwu.mcp.server.xiaohongshu.util.CookieManager;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.LoadState;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 小红书登录操作类
 *
 * @author lqwu
 * @version 1.0
 * @see PageWrapper
 * @see CookieManager
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAction {

    private static final String XIAOHONGSHU_URL = "https://www.xiaohongshu.com/explore";
    private static final String USER_ELEMENT_SELECTOR = ".main-container .user .link-wrapper .channel";
    private static final String QRCODE_ELEMENT_SELECTOR = ".login-container .qrcode-img";

    @Setter
    private PageWrapper pageWrapper;
    private final CookieManager cookieManager;

    /**
     * 检查登录状态
     */
    public boolean checkLoginStatus() {
        Page page = pageWrapper.getPage();
        page.navigate(XIAOHONGSHU_URL);
        page.waitForLoadState(LoadState.LOAD);
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        try {
            Locator userElement = page.locator(USER_ELEMENT_SELECTOR);
            return userElement.count() > 0;
        } catch (Exception e) {
            log.debug("User element not found, not logged in");
            return false;
        }
    }

    /**
     * 检查登录状态（不导航）
     */
    private boolean checkLoginStatusWithoutNavigate() {
        Page page = pageWrapper.getPage();
        try {
            Locator userElement = page.locator(USER_ELEMENT_SELECTOR);
            return userElement.count() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 执行登录流程
     */
    public void login(int timeoutSeconds) {
        // 1. 导航到小红书探索页 - 这会自动弹出登录二维码
        Page page = pageWrapper.getPage();
        page.navigate(XIAOHONGSHU_URL);
        page.waitForLoadState(LoadState.LOAD);
        // 2. 检查是否已登录
        if (checkLoginStatusWithoutNavigate()) {
            log.info("Already logged in");
            saveCookies();
            return;
        }
        // 3. 等待用户扫码登录
        boolean loginSuccess = waitForLogin(timeoutSeconds * 1000L);
        if (loginSuccess) {
            log.info("Login successful");
            saveCookies();
        } else {
            log.warn("Login timed out after {} seconds", timeoutSeconds);
        }
    }

    /**
     * 获取登录二维码图片的URL
     *
     * @return 二维码图片的URL，如果已登录返回null，失败返回null
     */
    public String fetchQrcodeImage() {
        Page page = pageWrapper.getPage();
        
        try {
            // 1. 导航到小红书探索页
            log.info("导航到小红书探索页...");
            page.navigate(XIAOHONGSHU_URL);
            // 使用 LOAD 而不是 NETWORKIDLE，对应 Go 版本的 MustWaitLoad()
            page.waitForLoadState(LoadState.LOAD);
            
            // 2. 等待2秒让页面完全加载
            log.info("等待页面加载...");
            Thread.sleep(2000);
            
            // 3. 检查是否已经登录
            if (checkLoginStatusWithoutNavigate()) {
                log.info("Already logged in, no QR code needed");
                return null;
            }
            
            // 4. 获取二维码图片
            log.info("尝试获取二维码图片，选择器: {}", QRCODE_ELEMENT_SELECTOR);
            String src = page.locator(QRCODE_ELEMENT_SELECTOR).getAttribute("src");
            
            if (src == null || src.isEmpty()) {
                log.warn("二维码 src 为空");
                return null;
            }
            
            log.info("成功获取二维码图片 URL: {}", src);
            return src;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取二维码被中断", e);
            return null;
        } catch (Exception e) {
            log.error("获取二维码时发生错误", e);
            return null;
        }
    }

    /**
     * 等待用户扫码登录
     * 扫码后页面会重渲染完成，然后用户元素才会出现。
     * @param timeoutMillis 超时时间（毫秒）
     * @return 是否登录成功
     */
    public boolean waitForLogin(long timeoutMillis) {
        log.info("Waiting for user to scan QR code... (timeout: {}ms)", timeoutMillis);

        try {
            // 获取 page 引用
            Page page = pageWrapper.getPage();

            // 使用 waitForSelector 等待元素出现（会阻塞）
            // 这比轮询更可靠，因为它会在元素出现时立即返回
            page.waitForSelector(USER_ELEMENT_SELECTOR,
                new Page.WaitForSelectorOptions()
                    .setTimeout((double) timeoutMillis)
                    .setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));

            log.info("User logged in successfully");
            return true;

        } catch (PlaywrightException e) {
            // 超时或其他 Playwright 异常
            log.warn("Login wait failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Login wait interrupted: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 保存Cookies
     */
    public void saveCookies() {
        var cookies = pageWrapper.getPage().context().cookies();
        cookieManager.saveCookies(cookies);
        log.info("Cookies saved successfully ({} cookies)", cookies.size());
    }
}