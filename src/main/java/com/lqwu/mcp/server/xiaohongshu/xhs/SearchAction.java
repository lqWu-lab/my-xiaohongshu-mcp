package com.lqwu.mcp.server.xiaohongshu.xhs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqwu.mcp.server.xiaohongshu.browser.PageWrapper;
import com.lqwu.mcp.server.xiaohongshu.xhs.req.XhsSearchFilter;
import com.lqwu.mcp.server.xiaohongshu.xhs.resp.XhsSearchResp;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 小红书搜索功能
 *
 * @author lqwu
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchAction {

    private static final String SEARCH_URL_TEMPLATE = "https://www.xiaohongshu.com/search_result?keyword=%s&source=web_explore_feed";
    private static final String FILTER_BUTTON_SELECTOR = "div.filter";
    private static final String FILTER_PANEL_SELECTOR = "div.filter-panel";

    @Setter
    private PageWrapper pageWrapper;
    private final ObjectMapper objectMapper;

    /**
     * 执行搜索
     * @param keyword  搜索关键词
     * @param filters  筛选选项（可选）
     * @return 搜索结果列表
     */
    public List<XhsSearchResp> search(String keyword, List<XhsSearchFilter> filters) {
        Page page = pageWrapper.getPage();
        try {
            // 1. 构建搜索 URL
            String searchURL = buildSearchURL(keyword);
            log.info("导航到搜索页面: {}", searchURL);
            // 2. 导航到搜索页面
            page.navigate(searchURL);
            page.waitForLoadState(LoadState.LOAD);
            // 3. 等待页面稳定
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            // 4. 额外等待一下，让页面渲染
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 5. 等待 __INITIAL_STATE__ 加载完成
            waitForInitialState(page);
            // 6. 应用筛选条件（如果有）
            if (filters != null && !filters.isEmpty()) {
                applyFilters(page, filters);
                // 7. 筛选后需要再次等待数据加载完成
                waitForInitialState(page);
            }
            // 8. 提取搜索结果
            return extractFeeds(page);
        } catch (Exception e) {
            log.error("搜索失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 构建搜索 URL
     * @param keyword 搜索关键词
     */
    private String buildSearchURL(String keyword) {
        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            return String.format(SEARCH_URL_TEMPLATE, encodedKeyword);
        } catch (Exception e) {
            log.error("URL 编码失败: {}", e.getMessage());
            return String.format(SEARCH_URL_TEMPLATE, keyword);
        }
    }

    /**
     * 等待 __INITIAL_STATE__ 加载完成
     * @param page 页面
     */
    private void waitForInitialState(Page page) {
        int maxAttempts = 60;   // 最大尝试次数（增加到60次）
        int attempt = 0;        // 当前尝试次数

        // 1. 初始等待，让页面先加载
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 2. 循环尝试
        while (attempt < maxAttempts) {
            try {
                // 检查 __INITIAL_STATE__ 是否存在且 search.feeds 有数据
                String result = (String) page.evaluate("() => {" +
                    "if (window.__INITIAL_STATE__ && " +
                    "    window.__INITIAL_STATE__.search && " +
                    "    window.__INITIAL_STATE__.search.feeds) {" +
                    "    const feeds = window.__INITIAL_STATE__.search.feeds;" +
                    "    const feedsData = feeds.value !== undefined ? feeds.value : feeds._value;" +
                    "    if (feedsData && feedsData.length > 0) {" +
                    "        return 'ready';" +
                    "    }" +
                    "}" +
                    "return '';" +
                "}");

                if ("ready".equals(result)) {
                    log.info("__INITIAL_STATE__.search.feeds 加载完成，有数据");
                    return;
                }

                // 不满足条件，等待 1 秒
                Thread.sleep(1000);
                attempt++;
                log.debug("等待 search.feeds 加载，尝试次数: {}/{}", attempt, maxAttempts);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("等待被中断");
                return;
            } catch (Exception e) {
                attempt++;
                log.debug("等待 __INITIAL_STATE__，尝试次数: {}/{}", attempt, maxAttempts);
            }
        }
        log.warn("__INITIAL_STATE__.search.feeds 等待超时（{}秒），可能没有数据", maxAttempts / 1000);
    }

    /**
     * 应用筛选条件
     * @param page 页面对象
     * @param filters 筛选条件
     */
    private void applyFilters(Page page, List<XhsSearchFilter> filters) {
        try {
            // 1.悬停在筛选按钮上
            page.hover(FILTER_BUTTON_SELECTOR);
            Thread.sleep(500);
            // 2.等待筛选面板出现
            page.waitForSelector(FILTER_PANEL_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(5000));
            // 3. 应用筛选条件
            for (XhsSearchFilter filter : filters) {
                applyFilter(page, filter);
            }
        } catch (Exception e) {
            log.warn("应用筛选条件失败: {}", e.getMessage());
        }
    }

    /**
     * 应用单个筛选条件
     * @param filter 筛选条件
     */
    private void applyFilter(Page page, XhsSearchFilter filter) {
        try {
            // 排序依据: 综合、最新、最多点赞、最多评论、最多收藏
            if (filter.getSortBy() != null && !filter.getSortBy().isEmpty()) {
                clickFilterOption(1, getSortByIndex(filter.getSortBy()));
            }
            // 笔记类型: 不限、视频、图文
            if (filter.getNoteType() != null && !filter.getNoteType().isEmpty()) {
                clickFilterOption(2, getNoteTypeIndex(filter.getNoteType()));
            }
            // 发布时间: 不限、一天内、一周内、半年内
            if (filter.getPublishTime() != null && !filter.getPublishTime().isEmpty()) {
                clickFilterOption(3, getPublishTimeIndex(filter.getPublishTime()));
            }
            // 搜索范围: 不限、已看过、未看过、已关注
            if (filter.getSearchScope() != null && !filter.getSearchScope().isEmpty()) {
                clickFilterOption(4, getSearchScopeIndex(filter.getSearchScope()));
            }
            // 位置距离: 不限、同城、附近
            if (filter.getLocation() != null && !filter.getLocation().isEmpty()) {
                clickFilterOption(5, getLocationIndex(filter.getLocation()));
            }
        } catch (Exception e) {
            log.warn("应用筛选选项失败: {}", e.getMessage());
        }
    }

    /**
     * 点击筛选选项
     * @param filtersIndex 筛选组索引（从1开始）
     * @param tagsIndex    标签索引（从1开始）
     */
    private void clickFilterOption(int filtersIndex, int tagsIndex) {
        if (tagsIndex < 1) {
            return;
        }
        try {
            String selector = String.format(
                "%s div.filters:nth-child(%d) div.tags:nth-child(%d)",
                FILTER_PANEL_SELECTOR, filtersIndex, tagsIndex
            );
            log.debug("点击筛选选项: {}", selector);
            pageWrapper.getPage().click(selector);
            // 点击后等待页面更新
            Thread.sleep(1500);
        } catch (Exception e) {
            log.debug("点击筛选选项失败: filters={}, tags={}, error={}",
                filtersIndex, tagsIndex, e.getMessage());
        }
    }

    /**
     * 获取排序依据索引
     */
    private int getSortByIndex(String sortBy) {
        return switch (sortBy) {
            case "综合" -> 1;
            case "最新" -> 2;
            case "最多点赞" -> 3;
            case "最多评论" -> 4;
            case "最多收藏" -> 5;
            default -> -1;
        };
    }

    /**
     * 获取笔记类型索引
     */
    private int getNoteTypeIndex(String noteType) {
        return switch (noteType) {
            case "不限" -> 1;
            case "视频" -> 2;
            case "图文" -> 3;
            default -> -1;
        };
    }

    /**
     * 获取发布时间索引
     */
    private int getPublishTimeIndex(String publishTime) {
        return switch (publishTime) {
            case "不限" -> 1;
            case "一天内" -> 2;
            case "一周内" -> 3;
            case "半年内" -> 4;
            default -> -1;
        };
    }

    /**
     * 获取搜索范围索引
     */
    private int getSearchScopeIndex(String searchScope) {
        return switch (searchScope) {
            case "不限" -> 1;
            case "已看过" -> 2;
            case "未看过" -> 3;
            case "已关注" -> 4;
            default -> -1;
        };
    }

    /**
     * 获取位置距离索引
     */
    private int getLocationIndex(String location) {
        return switch (location) {
            case "不限" -> 1;
            case "同城" -> 2;
            case "附近" -> 3;
            default -> -1;
        };
    }

    /**
     * 从页面提取搜索结果
     * @param page 页面对象
     * @return 搜索结果列表
     */
    private List<XhsSearchResp> extractFeeds(Page page) {
        try {
            // 1. 获取 __INITIAL_STATE__ 调试信息
            String debugInfo = (String) page.evaluate("() => {" +
                "if (window.__INITIAL_STATE__) {" +
                "    if (window.__INITIAL_STATE__.search) {" +
                "        if (window.__INITIAL_STATE__.search.feeds) {" +
                "            const feeds = window.__INITIAL_STATE__.search.feeds;" +
                "            const feedsData = feeds.value !== undefined ? feeds.value : feeds._value;" +
                "            return 'feeds found, length: ' + (feedsData ? feedsData.length : 0);" +
                "        }" +
                "        return 'search exists but no feeds';" +
                "    }" +
                "    return 'no search in state';" +
                "}" +
                "return 'no __INITIAL_STATE__';" +
            "}");
            log.debug("__INITIAL_STATE__ 状态: {}", debugInfo);

            // 2. 获取搜索结果数据
            String feedsJson = (String) page.evaluate("() => {" +
                "if (window.__INITIAL_STATE__ && " +
                "    window.__INITIAL_STATE__.search && " +
                "    window.__INITIAL_STATE__.search.feeds) {" +
                "    const feeds = window.__INITIAL_STATE__.search.feeds;" +
                "    const feedsData = feeds.value !== undefined ? feeds.value : feeds._value;" +
                "    if (feedsData && feedsData.length > 0) {" +
                "        return JSON.stringify(feedsData);" +
                "    }" +
                "}" +
                "return '';" +
            "}");

            // 3. 判断是否有数据
            if (feedsJson == null || feedsJson.isEmpty() || "[]".equals(feedsJson)) {
                log.warn("未找到搜索结果数据，调试信息: {}", debugInfo);
                return new ArrayList<>();
            }

            log.info("成功获取搜索结果，长度: {}", feedsJson.length());

            // 4. 解析数据
            List<XhsSearchResp> result = objectMapper.readValue(feedsJson, new TypeReference<List<XhsSearchResp>>() {});
            log.info("解析完成，共 {} 条结果", result.size());
            return result;
        } catch (Exception e) {
            log.error("提取搜索结果失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}