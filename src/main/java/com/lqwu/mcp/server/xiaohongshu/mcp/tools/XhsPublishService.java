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
import com.lqwu.mcp.server.xiaohongshu.xhs.req.XhsPublishReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 小红书内容发布MCP工具服务
 * 提供AI助手调用的工具函数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XhsPublishService {

    private final PublishImageAction publishImageAction;
    private final PublishVideoAction publishVideoAction;
    private final XhsApiProperties xhsApiProperties;
    private final ObjectMapper objectMapper;
    private final BrowserManager browserManager;
    private final CookieManager cookieManager;

    /**
     * 发布图文笔记到小红书
     */
    @Tool(name = "publishImageNote", description = "发布图文笔记到小红书平台。需要提前配置好浏览器和Cookie。")
    public String publishImageNote(
            @ToolParam(description = "笔记标题，长度不超过 20 个字符", required = true) String title,
            @ToolParam(description = "笔记内容，长度不超过 5000 个字符", required = true) String content,
            @ToolParam(description = "本地图片文件路径列表，如 [\"C:/Photos/img1.jpg\", \"C:/Photos/img2.jpg\"]", required = true) List<String> imagePaths,
            @ToolParam(description = "话题标签列表，数量不超过10个，如 [\"#美食\", \"#旅行\"]") List<String> tags,
            @ToolParam(description = "定时发布时间，格式 yyyy-MM-dd HH:mm，如 2024-12-25 10:00，不定时则传 null") LocalDateTime scheduleTime,
            @ToolParam(description = "是否声明原创，默认为 false") boolean isOriginal,
            @ToolParam(description = "可见性：公开可见、仅自己可见、仅互关好友可见，默认为公开可见") String visibility,
            @ToolParam(description = "商品关键词列表，用于绑定商品") List<String> products) {
        try {
            log.info("开始发布图文笔记：{}", title);

            // 1. 验证必要参数
            if (title == null || title.trim().isEmpty()) {
                return createErrorResponse("文章标题不能为空");
            }
            if (content == null || content.trim().isEmpty()) {
                return createErrorResponse("文章内容不能为空");
            }
            if (imagePaths == null || imagePaths.isEmpty()) {
                return createErrorResponse("图片列表不能为空");
            }

            // 2. 创建浏览器实例（如果还没有）
            PageWrapper pageWrapper = ensureBrowserSession();
            if (pageWrapper == null) {
                return createErrorResponse("无法创建浏览器会话，请检查浏览器配置");
            }

            // 3. 设置发布操作类的 pageWrapper
            publishImageAction.setPageWrapper(pageWrapper);

            // 4. 创建请求对象
            XhsPublishReq req = new XhsPublishReq();
            req.setTitle(title);
            req.setContent(content);
            req.setTags(tags);
            req.setImagePaths(imagePaths);
            req.setScheduleTime(scheduleTime);
            req.setVisibility(visibility != null ? visibility : "公开可见");
            req.setProducts(products);
            req.setIsOriginal(isOriginal);

            // 5. 执行发布
            publishImageAction.publish(req);

            log.info("图文笔记发布成功：{}", title);
            return createSuccessResponse("图文笔记发布成功", Map.of("title", title));

        } catch (Exception e) {
            log.error("❌ 发布图文笔记时出现异常: {}", e.getMessage(), e);
            return createErrorResponse("发布图文笔记时出现异常: " + e.getMessage());
        }
    }

    /**
     * 发布视频笔记到小红书
     */
    @Tool(name = "publishVideoNote", description = "发布视频笔记到小红书平台。需要提前配置好浏览器和Cookie。")
    public String publishVideoNote(
            @ToolParam(description = "视频标题，长度不超过 20 个字符", required = true) String title,
            @ToolParam(description = "视频内容描述，长度不超过 5000 个字符", required = true) String content,
            @ToolParam(description = "本地视频文件路径，如 \"C:/Videos/video1.mp4\"", required = true) String videoPath,
            @ToolParam(description = "话题标签列表，数量不超过10个，如 [\"#视频\", \"#Vlog\"]") List<String> tags,
            @ToolParam(description = "定时发布时间，格式 yyyy-MM-dd HH:mm，如 2024-12-25 10:00，不定时则传 null") LocalDateTime scheduleTime,
            @ToolParam(description = "可见性：公开可见、仅自己可见、仅互关好友可见，默认为公开可见") String visibility,
            @ToolParam(description = "商品关键词列表，用于绑定商品") List<String> products) {
        try {
            log.info("开始发布视频笔记：{}", title);

            // 1. 验证必要参数
            if (title == null || title.trim().isEmpty()) {
                return createErrorResponse("视频标题不能为空");
            }
            if (content == null || content.trim().isEmpty()) {
                return createErrorResponse("视频内容不能为空");
            }
            if (videoPath == null || videoPath.trim().isEmpty()) {
                return createErrorResponse("视频路径不能为空");
            }

            // 2. 创建浏览器实例（如果还没有）
            PageWrapper pageWrapper = ensureBrowserSession();
            if (pageWrapper == null) {
                return createErrorResponse("无法创建浏览器会话，请检查浏览器配置");
            }

            // 3. 设置发布操作类的 pageWrapper
            publishVideoAction.setPageWrapper(pageWrapper);

            // 4. 创建请求对象
            XhsPublishReq req = new XhsPublishReq();
            req.setTitle(title);
            req.setContent(content);
            req.setTags(tags);
            req.setImagePaths(List.of(videoPath));
            req.setScheduleTime(scheduleTime);
            req.setVisibility(visibility != null ? visibility : "公开可见");
            req.setProducts(products);
            req.setIsOriginal(false);

            // 5. 执行发布
            publishVideoAction.publish(req);

            log.info("视频笔记发布成功：{}", title);
            return createSuccessResponse("视频笔记发布成功", Map.of("title", title));

        } catch (Exception e) {
            log.error("❌ 发布视频笔记时出现异常: {}", e.getMessage(), e);
            return createErrorResponse("发布视频笔记时出现异常: " + e.getMessage());
        }
    }

    /**
     * 确保浏览器会话可用
     */
    private PageWrapper ensureBrowserSession() {
        try {
            BrowserConfig browserConfig = new BrowserConfig();
            browserConfig.setHeadless(true); // MCP服务使用无头模式
            browserConfig.setBinPath(null);

            PageWrapper pageWrapper = browserManager.newBrowser(browserConfig);

            // 如果需要登录，执行登录
            // TODO: 可以在这里添加自动登录逻辑

            return pageWrapper;
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
