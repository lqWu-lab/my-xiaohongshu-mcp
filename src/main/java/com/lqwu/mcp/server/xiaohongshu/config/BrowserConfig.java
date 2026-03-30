package com.lqwu.mcp.server.xiaohongshu.config;

import lombok.Data;

/**
 * 浏览器配置类
 */
@Data
public class BrowserConfig {

    /**
     * 浏览器路径
     */
    private String binPath;

    /**
     * 是否启用无头模式
     */
    private boolean headless = true;

    /**
     * 代理
     */
    private String proxy;
    
}
