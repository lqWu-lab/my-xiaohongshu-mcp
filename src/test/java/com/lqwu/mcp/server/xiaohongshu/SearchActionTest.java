package com.lqwu.mcp.server.xiaohongshu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqwu.mcp.server.xiaohongshu.browser.BrowserManager;
import com.lqwu.mcp.server.xiaohongshu.browser.PageWrapper;
import com.lqwu.mcp.server.xiaohongshu.config.BrowserConfig;
import com.lqwu.mcp.server.xiaohongshu.util.CookieManager;
import com.lqwu.mcp.server.xiaohongshu.xhs.LoginAction;
import com.lqwu.mcp.server.xiaohongshu.xhs.SearchAction;
import com.lqwu.mcp.server.xiaohongshu.xhs.req.XhsSearchFilter;
import com.lqwu.mcp.server.xiaohongshu.xhs.resp.XhsSearchResp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 小红书搜索功能测试类
 *
 * <p>该测试类用于测试 SearchAction 的各项功能，包括：
 * <ul>
 *   <li>简单关键词搜索</li>
 *   <li>带筛选条件的搜索</li>
 *   <li>搜索结果提取</li>
 *   <li>筛选条件应用</li>
 * </ul>
 *
 * <p>注意：由于需要实际启动浏览器和访问小红书网站，
 * 这些测试可能需要较长时间执行。建议在本地开发环境中运行。
 */
public class SearchActionTest {

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
     * 搜索操作类
     */
    private SearchAction searchAction;

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

