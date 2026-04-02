package com.lqwu.mcp.server.xiaohongshu.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqwu.mcp.server.xiaohongshu.browser.BrowserManager;
import com.lqwu.mcp.server.xiaohongshu.browser.PageWrapper;
import com.lqwu.mcp.server.xiaohongshu.config.BrowserConfig;
import com.lqwu.mcp.server.xiaohongshu.mcp.config.XhsApiProperties;
import com.lqwu.mcp.server.xiaohongshu.util.CookieManager;
import com.lqwu.mcp.server.xiaohongshu.xhs.LoginAction;
import com.lqwu.mcp.server.xiaohongshu.xhs.PublishImageAction;
import com.lqwu.mcp.server.xiaohongshu.xhs.PublishVideoAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class XhsPublishServiceTest {

    private XhsPublishService xhsPublishService;
    private ObjectMapper objectMapper;
    private XhsApiProperties xhsApiProperties;
    private PublishImageAction publishImageAction;
    private PublishVideoAction publishVideoAction;
    private BrowserManager browserManager;
    private CookieManager cookieManager;
    private PageWrapper pageWrapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        cookieManager = new CookieManager(objectMapper);
        browserManager = new BrowserManager(cookieManager, objectMapper);
        publishImageAction = new PublishImageAction();
        publishVideoAction = new PublishVideoAction();

        xhsApiProperties = new XhsApiProperties();
        xhsApiProperties.setCookie("test_cookie");

        xhsPublishService = new XhsPublishService(
                publishImageAction,
                publishVideoAction,
                xhsApiProperties,
                objectMapper,
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
    @DisplayName("测试发布图文笔记参数验证 - 空标题")
    public void testPublishImageNoteWithEmptyTitle() {
        String response = xhsPublishService.publishImageNote(
                "",
                "内容",
                List.of("C:/test.jpg"),
                null,
                null,
                false,
                "公开可见",
                null
        );

        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
        assertTrue(response.contains("标题不能为空"));
    }

    @Test
    @DisplayName("测试发布图文笔记参数验证 - 空内容")
    public void testPublishImageNoteWithEmptyContent() {
        String response = xhsPublishService.publishImageNote(
                "标题",
                "",
                List.of("C:/test.jpg"),
                null,
                null,
                false,
                "公开可见",
                null
        );

        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
        assertTrue(response.contains("内容不能为空"));
    }

    @Test
    @DisplayName("测试发布图文笔记参数验证 - 空图片列表")
    public void testPublishImageNoteWithEmptyImagePaths() {
        String response = xhsPublishService.publishImageNote(
                "标题",
                "内容",
                List.of(),
                null,
                null,
                false,
                "公开可见",
                null
        );

        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
        assertTrue(response.contains("图片列表不能为空"));
    }

    @Test
    @DisplayName("测试发布图文笔记参数验证 - 空图片路径")
    public void testPublishImageNoteWithNullImagePaths() {
        String response = xhsPublishService.publishImageNote(
                "标题",
                "内容",
                null,
                null,
                null,
                false,
                "公开可见",
                null
        );

        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
        assertTrue(response.contains("图片列表不能为空"));
    }

    @Test
    @DisplayName("测试发布图文笔记返回 JSON 格式")
    public void testPublishImageNoteReturnsJson() {
        String response = xhsPublishService.publishImageNote(
                "测试标题",
                "测试内容",
                List.of("C:/test.jpg"),
                null,
                null,
                false,
                "公开可见",
                null
        );

        assertNotNull(response);
        assertTrue(response.startsWith("{"));
        assertTrue(response.endsWith("}"));
    }

    @Test
    @DisplayName("测试发布视频笔记参数验证 - 空标题")
    public void testPublishVideoNoteWithEmptyTitle() {
        String response = xhsPublishService.publishVideoNote(
                "",
                "视频描述",
                "C:/test.mp4",
                null,
                null,
                "公开可见",
                null
        );

        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
        assertTrue(response.contains("标题不能为空"));
    }

    @Test
    @DisplayName("测试发布视频笔记参数验证 - 空描述")
    public void testPublishVideoNoteWithEmptyContent() {
        String response = xhsPublishService.publishVideoNote(
                "视频标题",
                "",
                "C:/test.mp4",
                null,
                null,
                "公开可见",
                null
        );

        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
        assertTrue(response.contains("描述不能为空"));
    }

    @Test
    @DisplayName("测试发布视频笔记参数验证 - 空视频路径")
    public void testPublishVideoNoteWithEmptyVideoPath() {
        String response = xhsPublishService.publishVideoNote(
                "视频标题",
                "视频描述",
                "",
                null,
                null,
                "公开可见",
                null
        );

        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
        assertTrue(response.contains("路径不能为空"));
    }

    @Test
    @DisplayName("测试发布视频笔记返回 JSON 格式")
    public void testPublishVideoNoteReturnsJson() {
        String response = xhsPublishService.publishVideoNote(
                "测试标题",
                "测试描述",
                "C:/test.mp4",
                null,
                null,
                "公开可见",
                null
        );

        assertNotNull(response);
        assertTrue(response.startsWith("{"));
        assertTrue(response.endsWith("}"));
    }

    @Test
    @DisplayName("测试带话题标签发布图文笔记")
    public void testPublishImageNoteWithTags() {
        List<String> tags = List.of("#美食", "#旅行");
        String response = xhsPublishService.publishImageNote(
                "测试标题",
                "测试内容",
                List.of("C:/test.jpg"),
                tags,
                null,
                false,
                "公开可见",
                null
        );

        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
    }

    @Test
    @DisplayName("测试带商品列表发布图文笔记")
    public void testPublishImageNoteWithProducts() {
        List<String> products = List.of("商品1", "商品2");
        String response = xhsPublishService.publishImageNote(
                "测试标题",
                "测试内容",
                List.of("C:/test.jpg"),
                null,
                null,
                false,
                "公开可见",
                products
        );

        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
    }
}