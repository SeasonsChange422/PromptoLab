package io.github.timemachinelab.core.session.application;

import io.github.timemachinelab.core.fingerprint.FingerprintService;
import io.github.timemachinelab.core.fingerprint.UserFingerprint;
import io.github.timemachinelab.core.session.application.SessionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * SSE验证服务
 * 封装指纹验证和SSE连接检查的通用逻辑
 * 
 * @author welsir
 * @date 2025/1/20
 */
@Slf4j
@Service
public class SseValidationService {
    
    @Resource
    private FingerprintService fingerprintService;
    
    @Resource
    private SseNotificationService sseNotificationService;
    
    /**
     * SSE验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String fingerprint;
        
        private ValidationResult(boolean valid, String errorMessage, String fingerprint) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.fingerprint = fingerprint;
        }
        
        public static ValidationResult success(String fingerprint) {
            return new ValidationResult(true, null, fingerprint);
        }
        
        public static ValidationResult error(String errorMessage) {
            return new ValidationResult(false, errorMessage, null);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public String getFingerprint() {
            return fingerprint;
        }
    }
    
    /**
     * 验证SSE连接并获取用户指纹
     * 如果验证失败，抛出业务异常
     * 
     * @param request HTTP请求对象
     * @return 用户指纹字符串
     * @throws SessionException 验证失败时抛出
     */
    public String validateAndGetFingerprint(HttpServletRequest request) throws SessionException {
        try {
            // 1. 获取已存在的用户指纹（不创建新指纹）
            UserFingerprint userFingerprint = fingerprintService.getExistingUserFingerprint(request);
            if (userFingerprint == null) {
                log.warn("用户指纹不存在，请先建立SSE连接");
                throw new SessionException("SSE连接验证失败: 用户指纹不存在，请先建立SSE连接");
            }
            
            String fingerprint = userFingerprint.getFingerprint();
            log.debug("用户指纹: {}, 访问次数: {}", fingerprint, userFingerprint.getVisitCount());
            
            // 2. 检查SSE连接是否存在
            if (!sseNotificationService.isConnectionExists(fingerprint)) {
                log.warn("SSE连接不存在 - 指纹: {}", fingerprint);
                throw new SessionException("SSE连接验证失败: SSE连接不存在，请先建立连接");
            }
            
            log.debug("SSE连接验证成功 - 指纹: {}", fingerprint);
            return fingerprint;
            
        } catch (SessionException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            log.error("SSE连接验证异常: {}", e.getMessage(), e);
            throw new SessionException("SSE连接验证失败: 系统异常，请重试");
        }
    }
    
    /**
     * 验证SSE连接和用户指纹
     * 
     * @param request HTTP请求对象
     * @return 验证结果
     * @deprecated 使用 validateAndGetFingerprint 方法替代
     */
    @Deprecated
    public ValidationResult validateSseConnection(HttpServletRequest request) {
        try {
            String fingerprint = validateAndGetFingerprint(request);
            return ValidationResult.success(fingerprint);
        } catch (SessionException e) {
            return ValidationResult.error(e.getMessage());
        }
    }
    
    /**
     * 发送SSE错误消息（如果连接存在）
     * 
     * @param request HTTP请求对象
     * @param errorMessage 错误消息
     */
    public void sendSseErrorIfConnected(HttpServletRequest request, String errorMessage) {
        try {
            UserFingerprint userFingerprint = fingerprintService.getExistingUserFingerprint(request);
            if (userFingerprint != null) {
                String fingerprint = userFingerprint.getFingerprint();
                
                if (sseNotificationService.isConnectionExists(fingerprint)) {
                    sseNotificationService.sendErrorMessage(fingerprint, errorMessage);
                }
            }
        } catch (Exception ex) {
            log.error("发送SSE错误消息失败: {}", ex.getMessage());
        }
    }
}