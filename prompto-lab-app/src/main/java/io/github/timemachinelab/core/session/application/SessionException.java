package io.github.timemachinelab.core.session.application;

/**
 * 会话异常
 * 用于会话处理过程中的各种异常情况，通过错误描述区分不同类型：
 * - "会话处理失败": 会话处理过程中的验证失败
 * - "SSE连接验证失败": SSE连接验证异常
 */
public class SessionException extends Exception {
    
    public SessionException(String message) {
        super(message);
    }
    
    public SessionException(String message, Throwable cause) {
        super(message, cause);
    }
}