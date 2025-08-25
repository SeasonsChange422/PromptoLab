package io.github.timemachinelab.core.session.application;

import io.github.timemachinelab.core.session.domain.entity.ConversationSession;
import io.github.timemachinelab.core.session.infrastructure.web.dto.UnifiedAnswerRequest;
import io.github.timemachinelab.core.session.application.SessionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 会话处理服务
 * 封装新建/继续会话的判断逻辑和验证逻辑
 */
@Slf4j
@Service
public class SessionProcessingService {
    
    @Resource
    private SessionManagementService sessionManagementService;
    
    /**
     * 会话处理结果
     */
    public static class SessionProcessingResult {
        private final ConversationSession session;
        private final boolean isNewConversation;
        
        public SessionProcessingResult(ConversationSession session, boolean isNewConversation) {
            this.session = session;
            this.isNewConversation = isNewConversation;
        }
        
        public ConversationSession getSession() {
            return session;
        }
        
        public boolean isNewConversation() {
            return isNewConversation;
        }
    }
    
    /**
     * 处理会话请求，根据sessionId判断新建还是继续会话
     * 并进行相应的验证
     * 
     * @param fingerprint 用户指纹
     * @param request 统一答案请求
     * @return 会话处理结果
     * @throws SessionException 会话处理异常
     */
    public SessionProcessingResult processSessionRequest(String fingerprint, UnifiedAnswerRequest request) 
            throws SessionException {
        
        String sessionId = request.getSessionId();
        boolean isNewConversation = (sessionId == null || sessionId.trim().isEmpty());
        
        ConversationSession session;
        
        if (isNewConversation) {
            // 新建对话，不需要nodeId
            log.info("新建对话 - 用户指纹: {}", fingerprint);
            
            // 创建新会话
            session = sessionManagementService.createNewSession(fingerprint);
            sessionId = session.getSessionId();
            
            log.info("已创建新会话 - 用户指纹: {}, 会话ID: {}", fingerprint, sessionId);
            
        } else {
            // 继续现有对话，需要验证sessionId和nodeId
            log.info("继续现有对话 - 用户指纹: {}, 会话ID: {}", fingerprint, sessionId);
            
            // 验证会话是否存在
            session = sessionManagementService.validateAndGetSession(fingerprint, sessionId);
            if (session == null) {
                log.warn("会话不存在或无效 - 用户指纹: {}, 会话ID: {}", fingerprint, sessionId);
                throw new SessionException("会话处理失败: 会话不存在或无效");
            }
            
            // 验证nodeId
            String nodeId = session.getCurrentNode();
            if (nodeId == null || nodeId.trim().isEmpty()) {
                log.warn("继续会话必须提供nodeId - 会话ID: {}", sessionId);
                throw new SessionException("会话处理失败: 继续会话必须提供nodeId");
            }
            
            // 验证nodeId是否属于该会话
            if (!sessionManagementService.validateNodeId(sessionId, nodeId)) {
                log.warn("无效的节点ID - 会话: {}, 节点: {}", sessionId, nodeId);
                throw new SessionException("会话处理失败: 无效的节点ID");
            }
            
            log.info("验证通过 - 会话: {}, 节点: {}", sessionId, nodeId);
        }
        
        return new SessionProcessingResult(session, isNewConversation);
    }
}