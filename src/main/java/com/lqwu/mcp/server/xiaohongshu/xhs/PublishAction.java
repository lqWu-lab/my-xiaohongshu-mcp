package com.lqwu.mcp.server.xiaohongshu.xhs;

import com.lqwu.mcp.server.xiaohongshu.browser.PageWrapper;
import com.lqwu.mcp.server.xiaohongshu.util.CookieManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 小红书发布内容功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublishAction {

    private PageWrapper pageWrapper;
    private final CookieManager cookieManager;
}
