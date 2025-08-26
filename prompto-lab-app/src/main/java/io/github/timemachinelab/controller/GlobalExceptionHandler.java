package io.github.timemachinelab.controller;

import io.github.timemachinelab.core.session.application.SseNotificationService;
import io.github.timemachinelab.core.session.application.SseValidationService;
import io.github.timemachinelab.core.session.application.SessionException;
import io.github.timemachinelab.entity.resp.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 * 统一处理应用中的异常
 * 
 * @author welsir
 * @date 2025/1/20
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @Resource
    private SseValidationService sseValidationService;
    @Resource
    SseNotificationService sseNotificationService;
    /**
     * 处理SSE验证异常
     * 
     * @param e SSE验证异常
     * @param request HTTP请求对象
     * @return 错误响应
     */
    @ExceptionHandler(SessionException.class)
    public ResponseEntity<?> handleSessionException(SessionException e, HttpServletRequest request) {
        log.warn("SSE验证失败: {}", e.getMessage());
        
        // 尝试通过SSE发送错误消息
        sseValidationService.sendSseErrorIfConnected(request, e.getMessage());
        sseNotificationService.sendErrorMessage(null, e.getMessage());
        // 根据请求路径返回不同格式的响应
        String requestPath = request.getRequestURI();
        if (requestPath.contains("/retry")) {
            return ResponseEntity.badRequest().body(ApiResult.error(e.getMessage()));
        } else {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    

    
    /**
     * 处理通用异常
     * 
     * @param e 异常
     * @param request HTTP请求对象
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception e, HttpServletRequest request) {
        log.error("系统异常: {}", e.getMessage(), e);
        
        // 尝试通过SSE发送错误消息
        sseValidationService.sendSseErrorIfConnected(request, "系统异常，请重试");
        
        // 根据请求路径返回不同格式的响应
        String requestPath = request.getRequestURI();
        if (requestPath.contains("/retry")) {
            return ResponseEntity.internalServerError().body(ApiResult.error("系统异常: " + e.getMessage()));
        } else {
            return ResponseEntity.internalServerError().body("系统异常，请重试");
        }
    }
}