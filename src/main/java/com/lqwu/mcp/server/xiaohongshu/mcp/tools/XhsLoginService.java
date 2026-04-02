package com.lqwu.mcp.server.xiaohongshu.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqwu.mcp.server.xiaohongshu.browser.BrowserManager;
import com.lqwu.mcp.server.xiaohongshu.browser.PageWrapper;
import com.lqwu.mcp.server.xiaohongshu.config.BrowserConfig;
import com.lqwu.mcp.server.xiaohongshu.mcp.config.XhsApiProperties;
import com.lqwu.mcp.server.xiaohongshu.util.CookieManager;
import com.lqwu.mcp.server.xiaohongshu.xhs.LoginAction;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 小红书登录MCP工具服务
 * 提供AI助手调用的工具函数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XhsLoginService {

    private final ObjectMapper objectMapper;
    private final XhsApiProperties xhsApiProperties;
    private final LoginAction loginAction;
    private final BrowserManager browserManager;
    private final CookieManager cookieManager;

    /**
     * 服务初始化时检查并生成Cookie文件
     */
    @PostConstruct
    public void init() {
        log.info("初始化小红书登录服务...");

        // 1. 检查Cookie文件是否已存在
        if (cookieManager.isCookieFileExists()) {
            log.info("Cookie文件已存在，跳过从配置生成");
            return;
        }

        // 2. Cookie文件不存在，尝试从配置生成
        log.info("Cookie文件不存在，尝试从配置生成...");
        String configCookie = xhsApiProperties.getCookie();

        if (configCookie == null || configCookie.trim().isEmpty()) {
            log.warn("配置文件中未设置Cookie，请先配置 xhs.api.cookie");
            return;
        }

        if ("填写你的cookie".equals(configCookie.trim())) {
            log.warn("Cookie配置为默认值'填写你的cookie'，请先配置有效的Cookie");
            return;
        }

        // 3. 从配置生成Cookie文件
        boolean success = cookieManager.generateCookieFileFromConfig(configCookie);
        if (success) {
            log.info("✅ 已成功从配置生成Cookie文件");
        } else {
            log.error("❌ 从配置生成Cookie文件失败，请检查Cookie格式");
        }
    }

    /**
     * 检查登录状态
     */
    @Tool(name = "checkLoginStatus", description = "检查小红书登录状态，如果未登录且配置了Cookie会尝试自动初始化")
    public String checkLoginStatus() {
        log.info("检查小红书登录状态...");
        try {
            // 1. 先检查Cookie文件是否存在，不存在则尝试从配置生成
            if (!cookieManager.isCookieFileExists()) {
                log.info("Cookie文件不存在，尝试从配置生成...");
                String configCookie = xhsApiProperties.getCookie();
                if (xhsApiProperties.isValidCookie()) {
                    boolean generated = cookieManager.generateCookieFileFromConfig(configCookie);
                    if (!generated) {
                        return createErrorResponse("Cookie文件不存在且从配置生成失败，请检查配置或手动登录");
                    }
                } else {
                    return createErrorResponse("Cookie未配置或无效，请在配置文件中设置有效的小红书 Cookie");
                }
            }

            // 2. 创建浏览器会话并检查登录状态
            PageWrapper pageWrapper = ensureBrowserSession();
            if (pageWrapper == null) {
                return createErrorResponse("无法创建浏览器会话");
            }

            loginAction.setPageWrapper(pageWrapper);
            boolean loginStatus = loginAction.checkLoginStatus();

            if (loginStatus) {
                return createSuccessResponse("✅ 已登录", Map.of("loggedIn", true));
            } else {
                return createSuccessResponse("❌ 未登录，请使用 getLoginQRCode 获取二维码登录", Map.of("loggedIn", false));
            }
        } catch (Exception e) {
            log.error("检查登录状态时发生错误", e);
            return createErrorResponse("检查登录状态时发生错误: " + e.getMessage());
        }
    }

    /**
     * 从配置重新生成Cookie文件
     */
    @Tool(name = "generateCookieFromConfig", description = "从application.yml配置的Cookie字符串重新生成Cookie文件")
    public String generateCookieFromConfig() {
        log.info("从配置生成Cookie文件...");

        String configCookie = xhsApiProperties.getCookie();

        if (!xhsApiProperties.isValidCookie()) {
            return createErrorResponse("配置文件中未设置有效的Cookie，请先配置 xhs.api.cookie");
        }

        boolean success = cookieManager.generateCookieFileFromConfig(configCookie);

        if (success) {
            return createSuccessResponse("✅ 已成功从配置生成Cookie文件", null);
        } else {
            return createErrorResponse("从配置生成Cookie文件失败，请检查Cookie格式");
        }
    }

    /**
     * 获取登录二维码
     * 返回二维码后会在后台等待扫码，扫码成功后自动保存Cookie
     */
    @Tool(name = "getLoginQRCode", description = "获取小红书登录的二维码，扫码登录后会自动保存Cookie")
    public String getLoginQRCode() {
        log.info("获取小红书登录二维码...");
        try {
            PageWrapper pageWrapper = ensureBrowserSession();
            if (pageWrapper == null) {
                return createErrorResponse("无法创建浏览器会话");
            }

            loginAction.setPageWrapper(pageWrapper);
            String qrcodeImage = loginAction.fetchQrcodeImage();

            if (qrcodeImage == null) {
                boolean isLoggedIn = loginAction.checkLoginStatus();
                if (isLoggedIn) {
                    return createSuccessResponse("✅ 已经登录，无需扫码", Map.of("alreadyLoggedIn", true));
                }
                return createErrorResponse("获取二维码失败，请重试");
            }

            log.info("二维码已生成，启动后台扫码等待...");

            new Thread(() -> {
                try {
                    log.info("等待用户扫码登录...");
                    boolean loginSuccess = loginAction.waitForLogin(300000);
                    if (loginSuccess) {
                        log.info("扫码成功，保存Cookie...");
                        loginAction.saveCookies();
                        log.info("✅ Cookie已保存");
                    } else {
                        log.warn("扫码登录超时");
                    }
                } catch (Exception e) {
                    log.error("等待扫码登录异常: {}", e.getMessage());
                }
            }).start();

            return createSuccessResponse("二维码已生成，请在手机上扫码登录（5分钟内有效）", Map.of(
                    "qrcodeImage", qrcodeImage,
                    "tip", "扫码成功后会显示提示，届时可再次调用 checkLoginStatus 验证"
            ));
        } catch (Exception e) {
            log.error("❌ 获取登录二维码时发生错误:{}", e.getMessage(), e);
            return createErrorResponse("获取登录二维码时发生错误: " + e.getMessage());
        }
    }

    /**
     * 删除 Cookies（重置登录）
     */
    @Tool(name = "loginOut", description = "删除 Cookies，重置登录状态")
    public String loginOut() {
        log.info("删除 Cookies，重置登录...");
        try {
            cookieManager.removeCookies();
            log.info("✅ Cookies 已删除");
            return createSuccessResponse("✅ 已退出登录，Cookies 已删除", null);
        } catch (Exception e) {
            log.error("❌ 删除 Cookies 时发生错误:{}", e.getMessage(), e);
            return createErrorResponse("删除 Cookies 时发生错误: " + e.getMessage());
        }
    }

    /**
     * 确保浏览器会话可用
     */
    private PageWrapper ensureBrowserSession() {
        try {
            BrowserConfig browserConfig = new BrowserConfig();
            browserConfig.setHeadless(false); // MCP服务使用无头模式
            browserConfig.setBinPath(null);

            return browserManager.newBrowser(browserConfig);
        } catch (Exception e) {
            log.error("创建浏览器会话失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 创建成功响应
     */
    private String createSuccessResponse(String message, Map<String, Object> data) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", message);
            if (data != null) {
                result.put("data", data);
            }
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"success\":true,\"message\":\"" + message + "\"}";
        }
    }

    /**
     * 创建错误响应
     */
    private String createErrorResponse(String message) {
        try {
            Map<String, Object> errorResult = Map.of(
                    "success", false,
                    "message", message
            );
            return objectMapper.writeValueAsString(errorResult);
        } catch (Exception e) {
            return "{\"success\":false,\"message\":\"" + message + "\"}";
        }
    }
}
