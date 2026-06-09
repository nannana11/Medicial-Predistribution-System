package com.triage.transport;

import com.triage.config.Config;
import com.triage.service.TriageService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty 服务端主类。
 * <p>
 * 负责绑定端口、启动服务、管理连接生命周期。
 * 使用行解码器 {@link LineBasedFrameDecoder} 处理 TCP 粘包/拆包，
 * 每条消息以换行符（\n）作为结束标记。
 * </p>
 *
 * <h3>网络架构：</h3>
 * <pre>
 * [分机1] ──→ ┐
 * [分机2] ──→ ┼──→ [NettyServer:8080] ──→ [TriageService] ──→ [AiClient]
 * [分机3] ──→ ┘                              ↓
 *                                    [ProtocolParser]
 * </pre>
 *
 * <h3>使用方式：</h3>
 * <pre>
 * NettyServer server = new NettyServer();
 * server.start();  // 启动服务（阻塞）
 * </pre>
 */
public class NettyServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private final Config config;
    private final TriageService triageService;

    /** Boss 线程组：负责接收新连接 */
    private EventLoopGroup bossGroup;
    /** Worker 线程组：负责处理已建立连接的 I/O 事件 */
    private EventLoopGroup workerGroup;

    public NettyServer() {
        this.config = Config.getInstance();
        this.triageService = new TriageService();
    }

    /**
     * 启动 Netty 服务端。
     * <p>
     * 此方法会阻塞当前线程，直到服务端关闭。
     * 建议在 {@link com.triage.MainApplication#main} 中调用。
     * </p>
     *
     * @throws InterruptedException 如果启动过程被中断
     */
    public void start() throws InterruptedException {
        int port = config.getServerPort();
        int workerThreads = config.getNettyWorkerThreads();

        logger.info("正在启动分诊系统 Netty 服务端，端口: {}, worker线程数: {}",
                port, workerThreads > 0 ? workerThreads : "自动");

        // 创建线程组
        // bossGroup: 处理 TCP 连接建立（通常1个线程即可）
        bossGroup = new NioEventLoopGroup(1);
        // workerGroup: 处理 I/O 读写（默认线程数为 CPU 核心数×2）
        workerGroup = workerThreads > 0
                ? new NioEventLoopGroup(workerThreads)
                : new NioEventLoopGroup();

        try {
            // 创建服务端启动引导器
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    // 使用 NIO 传输通道
                    .channel(NioServerSocketChannel.class)
                    // 打印网络事件日志
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // TCP 参数配置
                    .option(ChannelOption.SO_BACKLOG, 128)           // 连接请求等待队列大小
                    .option(ChannelOption.SO_REUSEADDR, true)        // 允许端口重用
                    .childOption(ChannelOption.SO_KEEPALIVE, true)   // 启用心跳保活
                    .childOption(ChannelOption.TCP_NODELAY, true)    // 禁用 Nagle 算法，减少延迟
                    // 配置 Channel 初始化器
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    // ============ 解码/编码器 ============

                                    // 1. 行解码器：以换行符 (\n) 作为消息结束标记，解决 TCP 粘包问题
                                    //    最大单行长度 8192 字节，超过则抛出异常
                                    new LineBasedFrameDecoder(8192),

                                    // 2. 字符串解码器：将 ByteBuf 转为 String
                                    new StringDecoder(CharsetUtil.UTF_8),

                                    // 3. 字符串编码器：将 String 转为 ByteBuf 发送
                                    new StringEncoder(CharsetUtil.UTF_8),

                                    // 4. 空闲检测：300 秒无读取则触发 IdleStateEvent
                                    //    参数：readerIdleTime, writerIdleTime, allIdleTime, unit
                                    new IdleStateHandler(300, 0, 0),

                                    // 5. 自定义业务处理器
                                    new TriageHandler(triageService)
                            );
                        }
                    });

            // 绑定端口并同步等待绑定完成
            ChannelFuture future = bootstrap.bind(port).sync();
            logger.info("分诊系统服务端启动成功，监听端口: {}", port);

            // 等待服务端关闭（阻塞当前线程）
            future.channel().closeFuture().sync();

        } finally {
            // 优雅关闭线程组
            shutdown();
        }
    }

    /**
     * 优雅关闭服务端。
     * <p>
     * 释放所有资源，包括 boss 和 worker 线程组。
     * 会等待已连接的分机处理完成后再关闭。
     * </p>
     */
    public void shutdown() {
        logger.info("正在关闭分诊系统服务端...");

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        logger.info("分诊系统服务端已关闭");
    }
}
