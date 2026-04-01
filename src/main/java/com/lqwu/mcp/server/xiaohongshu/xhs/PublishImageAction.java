package com.lqwu.mcp.server.xiaohongshu.xhs;

import com.lqwu.mcp.server.xiaohongshu.xhs.req.XhsPublishReq;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * 小红书发布图文内容功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublishImageAction extends AbstractPublishAction{

    /**
     * 发布内容
     * @param req 发布内容参数
     */
    public void publish(XhsPublishReq req){
        // 0.导航到发布页面
        navigateToPublishPage("上传图文");
        // 1.检测图片集合，上传图片
        if (req.getImagePaths() == null || req.getImagePaths().isEmpty()){
            throw new IllegalArgumentException("图片不能为空");
        }
        uploadImages(req.getImagePaths());

        // 等待所有图片上传完成并稳定显示（大图片需要更长时间）
        try {
            Thread.sleep(5000);  // 增加等待时间到 5 秒
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 2.填写标题、内容
        fillTitle(req.getTitle());
        // 等待标题输入稳定
        try {
            Thread.sleep(1000);  // 增加等待时间到 1 秒
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Locator contentEditor = fillContent(req.getContent());
        // 等待正文输入稳定
        try {
            Thread.sleep(2000);  // 增加等待时间到 2 秒
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 3.设置标签
        List<String> tags = req.getTags();
        if (tags != null && tags.size() >= 10){
            log.warn("标签数量超过 10，截取前 10 个");
            tags = tags.subList(0, 10);
        }
        inputTags(contentEditor, tags);
        // 4.设置定时发布
        if (req.getScheduleTime() != null){
            setSchedulePublish(req.getScheduleTime());
        }
        // 5.设置可见性
        setVisibility(req.getVisibility());
        // 6.设置原创声明
        if (req.getIsOriginal()){
            setOriginalDeclaration();
        }
        // 7.绑定商品
        if (req.getProducts() != null && !req.getProducts().isEmpty()){
            bindProducts(req.getProducts());
        }
        // 8.提交发布
        submitPublish();
    }

    /**
     * 上传图片
     * @param imagePaths 图片路径集合
     */
    private void uploadImages(List<String> imagePaths){
        Page page = pageWrapper.getPage();
        // 1.判断图片地址是否有效
        List<String> validPaths = new ArrayList<>();
        for (String imagePath : imagePaths) {
            if (Files.exists(Paths.get(imagePath))){
                validPaths.add(imagePath);
            } else {
                log.warn("图片文件不存在：{}", imagePath);
            }
        }
        // 2.循环上传
        for (int i = 0; i < validPaths.size(); i++) {
            // 上传控件的选择器
            String selector = (i == 0) ? ".upload-input" : "input[type='file']";

            // 找到上传输入框
            Locator uploadInput = page.locator(selector).first();

            // 直接设置文件
            // 不需要 waitForFileChooser，因为 input[type='file'] 是隐藏的
            uploadInput.setInputFiles(Paths.get(validPaths.get(i)));

            log.info("上传图片成功：{}", validPaths.get(i));

            // 等待上传完成
            waitForUploadComplete(i + 1);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 等待图片上传完成
     * @param expectedCount 预期数量
     */
    private void waitForUploadComplete(int expectedCount) {
        long startTime = System.currentTimeMillis();// 开始时间
        long maxWait = 120000;                      // 最大等待时间增加到 120 秒（大图片需要更长时间）
        Page page = pageWrapper.getPage();
        int lastLogCount = expectedCount - 1;
        // 循环等待
        while(System.currentTimeMillis() - startTime < maxWait){
            Locator uploadedImages = page.locator(".img-preview-area .pr");
            int currentCount = uploadedImages.count();
            // 数量变化时才打印，避免刷屏
            if (currentCount != lastLogCount) {
                log.info("等待图片上传: current={}, expected={}", currentCount, expectedCount);
                lastLogCount = currentCount;
            }
            if (currentCount >= expectedCount) {
                log.info("图片上传完成：{}", currentCount);
                // 大图片需要更长的处理时间（缩略图生成、压缩等）
                try {
                    Thread.sleep(3000);  // 增加等待时间到 3 秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
            try {
                Thread.sleep(1000);  // 增加检查间隔到 1 秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        // 超时后才抛出异常
        throw new RuntimeException("第" + expectedCount + "张图片上传超时(120秒)，请检查网络连接和图片大小");
    }

    private void submitPublish() {
        Page page = pageWrapper.getPage();
        Locator publishBtn = page.locator(".publish-page-publish-btn button.bg-red");
        publishBtn.click();

        log.info("已点击发布按钮");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
