package com.lqwu.mcp.server.xiaohongshu.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 小红书 API 配置属性
 */
@Component
@ConfigurationProperties(prefix = "xhs.api")
public class XhsApiProperties {
}
