package com.triage.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 配置管理类。
 * <p>
 * 负责加载 application.properties 配置文件，提供端口号、API Key 等配置项的访问。
 * 支持通过系统属性覆盖配置文件中的值（系统属性优先级更高）。
 * </p>
 */
public class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    /** 配置文件路径（classpath 根目录） */
    private static final String CONFIG_FILE = "application.properties";

    /** 单例实例 */
    private static Config instance;

    private final Properties properties;

    /**
     * 私有构造方法，从配置文件加载属性。
     */
    private Config() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                logger.warn("未找到配置文件 {}，将使用默认配置", CONFIG_FILE);
                return;
            }
            properties.load(input);
            logger.info("成功加载配置文件: {}", CONFIG_FILE);
        } catch (IOException e) {
            logger.error("加载配置文件失败: {}", CONFIG_FILE, e);
        }
    }

    /**
     * 获取配置单例。
     *
     * @return Config 实例（线程安全）
     */
    public static synchronized Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    /**
     * 获取 Netty 服务器端口号。
     * <p>
     * 优先读取系统属性 {@code triage.server.port}，若未设置则从配置文件中读取。
     * 默认值：8080
     * </p>
     *
     * @return 端口号
     */
    public int getServerPort() {
        return getIntProperty("triage.server.port", 8080);
    }

    /**
     * 获取 DeepSeek API Key。
     * <p>
     * 优先读取系统属性 {@code triage.deepseek.api.key}，若未设置则从配置文件中读取。
     * 可通过 -Dtriage.deepseek.api.key=xxx 在启动时传入。
     * </p>
     *
     * @return API Key（可能为空）
     */
    public String getDeepSeekApiKey() {
        String environmentValue = System.getenv("API_KEY");
        if (environmentValue == null || environmentValue.isBlank()) {
            environmentValue = System.getenv("API_KEY");
        }
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue.trim();
        }
        return getStringProperty("triage.deepseek.api.key", "").trim();
    }

    /**
     * 获取 DeepSeek API 的基础 URL。
     *
     * @return API 基础 URL
     */
    public String getDeepSeekApiUrl() {
        return getStringProperty("triage.deepseek.api.url",
                "https://api.deepseek.com/v1/chat/completions");
    }

    /**
     * 获取 DeepSeek 模型名称。
     *
     * @return 模型名称
     */
    public String getDeepSeekModel() {
        return getStringProperty("triage.deepseek.model", "deepseek-chat");
    }

    /**
     * 获取 AI 超时时间（毫秒）。
     *
     * @return 超时时间（毫秒）
     */
    public int getAiTimeoutMs() {
        return getIntProperty("triage.ai.timeout.ms", 30000);
    }

    /**
     * 获取 Netty 工作线程数。
     *
     * @return 线程数
     */
    public int getNettyWorkerThreads() {
        return getIntProperty("triage.netty.worker.threads", 0);
    }

    /**
     * 读取字符串类型配置项，支持系统属性覆盖。
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public String getStringProperty(String key, String defaultValue) {
        // 系统属性优先级最高
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isEmpty()) {
            return sysProp;
        }
        return properties.getProperty(key, defaultValue);
    }

    /**
     * 读取整数类型配置项，支持系统属性覆盖。
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public int getIntProperty(String key, int defaultValue) {
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isEmpty()) {
            try {
                return Integer.parseInt(sysProp);
            } catch (NumberFormatException e) {
                logger.warn("系统属性 {} 的值 \"{}\" 不是有效整数，使用默认值 {}", key, sysProp, defaultValue);
            }
        }
        String propValue = properties.getProperty(key);
        if (propValue != null && !propValue.isEmpty()) {
            try {
                return Integer.parseInt(propValue);
            } catch (NumberFormatException e) {
                logger.warn("配置项 {} 的值 \"{}\" 不是有效整数，使用默认值 {}", key, propValue, defaultValue);
            }
        }
        return defaultValue;
    }
    public boolean isMockMode(){
        return Boolean.parseBoolean(getStringProperty("triage.ai.mock","true"));
    }
}
