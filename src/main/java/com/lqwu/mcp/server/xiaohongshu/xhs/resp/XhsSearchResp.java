package com.lqwu.mcp.server.xiaohongshu.xhs.resp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 小红书搜索结果
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class XhsSearchResp {

    private String id;

    /**
     * 模型类型（note/video 等）
     */
    private String modelType;

    /**
     * 笔记卡片信息
     */
    private NoteCard noteCard;

    /**
     * 类型
     */
    private String type;

    /**
     * 显示标题
     */
    private String displayTitle;

    /**
     * 笔记卡片数据
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NoteCard {
        
        /**
         * 角标信息列表
         */
        private List<CornerTagInfo> cornerTagInfo;
        
        /**
         * 类型（video/note 等）
         */
        private String type;
        
        /**
         * 用户信息
         */
        private User user;
        
        /**
         * 互动信息
         */
        private InteractInfo interactInfo;
        
        /**
         * 封面信息
         */
        private Cover cover;
        
        /**
         * 图片列表
         */
        private List<ImageList> imageList;
    }

    /**
     * 角标信息
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CornerTagInfo {
        
        /**
         * 类型（如 publish_time）
         */
        private String type;
        
        /**
         * 文本内容（如"3 天前"）
         */
        private String text;
    }

    /**
     * 用户信息
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        
        /**
         * 昵称
         */
        private String nickName;
        
        /**
         * 头像 URL
         */
        private String avatar;
        
        /**
         * 用户 ID
         */
        private String userId;
        
        /**
         * 昵称（冗余字段）
         */
        private String nickname;
        
        /**
         * XsecToken
         */
        private String xsecToken;
    }

    /**
     * 互动信息
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InteractInfo {
        
        /**
         * 是否已点赞
         */
        private boolean liked;
        
        /**
         * 点赞数（字符串格式）
         */
        private String likedCount;
        
        /**
         * 是否已收藏
         */
        private boolean collected;
        
        /**
         * 收藏数（字符串格式）
         */
        private String collectedCount;
        
        /**
         * 评论数（字符串格式）
         */
        private String commentCount;
        
        /**
         * 分享数（字符串格式）
         */
        private String sharedCount;
    }

    /**
     * 封面信息
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cover {
        
        /**
         * 默认 URL
         */
        private String urlDefault;
        
        /**
         * 预览 URL
         */
        private String urlPre;
        
        /**
         * 高度
         */
        private int height;
        
        /**
         * 宽度
         */
        private int width;
    }

    /**
     * 图片列表项
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageList {
        
        /**
         * 图片场景信息列表
         */
        private List<InfoList> infoList;
        
        /**
         * 高度
         */
        private int height;
        
        /**
         * 宽度
         */
        private int width;
    }

    /**
     * 图片场景信息
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InfoList {
        
        /**
         * 图片场景（如 WB_DFT, WB_PRV）
         */
        private String imageScene;
        
        /**
         * 图片 URL
         */
        private String url;
    }
}
