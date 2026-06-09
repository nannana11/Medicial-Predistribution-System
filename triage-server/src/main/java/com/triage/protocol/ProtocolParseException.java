package com.triage.protocol;

/**
 * 协议解析异常。
 * <p>
 * 当解析分机发送的 JSON 数据失败时抛出，携带 HTTP 风格错误码和业务错误码。
 * </p>
 */
public class ProtocolParseException extends Exception {

    /** HTTP 风格状态码（400, 500 等） */
    private final int httpCode;

    /** 业务错误码 */
    private final String errorCode;

    public ProtocolParseException(String message, int httpCode, String errorCode) {
        super(message);
        this.httpCode = httpCode;
        this.errorCode = errorCode;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
