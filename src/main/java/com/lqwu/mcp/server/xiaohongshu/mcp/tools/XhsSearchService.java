package com.lqwu.mcp.server.xiaohongshu.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqwu.mcp.server.xiaohongshu.browser.BrowserManager;
import com.lqwu.mcp.server.xiaohongshu.browser.PageWrapper;
import com.lqwu.mcp.server.xiaohongshu.config.BrowserConfig;
import com.lqwu.mcp.server.xiaohongshu.mcp.config.XhsApiProperties;
import com.lqwu.mcp.server.xiaohongshu.xhs.SearchAction;
import com.lqwu.mcp.server.xiaohongshu.xhs.req.XhsSearchFilter;
import com.lqwu.mcp.server.xiaohongshu.xhs.resp.XhsSearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 小红书内容搜索MCP工具服务
 * 提供AI助手调用的工具函数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XhsSearchService {

    private final ObjectMapper objectMapper;
    private final XhsApiProperties xhsApiProperties;
    private final SearchAction searchAction;
    private final BrowserManager browserManager;

    /**
     * 搜索小红书笔记
     */
    @Tool(name = "searchXhsNotes", description = "在小红书平台搜索笔记内容，支持多种筛选条件。")
    public String searchXhsNotes(
            @ToolParam(description = "搜索关键词，如 \"美食\"、\"旅行攻略\"") String keyword,
            @ToolParam(description = "排序依据，可选：综合、最新、最多点赞、最多评论、最多收藏，默认为综合") String sortBy,
            @ToolParam(description = "笔记类型，可选：不限、视频、图文，默认为不限") String noteType,
            @ToolParam(description = "发布时间，可选：不限、一天内、一周内、半年内，默认为不限") String publishTime,
            @ToolParam(description = "搜索范围，可选：不限、已看过、未看过、已关注，默认为不限") String searchScope,
            @ToolParam(description = "位置距离，可选：不限、同城、附近，默认为不限") String location
    ) {
        log.info("开始在小红书上搜索笔记，关键词: {}", keyword);

        // 1. 校验参数
        if (keyword == null || keyword.trim().isEmpty()) {
            return createErrorResponse("请输入有效的搜索关键词");
        }

        try {
            // 2. 创建浏览器实例
            PageWrapper pageWrapper = ensureBrowserSession();
            if (pageWrapper == null) {
                return createErrorResponse("无法创建浏览器会话，请检查浏览器配置");
            }

            // 3. 设置搜索操作的 pageWrapper
            searchAction.setPageWrapper(pageWrapper);

            // 4. 创建查询请求对象
            XhsSearchFilter filter = new XhsSearchFilter();
            filter.setSortBy(sortBy != null ? sortBy : "综合");
            filter.setNoteType(noteType != null ? noteType : "不限");
            filter.setPublishTime(publishTime != null ? publishTime : "不限");
            filter.setSearchScope(searchScope != null ? searchScope : "不限");
            filter.setLocation(location != null ? location : "不限");

            // 5. 执行小红书搜索
            List<XhsSearchResp> searchResults = searchAction.search(keyword, List.of(filter));

            // 6. 处理搜索结果
            if (searchResults == null || searchResults.isEmpty()) {
                return createSuccessResponse("搜索完成，但未找到相关笔记", Map.of(
                        "keyword", keyword,
                        "total", 0,
                        "notes", List.of()
                ));
            }

            // 7. 格式化搜索结果
            List<Map<String, Object>> formattedNotes = searchResults.stream()
                    .map(this::formatNoteResult)
                    .toList();

            log.info("搜索完成，共找到 {} 条笔记", formattedNotes.size());

            return createSuccessResponse("搜索成功", Map.of(
                    "keyword", keyword,
                    "total", formattedNotes.size(),
                    "notes", formattedNotes
            ));

        } catch (Exception e) {
            log.error("❌ 搜索小红书笔记时出现异常: {}", e.getMessage(), e);
            return createErrorResponse("搜索小红书笔记时出现异常: " + e.getMessage());
        }
    }

    /**
     * 格式化单条笔记结果
     */
    private Map<String, Object> formatNoteResult(XhsSearchResp note) {
        Map<String, Object> result = new HashMap<>();

        // 基本信息
        result.put("id", note.getId());
        result.put("type", note.getType());
        result.put("modelType", note.getModelType());
        result.put("displayTitle", note.getDisplayTitle());

        // 笔记卡片信息
        if (note.getNoteCard() != null) {
            XhsSearchResp.NoteCard card = note.getNoteCard();

            // 用户信息
            if (card.getUser() != null) {
                result.put("author", Map.of(
                        "nickname", card.getUser().getNickName(),
                        "userId", card.getUser().getUserId(),
                        "avatar", card.getUser().getAvatar()
                ));
            }

            // 互动信息
            if (card.getInteractInfo() != null) {
                XhsSearchResp.InteractInfo interact = card.getInteractInfo();
                result.put("interactions", Map.of(
                        "likedCount", interact.getLikedCount(),
                        "collectedCount", interact.getCollectedCount(),
                        "commentCount", interact.getCommentCount(),
                        "sharedCount", interact.getSharedCount()
                ));
            }

            // 封面信息
            if (card.getCover() != null) {
                result.put("cover", card.getCover().getUrlDefault());
            }

            // 笔记类型
            result.put("noteType", card.getType());
        }

        return result;
    }

    /**
     * 确保浏览器会话可用
     */
    private PageWrapper ensureBrowserSession() {
        try {
            BrowserConfig browserConfig = new BrowserConfig();
            browserConfig.setHeadless(true); // MCP服务使用无头模式
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
            Map<String, Object> result = new java.util.HashMap<>();
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
