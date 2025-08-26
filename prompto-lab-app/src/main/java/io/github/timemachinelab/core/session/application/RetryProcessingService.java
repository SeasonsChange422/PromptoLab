package io.github.timemachinelab.core.session.application;

import io.github.timemachinelab.core.session.domain.entity.ConversationSession;
import io.github.timemachinelab.core.session.application.SessionException;
import io.github.timemachinelab.entity.req.RetryRequest;
import io.github.timemachinelab.entity.resp.RetryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 重试处理服务
 * 封装重试请求的验证和处理逻辑
 * 
 * @author welsir
 * @date 2025/1/20
 */
@Slf4j
@Service
public class RetryProcessingService {
    
    @Resource
    private SessionManagementService sessionManagementService;
    
    @Resource
    private MessageProcessingService messageProcessingService;
    
    /**
     * 重试处理结果
     */
    public static class RetryProcessingResult {
        private final ConversationSession session;
        private final String processedMessage;
        
        public RetryProcessingResult(ConversationSession session, String processedMessage) {
            this.session = session;
            this.processedMessage = processedMessage;
        }
        
        public ConversationSession getSession() {
            return session;
        }
        
        public String getProcessedMessage() {
            return processedMessage;
        }
    }
    
    /**
     * 处理重试请求
     * 包含完整的验证和处理逻辑
     * 
     * @param fingerprint 用户指纹
     * @param request 重试请求
     * @return 重试处理结果
     * @throws SessionException 重试处理异常
     */
    public RetryProcessingResult processRetryRequest(String fingerprint, RetryRequest request) 
            throws SessionException {
        
        String sessionId = request.getSessionId();
        String whyRetry = request.getWhyretry();
        
        log.info("处理重试请求 - 指纹: {}, sessionId: {}, 原因: {}",
                fingerprint, sessionId, whyRetry);

        ConversationSession session = sessionManagementService.getSessionById(sessionId);
        if (session == null) {
            log.warn("会话不存在 - sessionId: {}", sessionId);
            throw new SessionException("SSE连接验证失败: 会话不存在");
        }
        String nodeId = session.getCurrentNode();
        // 1. 验证节点存在性
        if (!sessionManagementService.validateNodeExists(sessionId, nodeId)) {
            log.warn("节点不存在 - nodeId: {}, sessionId: {}", nodeId, sessionId);
            throw new SessionException("SSE连接验证失败: 指定的节点不存在");
        }
        
        // 2. 验证问题内容
        String question = sessionManagementService.getNodeQuestion(sessionId, nodeId);
        if (question == null) {
            log.warn("节点问题内容为空 - nodeId: {}, sessionId: {}", nodeId, sessionId);
            throw new SessionException("SSE连接验证失败: 节点问题内容为空");
        }

        // 4. 使用MessageProcessingService处理重试消息
        String processedMessage = messageProcessingService.processRetryMessage(
                sessionId,
                nodeId,
                whyRetry,
                session
        );

        // 5. 移除要重试的节点（AI会基于parentId重新创建节点）
        boolean nodeRemoved = sessionManagementService.removeNode(sessionId, nodeId);
        if (!nodeRemoved) {
            log.warn("移除节点失败 - sessionId: {}, nodeId: {}", sessionId, nodeId);
            throw new RuntimeException("移除节点失败");
        }
        
        log.info("重试请求处理完成 - sessionId: {}, nodeId: {}", sessionId, nodeId);
        return new RetryProcessingResult(session, processedMessage);
    }
    
    /**
     * 构建重试响应
     * 
     * @param request 重试请求
     * @return 重试响应
     */
    public RetryResponse buildRetryResponse(RetryRequest request) {
        return RetryResponse.builder()
                .nodeId(request.getNodeId())
                .sessionId(request.getSessionId())
                .whyretry(request.getWhyretry())
                .processTime(System.currentTimeMillis())
                .build();
    }
}