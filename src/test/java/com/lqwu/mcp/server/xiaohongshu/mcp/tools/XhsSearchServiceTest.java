package com.lqwu.mcp.server.xiaohongshu.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqwu.mcp.server.xiaohongshu.browser.BrowserManager;
import com.lqwu.mcp.server.xiaohongshu.browser.PageWrapper;
import com.lqwu.mcp.server.xiaohongshu.config.BrowserConfig;
import com.lqwu.mcp.server.xiaohongshu.mcp.config.XhsApiProperties;
import com.lqwu.mcp.server.xiaohongshu.util.CookieManager;
import com.lqwu.mcp.server.xiaohongshu.xhs.SearchAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class XhsSearchServiceTest {

    private XhsSearchService xhsSearchService;
    private ObjectMapper objectMapper;
    private XhsApiProperties xhsApiProperties;
    private SearchAction searchAction;
    private BrowserManager browserManager;
    private CookieManager cookieManager;
    private PageWrapper pageWrapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        cookieManager = new CookieManager(objectMapper);
        browserManager = new BrowserManager(cookieManager, objectMapper);
        searchAction = new SearchAction(objectMapper);

        xhsApiProperties = new XhsApiProperties();

        xhsSearchService = new XhsSearchService(
                objectMapper,
                xhsApiProperties,
                searchAction,
                browserManager
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
    @DisplayName("测试空关键词搜索返回错误响应")
    public void testSearchWithEmptyKeyword() {
        String response = xhsSearchService.searchXhsNotes("", null, null, null, null, null);
        System.out.println("响应：" +  response);
        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
        assertTrue(response.contains("请输入有效的搜索关键词"));
    }

    @Test
    @DisplayName("测试 null 关键词搜索返回错误响应")
    public void testSearchWithNullKeyword() {
        String response = xhsSearchService.searchXhsNotes(null, null, null, null, null, null);
        System.out.println("响应：" +  response);
        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
        assertTrue(response.contains("请输入有效的搜索关键词"));
    }

    @Test
    @DisplayName("测试空格关键词搜索返回错误响应")
    public void testSearchWithBlankKeyword() {
        String response = xhsSearchService.searchXhsNotes("   ", null, null, null, null, null);

        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
        assertTrue(response.contains("请输入有效的搜索关键词"));
    }

    @Test
    @DisplayName("测试 searchXhsNotes 返回 JSON 格式")
    public void testSearchReturnsJson() {
        String response = xhsSearchService.searchXhsNotes("美食", null, null, null, null, null);
        System.out.println("响应：" +  response);
        assertNotNull(response);
        assertTrue(response.startsWith("{"));
        assertTrue(response.endsWith("}"));
    }

    @Test
    @DisplayName("测试带筛选条件的搜索返回 JSON 格式")
    public void testSearchWithFiltersReturnsJson() {
        String response = xhsSearchService.searchXhsNotes(
                "美食",
                "最新",
                "视频",
                "一周内",
                "不限",
                "不限"
        );
        System.out.println("响应：" +  response);
        assertNotNull(response);
        assertTrue(response.startsWith("{"));
        assertTrue(response.endsWith("}"));
    }

    @Test
    @DisplayName("测试带排序条件的搜索")
    public void testSearchWithSortBy() {
        String response = xhsSearchService.searchXhsNotes(
                "旅行",
                "最多点赞",
                null,
                null,
                null,
                null
        );

        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
    }

    @Test
    @DisplayName("测试带笔记类型筛选的搜索")
    public void testSearchWithNoteType() {
        String response = xhsSearchService.searchXhsNotes(
                "编程",
                null,
                "视频",
                null,
                null,
                null
        );

        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
    }

    @Test
    @DisplayName("测试带发布时间筛选的搜索")
    public void testSearchWithPublishTime() {
        String response = xhsSearchService.searchXhsNotes(
                "摄影",
                null,
                null,
                "半年内",
                null,
                null
        );

        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
    }

    @Test
    @DisplayName("测试所有筛选条件组合")
    public void testSearchWithAllFilters() {
        String response = xhsSearchService.searchXhsNotes(
                "旅游攻略",
                "最多收藏",
                "图文",
                "一周内",
                "未看过",
                "同城"
        );

        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
    }

    @Test
    @DisplayName("测试默认值处理")
    public void testDefaultValues() {
        String response = xhsSearchService.searchXhsNotes(
                "测试",
                null,
                null,
                null,
                null,
                null
        );

        assertNotNull(response);
        assertTrue(response.contains("\"success\""));
    }
}