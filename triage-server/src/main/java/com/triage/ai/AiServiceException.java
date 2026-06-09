package com.triage.ai;

/**
 * AI 服务异常类。
 * <p>
 * 封装 AI 调用过程中的各类异常（网络超时、API 不可用、解析错误等），
 * 携带错误码以便上层进行针对性处理。
 * </p>
 */
public class AiServiceException extends RuntimeException {

    /** 错误码 */
    private final String errorCode;

    /**
     * 构造异常。
     *
     * @param message 错误描述
     * @param errorCode 错误码
     */
    public AiServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造异常（含原始原因）。
     *
     * @param message 错误描述
     * @param errorCode 错误码
     * @param cause 原始异常
     */
    public AiServiceException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码。
     *
     * @return 错误码，例如 TIMEOUT、API_ERROR、PARSE_ERROR
     */
    public String getErrorCode() {
        return errorCode;
    }

    /** 超时错误码 */
    public static final String CODE_TIMEOUT = "TIMEOUT";
    /** API 调用错误码 */
    public static final String CODE_API_ERROR = "API_ERROR";
    /** 响应解析错误码 */
    public static final String CODE_PARSE_ERROR = "PARSE_ERROR";
    /** 参数校验错误码 */
    public static final String CODE_INVALID_INPUT = "INVALID_INPUT";
}
