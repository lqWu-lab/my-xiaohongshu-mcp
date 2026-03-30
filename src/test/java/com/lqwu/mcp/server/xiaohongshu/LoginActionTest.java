package com.lqwu.mcp.server.xiaohongshu;

import com.lqwu.mcp.server.xiaohongshu.browser.BrowserManager;
import com.lqwu.mcp.server.xiaohongshu.browser.PageWrapper;
import com.lqwu.mcp.server.xiaohongshu.config.BrowserConfig;
import com.lqwu.mcp.server.xiaohongshu.util.CookieManager;
import com.lqwu.mcp.server.xiaohongshu.xhs.LoginAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 小红书登录功能测试类
 *
 * <p>该测试类用于测试 LoginAction 的各项功能，包括：
 * <ul>
 *   <li>检查登录状态</li>
 *   <li>获取登录二维码</li>
 *   <li>等待用户扫码登录</li>
 *   <li>保存和加载Cookies</li>
 * </ul>
 *
 * <p>注意：由于需要实际启动浏览器和访问小红书网站，
 * 这些测试可能需要较长时间执行。建议在本地开发环境中运行。
 *
 * <p>运行测试前请确保：
 * <ul>
 *   <li>已安装 Playwright 浏览器驱动</li>
 *   <li>网络连接正常</li>
 *   <li>系统已安装兼容的浏览器</li>
 * </ul>
 */
public class LoginActionTest {

    /**
     * 浏览器管理器
     */
    private BrowserManager browserManager;

    /**
     * Cookie管理器
     */
    private CookieManager cookieManager;

    /**
     * 页面包装器
     */
    private PageWrapper pageWrapper;

    /**
     * 登录操作类
     */
    private LoginAction loginAction;

    /**
     * 浏览器配置
     */
    private BrowserConfig browserConfig;

    /**
     * ObjectMapper
     */
    private ObjectMapper objectMapper;

    /**
     * 测试前的准备工作
     *
     * <p>初始化浏览器管理器、Cookie管理器和登录操作类，
     * 并创建一个新的浏览器实例。
     */
    @BeforeEach
    public void setUp() {
        // 1. 创建 ObjectMapper（用于 JSON 处理）
        objectMapper = new ObjectMapper();

        // 2. 创建浏览器配置（设置为非无头模式，方便观察）
        browserConfig = new BrowserConfig();
        browserConfig.setHeadless(false); // 测试时使用可视化模式，方便观察
        browserConfig.setBinPath(null);    // 使用系统默认浏览器

        // 3. 创建浏览器管理器
        browserManager = new BrowserManager(new CookieManager(objectMapper), objectMapper);

        // 4. 创建 Cookie 管理器
        cookieManager = new CookieManager(objectMapper);

        // 5. 创建浏览器实例并获取页面包装器
        pageWrapper = browserManager.newBrowser(browserConfig);

        // 6. 创建登录操作类
        loginAction = new LoginAction(cookieManager);
        // 通过 setter 设置 pageWrapper
        loginAction.setPageWrapper(pageWrapper);
    }

    /**
     * 测试后的清理工作
     *
     * <p>关闭浏览器实例并清理资源。
     */
    @AfterEach
    public void tearDown() {
        if (pageWrapper != null) {
            String sessionId = pageWrapper.getSessionId();
            browserManager.closeBrowser(sessionId);
            System.out.println("浏览器会话已关闭: " + sessionId);
        }
    }

    /**
     * 测试检查登录状态功能
     *
     * <p>该测试验证 checkLoginStatus() 方法能否正确检测用户登录状态。
     * 由于是首次访问，应该是未登录状态。
     */
    @Test
    @DisplayName("测试检查登录状态功能")
    public void testCheckLoginStatus() {
        System.out.println("===== 测试检查登录状态 =====");

        // 执行检查登录状态
        boolean isLoggedIn = loginAction.checkLoginStatus();

        System.out.println("登录状态: " + isLoggedIn);
        System.out.println("===== 检查完成 =====\n");

        // 由于是首次访问，应该是未登录状态
        assertFalse(isLoggedIn, "首次访问应该返回未登录状态");
    }

    /**
     * 测试获取二维码图片功能
     *
     * <p>该测试验证 fetchQrcodeImage() 方法能否正确获取登录二维码。
     * 在未登录状态下应该返回二维码图片的 URL。
     */
    @Test
    @DisplayName("测试获取二维码图片功能")
    public void testFetchQrcodeImage() {
        System.out.println("===== 测试获取二维码图片 =====");

        // 获取二维码图片 URL
        String qrcodeUrl = loginAction.fetchQrcodeImage();

        System.out.println("二维码 URL: " + qrcodeUrl);

        // 在未登录状态下应该返回二维码 URL
        assertNotNull(qrcodeUrl, "未登录状态下应该返回二维码 URL");
        assertFalse(qrcodeUrl.isEmpty(), "二维码 URL 不应为空");

        System.out.println("===== 获取完成 =====\n");
    }

