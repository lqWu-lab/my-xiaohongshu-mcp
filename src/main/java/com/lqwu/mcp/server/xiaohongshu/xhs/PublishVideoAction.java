package com.lqwu.mcp.server.xiaohongshu.xhs;

import com.lqwu.mcp.server.xiaohongshu.browser.PageWrapper;
import com.lqwu.mcp.server.xiaohongshu.xhs.req.XhsPublishReq;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 小红书发布视频内容功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublishVideoAction extends AbstractPublishAction{

    private static final long MAX_WAIT_MS = 10 * 60 * 1000;
    private static final long INTERVAL_MS = 1000;

    /**
     * 发布内容
     * @param req 发布内容参数
     */
    public void publish(XhsPublishReq req){
        // 1.导航到发布页面
        navigateToPublishPage("上传视频");
        // 2.检查视频列表，并上传
        if (req.getImagePaths() == null || req.getImagePaths().isEmpty()){
            throw new IllegalArgumentException("视频不能为空");
        }
        uploadVideo(req.getImagePaths().get(0));
        // 3.输入标题、内容
        fillTitle(req.getTitle());
        // 等待标题输入稳定
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Locator contentEditor = fillContent(req.getContent());
        // 等待正文输入稳定
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // 4.输入标签
        List<String> tags = req.getTags();
        if (tags != null && tags.size() >= 10){
            log.warn("标签数量超过 10，截取前 10 个");
            tags = tags.subList(0, 10);
        }
        inputTags(contentEditor, tags);
        // 等待标签输入完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // 5.设置可见范围
        if (req.getVisibility() != null) {
            setVisibility(req.getVisibility());
        }
        // 6.设置定时发布
        if (req.getScheduleTime() != null){
            setSchedulePublish(req.getScheduleTime());
        }
        // 7.绑定商品
        if (req.getProducts() != null && !req.getProducts().isEmpty()){
            bindProducts(req.getProducts());
        }
        // 8.提交发布
        submitPublish();
    }

    /**
     * 提交发布
     */
    private void submitPublish() {
        // 等待发布按钮可点击
        Locator btn = waitForPublishButtonClickable();
        if (btn == null) {
            throw new RuntimeException("等待发布按钮可点击超时");
        }
        // 点击发布
        btn.click();
        log.info("已点击发布按钮");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 上传视频
     * @param videoPath 视频路径
     */
    private void uploadVideo(String videoPath){
        Page page = pageWrapper.getPage();
        // 1.检验视频路径是否存在
        Path path = Paths.get(videoPath);
        if (!Files.exists(path)){
            throw new RuntimeException("视频文件不存在：" + videoPath);
        }
        // 2.获取上传输入框
        Locator fileInput = page.locator(".upload-input").first();
        if (fileInput.count() == 0) {
            fileInput = page.locator("input[type='file']").first();
        }
        // 3.检验上传输入框
        if (fileInput.count() == 0) {
            throw new RuntimeException("未找到视频上传输入框");
        }
        // 4.上传视频
        fileInput.setInputFiles(path);
        log.info("上传视频成功：{}", path);
        // 5.等待上传完成
        waitForPublishButtonClickable();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 等待发布按钮可点击
     * @return 发布按钮元素
     */
    private Locator waitForPublishButtonClickable() {
        Page page = pageWrapper.getPage();
        String selector = ".publish-page-publish-btn button.bg-red";
        log.info("开始等待发布按钮可点击(视频)");

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < MAX_WAIT_MS) {
            Locator btn = page.locator(selector).first();
            if (btn.count() > 0) {
                try {
                    if (btn.isVisible()) {
                        String disabled = btn.getAttribute("disabled");
                        String className = btn.getAttribute("class");
                        if (disabled == null && className != null && !className.contains("disabled")) {
                            log.info("发布按钮可点击");
                            return btn;
                        }
                        if (disabled == null) {
                            return btn;
                        }
                    }
                } catch (Exception e) {
                    // 忽略异常继续等待
                }
            }
            try {
                Thread.sleep(INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new RuntimeException("等待发布按钮可点击超时");
    }
}
