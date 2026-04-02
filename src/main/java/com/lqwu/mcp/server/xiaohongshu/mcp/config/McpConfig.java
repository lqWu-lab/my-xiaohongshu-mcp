package com.lqwu.mcp.server.xiaohongshu.mcp.config;

import com.lqwu.mcp.server.xiaohongshu.mcp.tools.XhsLoginService;
import com.lqwu.mcp.server.xiaohongshu.mcp.tools.XhsPublishService;
import com.lqwu.mcp.server.xiaohongshu.mcp.tools.XhsSearchService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP 配置类
 * 用于注册 MCP 工具服务
 */
@Configuration
public class McpConfig {

    /**
     * 注册小红书登录服务工具
     */
    @Bean
    public ToolCallbackProvider xhsLoginTools(XhsLoginService loginService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(loginService)
                .build();
    }

    /**
     * 注册小红书发布服务工具
     */
    @Bean
    public ToolCallbackProvider xhsPublishTools(XhsPublishService publishService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(publishService)
                .build();
    }

    /**
     * 注册小红书搜索服务工具
     */
    @Bean
    public ToolCallbackProvider xhsSearchTools(XhsSearchService searchService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(searchService)
                .build();
    }
}
