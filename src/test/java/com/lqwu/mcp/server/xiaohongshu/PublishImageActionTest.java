package com.lqwu.mcp.server.xiaohongshu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqwu.mcp.server.xiaohongshu.browser.BrowserManager;
import com.lqwu.mcp.server.xiaohongshu.browser.PageWrapper;
import com.lqwu.mcp.server.xiaohongshu.config.BrowserConfig;
import com.lqwu.mcp.server.xiaohongshu.util.CookieManager;
import com.lqwu.mcp.server.xiaohongshu.xhs.LoginAction;
import com.lqwu.mcp.server.xiaohongshu.xhs.PublishImageAction;
import com.lqwu.mcp.server.xiaohongshu.xhs.req.XhsPublishReq;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;

/**
 * 小红书发布功能测试类
 *
 * <p>该测试类用于测试 PublishImageAction 的各项功能。
 * 注意：发布功能需要先登录成功才能测试。
 */
public class PublishImageActionTest {

    private BrowserManager browserManager;
    private CookieManager cookieManager;
    private PageWrapper pageWrapper;
    private PublishImageAction publishImageAction;
    private LoginAction loginAction;
    private BrowserConfig browserConfig;
    private ObjectMapper objectMapper;

    /**
     * 测试前的准备工作
     */
    @BeforeEach
    public void setUp() {
        // 1. 创建 ObjectMapper
        objectMapper = new ObjectMapper();

        // 2. 创建浏览器配置
        browserConfig = new BrowserConfig();
        browserConfig.setHeadless(false); // 测试时使用可视化模式
        browserConfig.setBinPath(null);

        // 3. 创建浏览器管理器
        cookieManager = new CookieManager(objectMapper);
        browserManager = new BrowserManager(cookieManager, objectMapper);

        // 4. 创建浏览器实例
        pageWrapper = browserManager.newBrowser(browserConfig);

        // 5. 创建登录操作类并登录
        loginAction = new LoginAction(cookieManager);
        loginAction.setPageWrapper(pageWrapper);

        // 6. 创建发布操作类
        publishImageAction = new PublishImageAction();
        publishImageAction.setPageWrapper(pageWrapper);

        System.out.println("===== 测试环境准备完成 =====");
    }

    /**
     * 测试后的清理工作
     */
    @AfterEach
    public void tearDown() {
        if (pageWrapper != null) {
            String sessionId = pageWrapper.getSessionId();
            browserManager.closeBrowser(sessionId);
            System.out.println("===== 浏览器会话已关闭: " + sessionId + " =====");
        }
    }

    /**
     * 测试发布内容基本流程
     */
    @Test
    @DisplayName("测试发布内容基本流程")
    public void testPublishBasicFlow() {
        System.out.println("===== 测试发布内容基本流程 =====");

        try {
            // 创建发布请求
            XhsPublishReq req = createBasicPublishReq();

            // 执行发布
            publishImageAction.publish(req);

            System.out.println("发布流程执行完成");
            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("发布失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试上传多张图片
     */
    @Test
    @DisplayName("测试上传多张图片")
    public void testUploadMultipleImages() {
        System.out.println("===== 测试上传多张图片 =====");

        try {
            XhsPublishReq req = createBasicPublishReq();
            // 确保有多张图片
            req.setImagePaths(Arrays.asList(
                    System.getProperty("user.dir") + "/src/main/resources/image/1.png",
                    System.getProperty("user.dir") + "/src/main/resources/image/2.png"
            ));

            publishImageAction.publish(req);

            System.out.println("多图上传测试完成");
            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试带标签的发布
     */
    @Test
    @DisplayName("测试带标签的发布")
    public void testPublishWithTags() {
        System.out.println("===== 测试带标签的发布 =====");

        try {
            XhsPublishReq req = createBasicPublishReq();
            // 设置标签
            req.setTags(Arrays.asList(
                "#薛之谦",
                "#音乐",
                "#演唱会",
                "#流行音乐"
            ));

            publishImageAction.publish(req);

            System.out.println("带标签发布测试完成");
            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试原创声明 todo 有点问题，不知道为什么有时候没有这个开关
     */
    @Test
    @DisplayName("测试原创声明")
    public void testOriginalDeclaration() {
        System.out.println("===== 测试原创声明 =====");

        try {
            XhsPublishReq req = createBasicPublishReq();
            req.setIsOriginal(true); // 开启原创声明

            publishImageAction.publish(req);

            System.out.println("原创声明测试完成");
            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试可见性设置
     */
    @Test
    @DisplayName("测试可见性设置")
    public void testVisibilitySetting() {
        System.out.println("===== 测试可见性设置 =====");

        try {
            // 测试仅自己可见
            XhsPublishReq req1 = createBasicPublishReq();
            req1.setVisibility("仅自己可见");
            publishImageAction.publish(req1);
            System.out.println("仅自己可见测试完成");

            // 测试互关好友可见
            XhsPublishReq req2 = createBasicPublishReq();
            req2.setVisibility("仅互关好友可见");
            publishImageAction.publish(req2);
            System.out.println("仅互关好友可见测试完成");

            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试定时发布
     */
    @Test
    @DisplayName("测试定时发布")
    public void testSchedulePublish() {
        System.out.println("===== 测试定时发布 =====");

        try {
            XhsPublishReq req = createBasicPublishReq();
            // 设置为1小时后的定时发布
            req.setScheduleTime(java.time.LocalDateTime.now().plusHours(1));

            publishImageAction.publish(req);

            System.out.println("定时发布测试完成");
            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试商品绑定 todo 好像没有这个功能
     */
    @Test
    @DisplayName("测试商品绑定")
    public void testBindProducts() {
        System.out.println("===== 测试商品绑定 =====");

        try {
            XhsPublishReq req = createBasicPublishReq();
            // 设置商品
            req.setProducts(Arrays.asList("商品关键词1", "商品关键词2"));

            publishImageAction.publish(req);

            System.out.println("商品绑定测试完成");
            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建基本的发布请求（用于测试）
     */
    private XhsPublishReq createBasicPublishReq() {
        XhsPublishReq req = new XhsPublishReq();
        req.setTitle("测试标题");
        req.setContent("这是测试内容，用于验证发布功能是否正常工作。");
        req.setImagePaths(Arrays.asList(System.getProperty("user.dir") + "/src/main/resources/image/1.png"));
        req.setIsOriginal(false);
        return req;
    }
}