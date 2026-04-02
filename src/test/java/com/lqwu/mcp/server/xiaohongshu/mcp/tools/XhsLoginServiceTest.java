package com.lqwu.mcp.server.xiaohongshu.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqwu.mcp.server.xiaohongshu.browser.BrowserManager;
import com.lqwu.mcp.server.xiaohongshu.browser.PageWrapper;
import com.lqwu.mcp.server.xiaohongshu.config.BrowserConfig;
import com.lqwu.mcp.server.xiaohongshu.mcp.config.XhsApiProperties;
import com.lqwu.mcp.server.xiaohongshu.util.CookieManager;
import com.lqwu.mcp.server.xiaohongshu.xhs.LoginAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class XhsLoginServiceTest {

    private XhsLoginService xhsLoginService;
    private ObjectMapper objectMapper;
    private XhsApiProperties xhsApiProperties;
    private LoginAction loginAction;
    private BrowserManager browserManager;
    private CookieManager cookieManager;
    private PageWrapper pageWrapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        cookieManager = new CookieManager(objectMapper);
        browserManager = new BrowserManager(cookieManager, objectMapper);
        loginAction = new LoginAction(cookieManager);

        xhsApiProperties = new XhsApiProperties();
        xhsApiProperties.setCookie("test_cookie");

        xhsLoginService = new XhsLoginService(
                objectMapper,
                xhsApiProperties,
                loginAction,
                browserManager,
                cookieManager
        );
    }

    @AfterEach
    public void tearDown() {
        if (pageWrapper != null) {
            try {
                browserManager.closeBrowser(pageWrapper.getSessionId());
            } catch (Exception e) {
                System.err.println("关闭浏览器失败: " + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("测试创建成功响应")
    public void testCreateSuccessResponse() throws Exception {
        java.util.Map<String, Object> data = java.util.Map.of("key", "value");
        String response = xhsLoginService.checkLoginStatus();

        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
    }

    @Test
    @DisplayName("测试 checkLoginStatus 返回 JSON 格式")
    public void testCheckLoginStatusReturnsJson() {
        String response = xhsLoginService.checkLoginStatus();
        System.out.println("响应：" + response);
        assertNotNull(response);
        assertTrue(response.startsWith("{"));
        assertTrue(response.endsWith("}"));
    }

    @Test
    @DisplayName("测试 loginOut 返回成功响应")
    public void testLoginOutReturnsSuccess() {
        String response = xhsLoginService.loginOut();
        System.out.println("响应：" + response);
        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
    }

    @Test
    @DisplayName("测试 generateCookieFromConfig 当配置无效时返回错误")
    public void testGenerateCookieFromConfigWithInvalidCookie() {
        xhsApiProperties.setCookie("填写你的cookie");

        String response = xhsLoginService.generateCookieFromConfig();

        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
    }

    @Test
    @DisplayName("测试 getLoginQRCode")
    public void testGetLoginQRCode() {
        String response = xhsLoginService.getLoginQRCode();
        System.out.println("响应：" + response);
        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
    }
}