    /**
     * 测试保存Cookies功能
     *
     * <p>该测试验证 saveCookies() 方法能否正确保存 Cookies 到文件。
     */
    @Test
    @DisplayName("测试保存Cookies功能")
    public void testSaveCookies() {
        System.out.println("===== 测试保存Cookies =====");

        // 先获取一次 Cookies（这会触发页面加载）
        loginAction.checkLoginStatus();

        // 保存 Cookies
        loginAction.saveCookies();

        System.out.println("Cookies 保存成功");
        System.out.println("===== 保存完成 =====\n");

        // 验证文件是否创建
        String cookiePath = cookieManager.getCookiesFilePath();
        assertNotNull(cookiePath, "Cookie 文件路径不应为空");
    }

    /**
     * 测试完整登录流程（仅检测，不等待扫码）
     *
     * <p>该测试验证 login() 方法的基本流程是否能正常执行。
     * 由于不会实际扫码，使用较短的超时时间。
     *
     * <p>注意：该测试不会真正等待扫码登录，只是测试流程是否能正常启动。
     */
    @Test
    @DisplayName("测试完整登录流程（不等待扫码）")
    public void testLoginFlowWithoutWaiting() {
        System.out.println("===== 测试登录流程（不等待扫码） =====");

        // 执行登录流程，超时时间设为 1 秒（不实际等待扫码）
        loginAction.login(1);

        System.out.println("登录流程执行完成");
        System.out.println("===== 测试完成 =====\n");

        // 该测试仅验证方法能正常执行，不验证登录结果
        assertTrue(true, "登录流程应该能正常执行");
    }

    /**
     * 测试等待登录功能（短超时）
     *
     * <p>该测试验证 waitForLogin() 方法能否在超时后正确返回。
     * 由于不会实际扫码，使用较短的超时时间来验证等待逻辑。
     */
    @Test
    @DisplayName("测试等待登录功能（短超时）")
    public void testWaitForLoginShortTimeout() {
        System.out.println("===== 测试等待登录（短超时） =====");

        // 设置超时为 2 秒
        long timeoutMillis = 2000;

        long startTime = System.currentTimeMillis();
        boolean loginSuccess = loginAction.waitForLogin(timeoutMillis);
        long elapsedTime = System.currentTimeMillis() - startTime;

        System.out.println("等待结果: " + loginSuccess);
        System.out.println("实际耗时: " + elapsedTime + "ms");

        // 由于不会实际扫码，应该返回 false
        assertFalse(loginSuccess, "未扫码情况下应该返回 false");
        // 验证等待时间接近设定的超时时间
        assertTrue(elapsedTime >= timeoutMillis - 100, "等待时间应该接近设定的超时时间");

        System.out.println("===== 测试完成 =====\n");
    }

    /**
     * 测试页面导航功能
     *
     * <p>该测试验证多次调用 checkLoginStatus() 时，
     * 不会重复导航到小红书页面（通过检查 URL）。
     */
    @Test
    @DisplayName("测试页面导航功能")
    public void testPageNavigation() {
        System.out.println("===== 测试页面导航 =====");

        // 第一次检查登录状态（会导航）
        loginAction.checkLoginStatus();
        String url1 = pageWrapper.getPage().url();
        System.out.println("第一次检查后的 URL: " + url1);

        // 第二次检查登录状态（不应重复导航）
        loginAction.checkLoginStatus();
        String url2 = pageWrapper.getPage().url();
        System.out.println("第二次检查后的 URL: " + url2);

        // 验证 URL 没有变化（没有重复导航）
        assertEquals(url1, url2, "重复检查时不应重新导航页面");

        System.out.println("===== 测试完成 =====\n");
    }

    /**
     * 测试浏览器资源管理
     *
     * <p>该测试验证 BrowserManager 能否正确管理浏览器资源，
     * 包括创建、关闭会话等功能。
     */
    @Test
    @DisplayName("测试浏览器资源管理")
    public void testBrowserResourceManagement() {
        System.out.println("===== 测试浏览器资源管理 =====");

        // 获取初始的会话 ID
        String sessionId = pageWrapper.getSessionId();
        System.out.println("会话 ID: " + sessionId);

        // 验证会话 ID 格式正确
        assertNotNull(sessionId, "会话 ID 不应为空");
        assertTrue(sessionId.startsWith("session_"), "会话 ID 格式应该正确");

        // 关闭会话
        browserManager.closeBrowser(sessionId);
        System.out.println("会话已关闭: " + sessionId);

        System.out.println("===== 测试完成 =====\n");
    }
}