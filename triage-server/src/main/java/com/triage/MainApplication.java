package com.triage;

import com.triage.transport.HttpTriageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 医院分诊系统 - 中央主机启动类。
 * <p>
 * 包含 main 方法，启动 HTTP 服务端，等待客户端请求并处理分诊。
 * </p>
 *
 * <h3>启动方式：</h3>
 * <ol>
 *   <li>直接运行此类的 main 方法（IDE 中）</li>
 *   <li>Maven 打包后运行 jar 文件</li>
 * </ol>
 *
 * <h3>启动参数（可选）：</h3>
 * <ul>
 *   <li>{@code -Dtriage.server.port=9090} 自定义端口号</li>
 *   <li>{@code -Dtriage.deepseek.api.key=sk-xxx} 传入 API Key</li>
 * </ul>
 *
 * <h3>配置说明：</h3>
 * 默认配置在 {@code application.properties} 中，也可通过系统属性覆盖。
 *
 * <h3>分机客户端连接示例（Telnet）：</h3>
 * <pre>
 * telnet 127.0.0.1 8080
 * POST http://localhost:8080/api/triage/message
 * </pre>
 */
public class MainApplication {

    private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);

    public static void main(String[] args) {

        logger.info("========================================");
        logger.info("  医院分诊系统 - 中央主机 启动中...");
        logger.info("========================================");

        // 打印配置概要
        com.triage.config.Config config = com.triage.config.Config.getInstance();
        logger.info("服务端口: {}", config.getServerPort());
        /*logger.info("AI 模式: {}",
                System.getProperty("triage.ai.mock", "true").equals("false")
                        ? "真实 DeepSeek API" : "模拟模式 (Mock)");*/
        logger.info("AI模式：{}",config.isMockMode()?"Mock":"Practice");
        logger.info("AI 超时: {}ms", config.getAiTimeoutMs());

        HttpTriageServer server = new HttpTriageServer();

        // 注册 JVM 关闭钩子，确保服务优雅停止
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("收到关闭信号，正在停止服务...");
            server.shutdown();
            logger.info("服务已停止");
        }));

        try {
            server.start();
        } catch (InterruptedException e) {
            logger.error("服务端运行被中断", e);
            Thread.currentThread().interrupt();
            server.shutdown();
        } catch (Exception e) {
            logger.error("服务端运行异常", e);
            server.shutdown();
            System.exit(1);
        }
    }
}
