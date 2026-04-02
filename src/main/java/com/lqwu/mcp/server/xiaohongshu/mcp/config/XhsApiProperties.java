package com.lqwu.mcp.server.xiaohongshu.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 小红书 API 配置属性
 */
@Component
@ConfigurationProperties(prefix = "xhs.api")
public class XhsApiProperties {

    /**
     * 小红书用户的 Cookie 认证信息
     */
    private String cookie;

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    /**
     * 判断 Cookie 是否有效
     */
    public boolean isValidCookie() {
        return cookie != null &&
                !cookie.trim().isEmpty() &&
                !cookie.contains("填写你的cookie");
    }
}
