package com.lqwu.mcp.server.xiaohongshu.xhs.req;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 小红书发布内容请求参数
 */
@Data
public class XhsPublishReq {

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * url 列表（可以是图片的，也可以是视频的）
     */
    private List<String> imagePaths;

    /**
     * 定时发布时间
     */
    private LocalDateTime scheduleTime;

    /**
     * 是否原创
     */
    private Boolean isOriginal;

    /**
     * 访问权限
     */
    private String visibility = "公开可见";

    /**
     * 商品列表
     */
    private List<String> products;
}
