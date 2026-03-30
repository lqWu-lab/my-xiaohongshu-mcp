package com.lqwu.mcp.server.xiaohongshu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqwu.mcp.server.xiaohongshu.browser.BrowserManager;
import com.lqwu.mcp.server.xiaohongshu.browser.PageWrapper;
import com.lqwu.mcp.server.xiaohongshu.config.BrowserConfig;
import com.lqwu.mcp.server.xiaohongshu.util.CookieManager;
import com.lqwu.mcp.server.xiaohongshu.xhs.LoginAction;

import java.util.Scanner;

/**
 * 小红书登录辅助工具
 *
 * <p>该工具用于首次登录小红书账号并保存 cookies。
 *
 * <p>使用方法：
 * <pre>
 *     // 方式1: 直接运行 main 方法
 *     LoginHelper.main(new String[]{});
 *
 *     // 方式2: 在 IDE 中运行 LoginHelper 类
 * </pre>
 *
 * <p>登录流程：
 * <ol>
 *   <li>启动浏览器（可视化模式）</li>
 *   <li>导航到小红书页面，显示登录二维码</li>
 *   <li>等待用户用手机小红书 App 扫码</li>
 *   <li>登录成功后自动保存 cookies 到 ~/.xiaohongshu-mcp/cookies.json</li>
 *   <li>关闭浏览器</li>
 * </ol>
 *
 * <p>注意：只需要登录一次，后续测试会自动使用保存的 cookies。
 *
 * @author lqwu
 * @version 1.0
 */
public class LoginHelper {

    /**
     * 默认超时时间（秒）- 等待用户扫码的时间
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 120;

    /**
     * 运行登录流程
     *
     * @param args 命令行参数（可选）
     *             <ul>
     *               <li>args[0]: 超时时间（秒），默认 120 秒</li>
     *             </ul>
     */
    public static void main(String[] args) {
        // 1. 解析超时参数
        int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        if (args != null && args.length > 0) {
            try {
                timeoutSeconds = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("无效的超时参数，使用默认值: " + DEFAULT_TIMEOUT_SECONDS + " 秒");
            }
        }

        // 2. 打印欢迎信息
        printWelcome(timeoutSeconds);

        // 3. 执行登录
        try {
            boolean success = doLogin(timeoutSeconds);

            // 4. 打印结果
            if (success) {
                printSuccess();
            } else {
                printFailure();
            }
        } catch (Exception e) {
            System.err.println("登录过程发生错误: " + e.getMessage());
            e.printStackTrace();
        }

        // 5. 等待用户确认
        waitForExit();
    }

    /**
     * 执行登录流程
     *
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否登录成功
     */
    private static boolean doLogin(int timeoutSeconds) {
        try {
            // 1. 初始化组件
            System.out.println("\n[1/5] 初始化组件...");
            ObjectMapper objectMapper = new ObjectMapper();
            CookieManager cookieManager = new CookieManager(objectMapper);
            BrowserConfig browserConfig = new BrowserConfig();
            browserConfig.setHeadless(false); // 可视化模式
            browserConfig.setBinPath(null);

            System.out.println("[2/5] 启动浏览器...");
            BrowserManager browserManager = new BrowserManager(cookieManager, objectMapper);
            PageWrapper pageWrapper = browserManager.newBrowser(browserConfig);

            System.out.println("[3/5] 创建登录会话...");
            LoginAction loginAction = new LoginAction(cookieManager);
            loginAction.setPageWrapper(pageWrapper);

            // 2. 获取二维码
            System.out.println("[4/5] 获取登录二维码...");
            String qrcodeUrl = loginAction.fetchQrcodeImage();

            if (qrcodeUrl == null) {
                System.out.println("无法获取二维码，请检查网络连接或页面结构是否变化");
                browserManager.closeBrowser(pageWrapper.getSessionId());
                return false;
            }

            System.out.println("二维码已获取，请在手机小红书 App 中扫描");
            System.out.println("提示：打开小红书 App -> 首页左上角扫一扫");

            // 3. 等待登录
            System.out.println("[5/5] 等待扫码登录（超时: " + timeoutSeconds + " 秒）...");
            System.out.println("请在 " + timeoutSeconds + " 秒内完成扫码...");

            boolean loginSuccess = loginAction.waitForLogin(timeoutSeconds * 1000L);

            if (loginSuccess) {
                System.out.println("\n检测到登录成功，正在保存 cookies...");

                // 4. 保存 cookies
                loginAction.saveCookies();

                System.out.println("Cookies 已保存到: " + cookieManager.getCookiesFilePath());
            } else {
                System.out.println("\n登录超时，请重新运行程序");
            }

            // 5. 关闭浏览器
            browserManager.closeBrowser(pageWrapper.getSessionId());
            System.out.println("浏览器已关闭");

            return loginSuccess;

        } catch (Exception e) {
            System.err.println("登录过程发生异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 打印欢迎信息
     */
    private static void printWelcome(int timeoutSeconds) {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║           小红书登录辅助工具 v1.0                              ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        System.out.println("║  功能：帮助登录小红书并保存 cookies                              ║");
        System.out.println("║  说明：登录一次后，后续测试会自动使用保存的 cookies              ║");
        System.out.println("║  超时：" + timeoutSeconds + " 秒                                               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * 打印成功信息
     */
    private static void printSuccess() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    ✓ 登录成功！                               ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        System.out.println("║  Cookies 已保存，下次运行测试时无需再次登录                      ║");
        System.out.println("║  如果 cookies 过期，请重新运行此工具进行登录                     ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
    }

    /**
     * 打印失败信息
     */
    private static void printFailure() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    ✗ 登录失败                                  ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        System.out.println("║  可能的原因：                                                 ║");
        System.out.println("║    - 扫码超时                                                  ║");
        System.out.println("║    - 网络连接问题                                              ║");
        System.out.println("║    - 页面结构变化                                              ║");
        System.out.println("║  请重新运行此工具尝试登录                                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
    }

    /**
     * 等待用户确认退出
     */
    private static void waitForExit() {
        System.out.println();
        System.out.println("按 Enter 键退出...");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        scanner.close();
    }
}