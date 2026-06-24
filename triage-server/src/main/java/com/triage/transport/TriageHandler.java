package com.triage.transport;

import com.triage.service.TriageService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * 分诊消息处理器。
 * <p>
 * 继承 {@link ChannelInboundHandlerAdapter}，处理分机发送的消息。
 * 每条消息被视为一个完整的 JSON 请求（通过 {@link io.netty.handler.codec.LineBasedFrameDecoder}
 * 或 {@link io.netty.handler.codec.string.StringDecoder} 解码）。
 * </p>
 *
 * <h3>处理流程：</h3>
 * <ol>
 *   <li>接收分机发送的 JSON 字符串</li>
 *   <li>调用 {@link TriageService#processTriage(String)} 进行分诊处理</li>
 *   <li>将结果写回对应的分机（通过保存的 channel）</li>
 * </ol>
 *
 * <h3>注意事项：</h3>
 * <ul>
 *   <li>该类使用 {@code @Sharable} 注解，可在多个 Channel 间共享</li>
 *   <li>需要注意不要在该类中保存有状态数据</li>
 *   <li>空闲连接会自动关闭以节省资源</li>
 * </ul>
 */
@ChannelHandler.Sharable
public class TriageHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(TriageHandler.class);

    private final TriageService triageService;

    public TriageHandler(TriageService triageService) {
        this.triageService = triageService;
    }

    /**
     * 当收到分机消息时调用。
     * <p>
     * 接收消息后，将消息转为字符串，调用分诊服务处理，
     * 然后将结果写回分机。消息末尾自动追加换行符以便分机识别消息边界。
     * </p>
     *
     * @param ctx Channel 上下文
     * @param msg 收到的消息（由前面的解码器转为字符串）
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String clientIp = getClientIp(ctx);
        String rawMessage = msg instanceof String ? (String) msg : msg.toString();

        logger.info("✦ 收到来自分机 [{}] 的消息: {}", clientIp, rawMessage);

        // 调用业务层进行分诊处理
        String response = triageService.processTriage(rawMessage);

        logger.info("✦ 已向分机 [{}] 返回分诊结果", clientIp);

        // 将响应写回客户端，并添加换行符作为消息结束标记
        ByteBuf responseBuf = Unpooled.copiedBuffer(response + "\n", CharsetUtil.UTF_8);
        ctx.writeAndFlush(responseBuf)
                .addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        logger.error("向分机 [{}] 发送响应失败", clientIp, future.cause());
                    }
                });
    }

    /**
     * 读取完成后调用（Netty 中一般不强制使用，但需确保不遗漏 flush）。
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        // 已在 channelRead 中 flush，此处无需额外操作
    }

    /**
     * 当分机连接建立时调用。
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String clientIp = getClientIp(ctx);
        logger.info("╔═══════════════════════════════════════════╗");
        logger.info("║  ✦ 分机连接建立: {}        ", clientIp);
        logger.info("╚═══════════════════════════════════════════╝");
    }

    /**
     * 当分机断开连接时调用。
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String clientIp = getClientIp(ctx);
        logger.info("╔═══════════════════════════════════════════╗");
        logger.info("║  ✦ 分机连接断开: {}      ", clientIp);
        logger.info("╚═══════════════════════════════════════════╝");
    }

    /**
     * 处理 I/O 异常或业务处理中的异常。
     * <p>
     * 避免静默吞异常，确保异常被记录并通知客户端（如果连接仍存在）。
     * </p>
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String clientIp = getClientIp(ctx);
        logger.error("处理分机 [{}] 消息时发生异常", clientIp, cause);

        // 向客户端返回错误消息
        String errorMsg = "{\"code\":500,\"message\":\"服务器处理异常：" +
                cause.getMessage() + "\",\"data\":null,\"request_id\":\"\"}\n";
        ByteBuf errorBuf = Unpooled.copiedBuffer(errorMsg, CharsetUtil.UTF_8);

        ctx.writeAndFlush(errorBuf).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 处理空闲状态事件（读空闲、写空闲、读写空闲）。
     * <p>
     * 默认在指定时间无读取时关闭连接，释放资源。
     * </p>
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            String clientIp = getClientIp(ctx);
            IdleStateEvent event = (IdleStateEvent) evt;
            logger.warn("分机 [{}] 触发空闲事件: {}, 关闭连接", clientIp, event.state());
            ctx.close();
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    /**
     * 获取客户端的 IP 地址。
     */
    private String getClientIp(ChannelHandlerContext ctx) {
        if (ctx.channel().remoteAddress() instanceof InetSocketAddress addr) {
            return addr.getAddress().getHostAddress() + ":" + addr.getPort();
        }
        return "unknown";
    }
}