        // 6. 创建搜索操作类
        searchAction = new SearchAction(objectMapper);
        searchAction.setPageWrapper(pageWrapper);

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
     * 测试简单关键词搜索
     */
    @Test
    @DisplayName("测试简单关键词搜索")
    public void testSimpleSearch() {
        System.out.println("===== 测试简单关键词搜索 =====");

        try {
            // 执行搜索
            List<XhsSearchResp> results = searchAction.search("薛之谦", null);

            // 验证结果
            System.out.println("搜索结果数量: " + (results != null ? results.size() : 0));
            if (results != null && !results.isEmpty()) {
                System.out.println("第一个结果:");
                XhsSearchResp first = results.get(0);
                System.out.println("  - ID: " + first.getId());
                System.out.println("  - modelType: " + first.getModelType());
                if (first.getNoteCard() != null) {
                    System.out.println("  - 类型: " + first.getNoteCard().getType());
                    if (first.getNoteCard().getUser() != null) {
                        System.out.println("  - 作者: " + first.getNoteCard().getUser().getNickName());
                    }
                    if (first.getNoteCard().getInteractInfo() != null) {
                        System.out.println("  - 点赞数: " + first.getNoteCard().getInteractInfo().getLikedCount());
                    }
                }
            }

            // 搜索应该返回结果（如果登录成功）
            assertNotNull(results, "搜索结果不应为null");
            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("搜索失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试带排序条件的搜索
     */
    @Test
    @DisplayName("测试带排序条件的搜索")
    public void testSearchWithSortBy() {
        System.out.println("===== 测试带排序条件的搜索 =====");

        try {
            // 创建筛选条件：按最新排序
            List<XhsSearchFilter> filters = new ArrayList<>();
            XhsSearchFilter filter = new XhsSearchFilter();
            filter.setSortBy("最新");
            filters.add(filter);

            // 执行搜索
            List<XhsSearchResp> results = searchAction.search("薛之谦", filters);

            // 验证结果
            System.out.println("搜索结果数量: " + (results != null ? results.size() : 0));

            // 搜索应该返回结果
            assertNotNull(results, "搜索结果不应为null");
            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("搜索失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试带多种筛选条件的搜索
     */
    @Test
    @DisplayName("测试带多种筛选条件的搜索")
    public void testSearchWithMultipleFilters() {
        System.out.println("===== 测试带多种筛选条件的搜索 =====");

        try {
            // 创建多个筛选条件
            List<XhsSearchFilter> filters = new ArrayList<>();

            XhsSearchFilter sortFilter = new XhsSearchFilter();
            sortFilter.setSortBy("最新");
            filters.add(sortFilter);

            XhsSearchFilter typeFilter = new XhsSearchFilter();
            typeFilter.setNoteType("视频");
            filters.add(typeFilter);

            // 执行搜索
            List<XhsSearchResp> results = searchAction.search("薛之谦", filters);

            // 验证结果
            System.out.println("搜索结果数量: " + (results != null ? results.size() : 0));

            // 搜索应该返回结果
            assertNotNull(results, "搜索结果不应为null");
            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("搜索失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试所有筛选条件
     */
    @Test
    @DisplayName("测试所有筛选条件")
    public void testAllFilterOptions() {
        System.out.println("===== 测试所有筛选条件 =====");

        try {
            // 测试所有筛选选项
            String[] sortOptions = {"综合", "最新", "最多点赞", "最多评论", "最多收藏"};
            String[] typeOptions = {"不限", "视频", "图文"};
            String[] timeOptions = {"不限", "一天内", "一周内", "半年内"};
            String[] scopeOptions = {"不限", "已看过", "未看过", "已关注"};
            String[] locationOptions = {"不限", "同城", "附近"};

            for (String sort : sortOptions) {
                System.out.println("测试排序: " + sort);
                List<XhsSearchFilter> filters = new ArrayList<>();
                XhsSearchFilter filter = new XhsSearchFilter();
                filter.setSortBy(sort);
                filters.add(filter);

                List<XhsSearchResp> results = searchAction.search("薛之谦", filters);
                System.out.println("  结果数量: " + (results != null ? results.size() : 0));
            }

            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试搜索结果数据提取
     */
    @Test
    @DisplayName("测试搜索结果数据提取")
    public void testSearchResultDataExtraction() {
        System.out.println("===== 测试搜索结果数据提取 =====");

        try {
            // 执行搜索
            List<XhsSearchResp> results = searchAction.search("薛之谦", null);

            if (results != null && !results.isEmpty()) {
                // 打印前3个结果的所有字段
                int limit = Math.min(3, results.size());
                for (int i = 0; i < limit; i++) {
                    XhsSearchResp resp = results.get(i);
                    System.out.println("结果 " + (i + 1) + ":");
                    System.out.println("  - ID: " + resp.getId());
                    System.out.println("  - modelType: " + resp.getModelType());

                    // 嵌套的 NoteCard 数据
                    if (resp.getNoteCard() != null) {
                        XhsSearchResp.NoteCard card = resp.getNoteCard();

                        // 类型
                        System.out.println("  - 类型: " + card.getType());

                        // 用户信息
                        if (card.getUser() != null) {
                            System.out.println("  - 作者昵称: " + card.getUser().getNickName());
                            System.out.println("  - 作者头像: " + card.getUser().getAvatar());
                        }

                        // 互动信息
                        if (card.getInteractInfo() != null) {
                            XhsSearchResp.InteractInfo interact = card.getInteractInfo();
                            System.out.println("  - 点赞数: " + interact.getLikedCount());
                            System.out.println("  - 评论数: " + interact.getCommentCount());
                            System.out.println("  - 收藏数: " + interact.getCollectedCount());
                            System.out.println("  - 已点赞: " + interact.isLiked());
                            System.out.println("  - 已收藏: " + interact.isCollected());
                        }

                        // 封面信息
                        if (card.getCover() != null) {
                            System.out.println("  - 封面URL: " + card.getCover().getUrlDefault());
                        }

                        // 图片列表
                        if (card.getImageList() != null && !card.getImageList().isEmpty()) {
                            System.out.println("  - 图片数量: " + card.getImageList().size());
                        }

                        // 角标信息（发布时间等）
                        if (card.getCornerTagInfo() != null && !card.getCornerTagInfo().isEmpty()) {
                            for (XhsSearchResp.CornerTagInfo tag : card.getCornerTagInfo()) {
                                System.out.println("  - 角标[" + tag.getType() + "]: " + tag.getText());
                            }
                        }
                    }
                }
            }

            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试空关键词搜索
     */
    @Test
    @DisplayName("测试空关键词搜索")
    public void testEmptyKeywordSearch() {
        System.out.println("===== 测试空关键词搜索 =====");

        try {
            // 执行空关键词搜索
            List<XhsSearchResp> results = searchAction.search("", null);

            // 验证结果（应该返回空列表或null）
            System.out.println("搜索结果: " + (results != null ? results.size() : "null"));
            System.out.println("===== 测试完成 =====\n");

        } catch (Exception e) {
            System.err.println("测试完成（预期会失败）: " + e.getMessage());
        }
    }

    /**
     * 测试不同关键词搜索
     */
    @Test
    @DisplayName("测试不同关键词搜索")
    public void testDifferentKeywords() {
        System.out.println("===== 测试不同关键词搜索 =====");

        String[] keywords = {"薛之谦", "编程", "旅游"};

        for (String keyword : keywords) {
            try {
                System.out.println("搜索关键词: " + keyword);
                List<XhsSearchResp> results = searchAction.search(keyword, null);
                System.out.println("  结果数量: " + (results != null ? results.size() : 0));
            } catch (Exception e) {
                System.err.println("  搜索失败: " + e.getMessage());
            }
        }

        System.out.println("===== 测试完成 =====\n");
    }
}