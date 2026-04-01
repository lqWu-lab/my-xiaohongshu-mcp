package com.lqwu.mcp.server.xiaohongshu.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqwu.mcp.server.xiaohongshu.mcp.config.XhsApiProperties;
import com.lqwu.mcp.server.xiaohongshu.xhs.PublishImageAction;
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
    private final XhsApiProperties xhsApiProperties;
    private final ObjectMapper objectMapper;


    @Tool(name = "publishImageNote", description = "发布图文笔记到小红书平台。Cookie和分类已通过配置文件预设。")
    public String publishImageNote(
            @ToolParam(description = "笔记标题，长度不超过 20 个字符") String title,
            @ToolParam(description = "笔记内容，长度不超过 5000 个字符") String content,
            @ToolParam(description = "话题标签列表，数量不超过10个") List<String> tags,
            @ToolParam(description = "本地图片文件路径列表，如 [\"C:/Photos/img1.jpg\"]") List<String> imagePaths,
            @ToolParam(description = "定时发布时间，格式 yyyy-MM-dd HH:mm，如 2024-12-25 10:00，不定时则传 null") LocalDateTime scheduleTime,
            boolean isOriginal,
            String visibility,
            List<String> products) {
        try{
            log.info("开始发布图文笔记：{}", title);
            // 1.首先验证必要参数
            if (title == null || title.trim().isEmpty()) {
                return createErrorResponse("文章标题不能为空");
            }
            if (content == null || content.trim().isEmpty()) {
                return createErrorResponse("文章内容不能为空");
            }
            if (imagePaths == null || imagePaths.isEmpty()){
                return createErrorResponse("图片列表不能为空");
            }
            // 2.然后检查配置的Cookie是否有效

            // 3.创建请求对象，使用配置文件中的values
            XhsPublishReq req = new XhsPublishReq();
            req.setTitle(title);
            req.setContent(content);
            req.setTags(tags);
            req.setImagePaths(imagePaths);
            req.setScheduleTime(scheduleTime);
            req.setVisibility(visibility);
            req.setProducts(products);
            req.setIsOriginal(isOriginal);
            // 4.

            return null;
        } catch (Exception e){
            log.error("❌ 发布图文笔记时出现异常: {}", e.getMessage(), e);
            return createErrorResponse("发布图文笔记时出现异常: " + e.getMessage());
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
