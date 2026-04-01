package com.lqwu.mcp.server.xiaohongshu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqwu.mcp.server.xiaohongshu.browser.BrowserManager;
import com.lqwu.mcp.server.xiaohongshu.browser.PageWrapper;
import com.lqwu.mcp.server.xiaohongshu.config.BrowserConfig;
import com.lqwu.mcp.server.xiaohongshu.util.CookieManager;
import com.lqwu.mcp.server.xiaohongshu.xhs.LoginAction;
import com.lqwu.mcp.server.xiaohongshu.xhs.PublishVideoAction;
import com.lqwu.mcp.server.xiaohongshu.xhs.req.XhsPublishReq;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;

/**
 * 小红书发布视频功能测试类
 *
 * <p>该测试类用于测试 PublishVideoAction 的各项功能。
 * 注意：发布功能需要先登录成功才能测试。
 */
public class PublishVideoActionTest {

    private BrowserManager browserManager;
    private CookieManager cookieManager;
    private PageWrapper pageWrapper;
    private PublishVideoAction publishVideoAction;
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
        publishVideoAction = new PublishVideoAction();
        publishVideoAction.setPageWrapper(pageWrapper);

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
     * 测试发布视频基本流程
     */
    @Test
    @DisplayName("测试发布视频基本流程")
    public void testPublishVideoBasicFlow() {
        System.out.println("===== 测试发布视频基本流程 =====");

        try {
            // 创建发布请求
            XhsPublishReq req = createBasicPublishReq();

            // 执行发布
            publishVideoAction.publish(req);

            System.out.println("视频发布流程执行完成");
            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("视频发布失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试发布视频带标签
     */
    @Test
    @DisplayName("测试发布视频带标签")
    public void testPublishVideoWithTags() {
        System.out.println("===== 测试发布视频带标签 =====");

        try {
            XhsPublishReq req = createBasicPublishReq();
            // 设置标签
            req.setTags(Arrays.asList(
                "#视频",
                "#生活记录",
                "#Vlog",
                "#日常"
            ));

            publishVideoAction.publish(req);

            System.out.println("带标签视频发布测试完成");
            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试视频可见性设置
     */
    @Test
    @DisplayName("测试视频可见性设置")
    public void testVideoVisibilitySetting() {
        System.out.println("===== 测试视频可见性设置 =====");

        try {
            // 测试仅自己可见
            XhsPublishReq req1 = createBasicPublishReq();
            req1.setVisibility("仅自己可见");
            publishVideoAction.publish(req1);
            System.out.println("仅自己可见视频测试完成");

            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试视频定时发布
     */
    @Test
    @DisplayName("测试视频定时发布")
    public void testVideoSchedulePublish() {
        System.out.println("===== 测试视频定时发布 =====");

        try {
            XhsPublishReq req = createBasicPublishReq();
            // 设置为1小时后的定时发布
            req.setScheduleTime(java.time.LocalDateTime.now().plusHours(1));

            publishVideoAction.publish(req);

            System.out.println("视频定时发布测试完成");
            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试视频商品绑定
     */
    @Test
    @DisplayName("测试视频商品绑定")
    public void testVideoBindProducts() {
        System.out.println("===== 测试视频商品绑定 =====");

        try {
            XhsPublishReq req = createBasicPublishReq();
            // 设置商品
            req.setProducts(Arrays.asList("商品关键词1", "商品关键词2"));

            publishVideoAction.publish(req);

            System.out.println("视频商品绑定测试完成");
            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试视频路径为空的情况
     */
    @Test
    @DisplayName("测试视频路径为空的情况")
    public void testEmptyVideoPath() {
        System.out.println("===== 测试视频路径为空的情况 =====");

        try {
            XhsPublishReq req = createBasicPublishReq();
            req.setImagePaths(null);

            publishVideoAction.publish(req);

            System.err.println("应该抛出异常，但测试通过了");
        } catch (IllegalArgumentException e) {
            System.out.println("成功捕获预期异常: " + e.getMessage());
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
        req.setTitle("测试视频标题");
        req.setContent("这是测试视频内容，用于验证视频发布功能是否正常工作。");
        req.setImagePaths(Arrays.asList(System.getProperty("user.dir") + "/src/main/resources/video/1.mp4"));
        req.setIsOriginal(false);
        return req;
    }
}
