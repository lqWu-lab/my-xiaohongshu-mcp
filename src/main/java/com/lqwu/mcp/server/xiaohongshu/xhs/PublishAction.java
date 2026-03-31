package com.lqwu.mcp.server.xiaohongshu.xhs;

import com.lqwu.mcp.server.xiaohongshu.browser.PageWrapper;
import com.lqwu.mcp.server.xiaohongshu.util.CookieManager;
import com.lqwu.mcp.server.xiaohongshu.xhs.req.XhsPublishReq;
import com.microsoft.playwright.FileChooser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 小红书发布内容功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublishAction {

    @Setter
    private PageWrapper pageWrapper;

    // 发布内容页面 url
    private static final String PUBLISH_URL = "https://creator.xiaohongshu.com/publish/publish?source=official";

    /**
     * 发布内容
     * @param req 发布内容参数
     */
    public void publish(XhsPublishReq req){
        // 0.导航到发布页面
        navigateToPublishPage();
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
        if (req.isOriginal()){
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
     * 导航到发布页面
     */
    private void navigateToPublishPage() {
        Page page = pageWrapper.getPage();
        log.info("导航到发布页面: {}", PUBLISH_URL);
        page.navigate(PUBLISH_URL);

        // 等待页面加载（参考 Go 版本的 WaitLoad）
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 等待 DOM 稳定
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 点击"上传图文" tab
        clickPublishTab("上传图文");

        // 等待 tab 切换完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("发布页面加载完成");
    }

    /**
     * 点击发布 TAB
     * @param tabName tab 名称
     */
    private void clickPublishTab(String tabName) {
        Page page = pageWrapper.getPage();
        log.info("点击发布 TAB: {}", tabName);
        try {
            // 1.等待上传内容区域出现
            page.locator("div.upload-content").waitFor();
            // 2.查找所有 tab
            List<Locator> tabs = page.locator("div.creator-tab").all();
            for (Locator tab : tabs) {
                if (!tab.isVisible()) {
                    continue;
                }
                String text = tab.textContent().trim();
                if (!tabName.equals(text)) {
                    continue;
                }
                // 检查元素是否被遮挡
                if (isElementBlocked(tab)) {
                    removePopCover();
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                tab.click();
                log.info("已点击发布 TAB: {}", tabName);
                return;
            }
            throw new RuntimeException("未找到发布 TAB: " + tabName);
        } catch (Exception e) {
            log.error("点击发布 TAB 失败: {}", e.getMessage());
            throw new RuntimeException("点击发布 TAB 失败", e);
        }
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

            // 找到上传输入框（参考 Go 版本的 page.Element）
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
            // 数量变化时才打印，避免刷屏（参考 Go 版本）
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

    /**
     * 填写标题
     * @param title 标题
     */
    private void fillTitle(String title){
        Page page = pageWrapper.getPage();
        Locator titleInput = page.locator("div.d-input input");
        titleInput.fill(title);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        checkTitleMaxLength();
    }

    /**
     * 检查标题长度
     */
    private void checkTitleMaxLength() {
        Page page = pageWrapper.getPage();
        Locator errorElement = page.locator("div.title-container div.max_suffix");
        if (errorElement.isVisible()) {
            String text = errorElement.textContent();
            throw new RuntimeException("标题长度超限：" + text);
        }
    }

    /**
     * 填写内容
     * @param content 内容
     * @return 内容编辑器定位器
     */
    private Locator fillContent(String content){
        Page page = pageWrapper.getPage();
        Locator contentEditor = page.locator("div.ql-editor").first();
        if (contentEditor.count() == 0) {
            contentEditor = findContentEditorByPlaceholder();
        }
        contentEditor.fill(content);
        // 回点标题输入框，确保发布时数据保存
        Locator titleInput = page.locator("div.d-input input");
        titleInput.click();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        checkContentMaxLength();
        return contentEditor;
    }

    /**
     * 通过 placeholder 寻找内容输入框
     * @return 内容输入框
     */
    private Locator findContentEditorByPlaceholder() {
        Page page = pageWrapper.getPage();
        // 1.获取所有段落元素
        List<Locator> paragraphs = page.locator("p").all();
        // 2.遍历查找目标占位符
        for (Locator p : paragraphs) {
            String placeholder = p.getAttribute("data-placeholder");
            if (placeholder != null && placeholder.contains("输入正文描述")) {
                // 3. 向上查找父级容器
                Locator parent = p.locator("..");
                for (int i = 0; i < 5; i++) {
                    String role = parent.getAttribute("role");
                    // 4.寻找 role="textbox" 的元素（这是可编辑区域的标识）
                    if ("textbox".equals(role)) {
                        return parent;
                    }
                    parent = parent.locator("..");
                }
            }
        }
        throw new RuntimeException("未找到内容输入框");
    }

    /**
     * 检查正文内容长度
     */
    private void checkContentMaxLength() {
        Page page = pageWrapper.getPage();
        Locator errorElement = page.locator("div.edit-container div.length-error");
        if (errorElement.isVisible()) {
            String text = errorElement.textContent();
            throw new RuntimeException("正文长度超限：" + text);
        }
    }

    /**
     * 填写标签
     * @param contentEditor 内容编辑器定位器（从 fillContent 获取）
     * @param tags 标签集合
     */
    private void inputTags(Locator contentEditor, List<String> tags){
        Page page = pageWrapper.getPage();
        if(tags == null || tags.isEmpty()){
            return;
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        contentEditor.click();
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (int i = 0; i < 20; i++) {
            page.keyboard().press("ArrowDown");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        page.keyboard().press("Enter");
        page.keyboard().press("Enter");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (String tag : tags) {
            inputSingleTag(tag);
        }
    }

    /**
     * 输入单个标签
     * @param tag 标签内容，会自动移除开头的#符号
     */
    private void inputSingleTag(String tag) {
        Page page = pageWrapper.getPage();
        // 1.移除标签开头的#符号，避免重复
        tag = tag.replaceFirst("^#", "");
        // 2.先输入#符号触发标签功能
        page.keyboard().type("#");
        // 3.短暂延迟等待界面响应
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // 4.逐个字符输入标签内容，模拟人工输入速度以触发联想
        for (char c : tag.toCharArray()) {
            page.keyboard().type(String.valueOf(c));
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // 5.等待标签联想列表加载完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // 6.查找标签联想容器
        Locator topicContainer = page.locator("#creator-editor-topic-container");
        // 7.如果标签联想容器未显示，输入空格结束标签输入
        if (!topicContainer.isVisible()) {
            page.keyboard().type(" ");
            return;
        }
        // 8.获取联想列表的第一个选项
        Locator firstItem = topicContainer.locator(".item").first();
        // 9.如果第一个选项可见，则点击选择；否则输入空格结束标签输入
        if (firstItem.isVisible()) {
            firstItem.click();
            log.info("已选择标签联想：{}", tag);
        } else {
            page.keyboard().type(" ");
        }
        // 10.等待标签选择完成
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 设置定时发布
     * 该方法用于配置笔记的定时发布时间，通过点击定时发布开关并输入指定的发布时间来实现。
     * @param scheduleTime 定时发布时间，格式为 LocalDateTime
     */
    private void setSchedulePublish(LocalDateTime scheduleTime){
        Page page = pageWrapper.getPage();
        // 1.定位并点击定时发布开关
        Locator switchElem = page.locator(".post-time-wrapper .d-switch");
        switchElem.click();
        // 2.等待开关切换完成
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // 3.格式化时间为小红书接受的格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String dateTimeStr = scheduleTime.format(formatter);
        // 4.定位日期输入框并填入格式化后的时间
        Locator dateInput = page.locator(".date-picker-container input");
        dateInput.selectText();
        dateInput.fill(dateTimeStr);
        log.info("已设置定时发布时间：{}", dateTimeStr);
        // 5.等待输入完成
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 设置可见性
     * @param visibility 可见性
     */
    private void setVisibility(String visibility){
        // 1.检查可见性参数
        Page page = pageWrapper.getPage();
        if (visibility == null || "公开可见".equals(visibility)) {
            log.info("使用默认可见范围：公开可见");
            return;
        }
        // 2.检查可见性参数是否支持
        Set<String> supported = new HashSet<>(Arrays.asList("仅自己可见", "仅互关好友可见"));
        if (!supported.contains(visibility)) {
            throw new IllegalArgumentException("不支持的可见范围：" + visibility);
        }
        // 3.点击可见性下拉框
        Locator dropdown = page.locator("div.permission-card-wrapper div.d-select-content");
        dropdown.click();
        // 4.等待可见性下拉框加载完成
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // 5.查找可见性选项
        List<Locator> options = page.locator("div.d-options-wrapper div.d-grid-item div.custom-option").all();
        // 6.遍历选项，查找匹配的选项并点击
        for (Locator opt : options) {
            String text = opt.textContent();
            if (text.contains(visibility)) {
                opt.click();
                log.info("已设置可见范围：{}", visibility);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
        }
        throw new RuntimeException("未找到可见范围选项：" + visibility);
    }

    /**
     * 设置原创声明
     */
    private void setOriginalDeclaration(){
        Page page = pageWrapper.getPage();
        // 1.获取所有开关卡
        List<Locator> switchCards = page.locator("div.custom-switch-card").all();
        // 2.遍历开关卡，查找原创声明
        for (Locator card : switchCards) {
            String text = card.textContent();
            if (!text.contains("原创声明")) {
                continue;
            }
            // 3.查找开关元素
            Locator switchElem = card.locator("div.d-switch").first();
            // 4.检查开关状态（在该开关上下文中查找 checkbox）
            Boolean isChecked = (Boolean) switchElem.evaluate("""
            () => {
                const input = this.querySelector('input[type="checkbox"]');
                return input ? input.checked : false;
            }
        """);
            if (isChecked != null && isChecked) {
                log.info("原创声明已开启");
                return;
            }
            // 5.点击开关
            switchElem.click();
            // 6.等待开关状态更新
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 7.确认原创声明
            confirmOriginalDeclaration();
            log.info("已开启原创声明");
            return;
        }

        throw new RuntimeException("未找到原创声明选项");
    }

    /**
     * 确认原创声明
     */
    private void confirmOriginalDeclaration() {
        Page page = pageWrapper.getPage();
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        page.evaluate("""
        () => {
            const footers = document.querySelectorAll('div.footer');
            for (const footer of footers) {
                if (footer.textContent.includes('原创声明须知')) {
                    const checkbox = footer.querySelector('input[type="checkbox"]');
                    if (checkbox && !checkbox.checked) {
                        checkbox.click();
                    }
                    return 'found';
                }
            }
            return 'not_found';
        }
    """);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Object result = page.evaluate("""
        () => {
            const footers = document.querySelectorAll('div.footer');
            for (const footer of footers) {
                if (footer.textContent.includes('声明原创')) {
                    const btn = footer.querySelector('button');
                    if (btn && !btn.disabled) {
                        btn.click();
                        return 'clicked';
                    }
                }
            }
            return 'not_found';
        }
    """);

        if ("not_found".equals(result)) {
            throw new RuntimeException("未找到声明原创按钮");
        }

        log.info("已成功点击声明原创按钮");
    }

    /**
     * 绑定商品
     * @param products 商品集合
     */
    private void bindProducts(List<String> products){
        log.info("开始绑定商品：{}", products);

        clickAddProductButton();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Locator modal = waitForProductModal();
        log.info("商品选择弹窗已打开");

        List<String> failedProducts = new ArrayList<>();

        for (String keyword : products) {
            try {
                searchAndSelectProduct(modal, keyword);
            } catch (Exception e) {
                log.warn("搜索选择商品失败：{} - {}", keyword, e.getMessage());
                failedProducts.add(keyword);
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        clickModalSaveButton(modal);
        waitForModalClose();

        if (!failedProducts.isEmpty()) {
            throw new RuntimeException("部分商品未找到：" + failedProducts);
        }

        log.info("商品绑定完成：{}", products.size());
    }

    private void clickAddProductButton() {
        Page page = pageWrapper.getPage();
        List<Locator> spans = page.locator("span.d-text").all();

        for (Locator span : spans) {
            String text = span.textContent().trim();
            if ("添加商品".equals(text)) {
                Locator parent = span.locator("..");
                for (int i = 0; i < 5; i++) {
                    String tagName = (String) page.evaluate(
                            "(el) => el.tagName.toLowerCase()",
                            parent.elementHandle()
                    );

                    if ("button".equals(tagName)) {
                        parent.click();
                        log.info("已点击添加商品按钮");

                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return;
                    }

                    parent = parent.locator("..");
                }
            }
        }

        throw new RuntimeException("未找到添加商品按钮");
    }

    private Locator waitForProductModal() {
        long startTime = System.currentTimeMillis();
        long timeout = 10000;
        Page page = pageWrapper.getPage();

        while (System.currentTimeMillis() - startTime < timeout) {
            Locator modal = page.locator(".multi-goods-selector-modal");
            if (modal.isVisible()) {
                log.info("商品选择弹窗已出现");
                return modal;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        throw new RuntimeException("等待商品弹窗超时");
    }

    private void searchAndSelectProduct(Locator modal, String keyword) {
        Page page = pageWrapper.getPage();
        log.info("搜索商品：{}", keyword);

        Locator searchInput = modal.locator("input[placeholder='搜索商品 ID 或 商品名称']");
        searchInput.selectText();
        searchInput.fill(keyword);

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        page.keyboard().press("Enter");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        waitForLoadingDisappear(modal);

        Locator checkbox = modal.locator(".goods-list-normal .good-card-container .d-checkbox").first();

        boolean isChecked = (boolean) page.evaluate("""
        (el) => {
            return el.querySelector('.checked') !== null;
        }
    """, checkbox.elementHandle());

        if (!isChecked) {
            checkbox.click();
        }

        try {
            Thread.sleep(800 + new Random().nextInt(700));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("已选择商品：{}", keyword);
    }

    private void waitForLoadingDisappear(Locator modal) {
        long startTime = System.currentTimeMillis();
        long timeout = 10000;

        while (System.currentTimeMillis() - startTime < timeout) {
            Locator loading = modal.locator(".goods-list-loading");
            if (!loading.isVisible()) {
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void clickModalSaveButton(Locator modal) {
        Locator saveBtn = modal.locator(".goods-selected-footer button").first();
        if (saveBtn.isVisible()) {
            saveBtn.click();
            log.info("已点击保存按钮");
        }
    }

    private void waitForModalClose() {
        long startTime = System.currentTimeMillis();
        long timeout = 5000;
        Page page = pageWrapper.getPage();
        while (System.currentTimeMillis() - startTime < timeout) {
            Locator modal = page.locator(".multi-goods-selector-modal");
            if (!modal.isVisible()) {
                log.info("弹窗已关闭");
                return;
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
//
//    private void clickPublishTab(String tabName) {
//        Page page = pageWrapper.getPage();
//        page.locator("div.upload-content").waitFor();
//
//        long deadline = System.currentTimeMillis() + 15000;
//
//        while (System.currentTimeMillis() < deadline) {
//            List<Locator> tabs = page.locator("div.creator-tab").all();
//
//            for (Locator tab : tabs) {
//                if (!tab.isVisible()) {
//                    continue;
//                }
//
//                String text = tab.textContent().trim();
//                if (!tabName.equals(text)) {
//                    continue;
//                }
//
//                if (isElementBlocked(tab)) {
//                    removePopCover();
//
//                    try {
//                        Thread.sleep(200);
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                    }
//                    continue;
//                }
//
//                tab.click();
//                log.info("已点击发布 TAB: {}", tabName);
//                return;
//            }
//
//            try {
//                Thread.sleep(200);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//
//        throw new RuntimeException("未找到发布 TAB: " + tabName);
//    }

    private boolean isElementBlocked(Locator elem) {
        Page page = pageWrapper.getPage();
        return (boolean) page.evaluate("""
        (el) => {
            const rect = el.getBoundingClientRect();
            if (rect.width === 0 || rect.height === 0) {
                return true;
            }
            const x = rect.left + rect.width / 2;
            const y = rect.top + rect.height / 2;
            const target = document.elementFromPoint(x, y);
            return !(target === el || el.contains(target));
        }
    """, elem.elementHandle());
    }

    private void removePopCover() {
        Page page = pageWrapper.getPage();
        Locator popover = page.locator("div.d-popover").first();
        if (popover.isVisible()) {
            popover.evaluate("(el) => el.remove()");
        }

        clickEmptyPosition();
    }

    private void clickEmptyPosition() {
        Page page = pageWrapper.getPage();
        Random rand = new Random();
        int x = 380 + rand.nextInt(100);
        int y = 20 + rand.nextInt(60);

        // 移动到随机位置并点击，模拟用户操作
        page.mouse().click(x,y);

        // 等待点击完成
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
