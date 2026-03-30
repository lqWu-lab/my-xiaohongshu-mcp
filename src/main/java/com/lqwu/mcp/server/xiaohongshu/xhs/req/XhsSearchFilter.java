package com.lqwu.mcp.server.xiaohongshu.xhs.req;

import lombok.Data;

/**
 * 小红书搜索筛选条件
 */
@Data
public class XhsSearchFilter {

    /**
     * 排序依据
     */
    private String sortBy;

    /**
     * 笔记类型
     */
    private String noteType;

    /**
     * 发布时间
     */
    private String publishTime;

    /**
     * 搜索范围
     */
    private String searchScope;

    /**
     * 位置距离
     */
    private String location;
}
