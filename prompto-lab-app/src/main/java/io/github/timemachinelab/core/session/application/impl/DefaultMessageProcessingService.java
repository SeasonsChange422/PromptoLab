package io.github.timemachinelab.core.session.application.impl;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.github.timemachinelab.core.constant.AllPrompt;
import io.github.timemachinelab.core.qatree.QaTreeNode;
import io.github.timemachinelab.core.session.application.ConversationService;
import io.github.timemachinelab.core.qatree.QaTree;
import io.github.timemachinelab.core.qatree.QaTreeDomain;
import io.github.timemachinelab.core.qatree.QaTreeNode;
import io.github.timemachinelab.core.session.application.MessageProcessingService;
import io.github.timemachinelab.core.session.application.SessionManagementService;
import io.github.timemachinelab.core.session.application.SseNotificationService;
import io.github.timemachinelab.core.session.domain.entity.ConversationSession;
import io.github.timemachinelab.core.session.infrastructure.web.dto.UnifiedAnswerRequest;
import io.github.timemachinelab.util.QaTreeSerializeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 默认消息处理服务实现
 * 提供基础的消息处理逻辑，可根据需要进行扩展
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
@Service
@Slf4j
public class DefaultMessageProcessingService implements MessageProcessingService {

    @Resource
    SessionManagementService sessionManagementService;
    @Resource
    QaTreeDomain qaTreeDomain;
    @Resource
    ConversationService conversationService;
    @Resource
    SseNotificationService  sseNotificationService;

    /**
     * 更新qaTree中的答案
     */
    private void updateQaTreeWithAnswer(ConversationSession session, UnifiedAnswerRequest request) {
        try {
            QaTree qaTree = session.getQaTree();
            if (qaTree == null) {
                log.warn("会话的qaTree为空，无法更新答案 - 会话ID: {}", session.getSessionId());
                return;
            }
            
            String nodeId = session.getCurrentNode();
            
            // 根据问题类型准备答案数据
            Object answerData = prepareAnswerData(request);
            
            // 更新节点答案
             boolean updated = qaTreeDomain.updateNodeAnswer(qaTree, nodeId, answerData);
             if (updated) {
                 log.info("成功更新qaTree节点答案 - 会话ID: {}, 节点ID: {}", session.getSessionId(), nodeId);
                 // 会话数据已在内存中更新，无需额外保存操作
             } else {
                 log.warn("更新qaTree节点答案失败 - 会话ID: {}, 节点ID: {}", session.getSessionId(), nodeId);
             }
        } catch (Exception e) {
            log.error("更新qaTree答案时发生异常 - 会话ID: {}, 错误: {}", session.getSessionId(), e.getMessage(), e);
        }
    }
    
    /**
     * 根据问题类型准备答案数据
     */
    private Object prepareAnswerData(UnifiedAnswerRequest request) {
        switch (request.getQuestionType().toLowerCase()) {
            case "input":
                return request.getInputAnswer();
            case "single":
            case "multi":
                return request.getChoiceAnswer();
            case "form":
                return request.getFormAnswer();
            default:
                return request.getAnswerString();
        }
    }
    
    @Override
    public String preprocessMessage(String originalMessage, UnifiedAnswerRequest answerRequest,ConversationSession conversationSession) {
        try {

            updateQaTreeWithAnswer(conversationSession, answerRequest);

            JSONObject object = new JSONObject();
            object.put("set-user-profile", conversationSession.getUser());
            object.put("prompt",AllPrompt.GLOBAL_PROMPT);
            object.put("tree", QaTreeSerializeUtil.serialize(conversationSession.getQaTree()));
            object.put("input", answerRequest.getAnswerString());
            return object.toString();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("输入加工失败",e);
        }

    }
    
    @Override
    public boolean validateAnswer(UnifiedAnswerRequest request) {
        if (request == null) {
            return false;
        }
        
        if (request.getQuestionType() == null || request.getAnswer() == null) {
            return false;
        }
        
        // 根据问题类型验证答案格式
        switch (request.getQuestionType().toLowerCase()) {
            case "single":
            case "multi":
                return request.getChoiceAnswer() != null && !request.getChoiceAnswer().isEmpty();
                
            case "input":
                String input = request.getInputAnswer();
                return input != null && !input.trim().isEmpty();
                
            case "form":
                return request.getFormAnswer() != null && !request.getFormAnswer().isEmpty();
                
            default:
                log.warn("未知的问题类型: {}", request.getQuestionType());
                return false;
        }
    }
    
    @Override
    public String processRetryMessage(String sessionId, String nodeId, String whyRetry, ConversationSession conversationSession) {
        try {
            // 获取上一个问题内容
            String preQuestion = sessionManagementService.getNodeQuestion(sessionId, nodeId);
            if (preQuestion == null) {
                log.warn("无法获取节点问题内容 - sessionId: {}, nodeId: {}", sessionId, nodeId);
                preQuestion = "无法获取上一个问题内容";
            }
            
            // 构建重试请求的JSON格式
            JSONObject retryObject = new JSONObject();
            retryObject.put("action", "retry");
            retryObject.put("preQuestion", preQuestion);
            retryObject.put("whyRetry", whyRetry);
            
            // 构建完整的消息对象
            JSONObject messageObject = new JSONObject();
            messageObject.put("prompt", AllPrompt.GLOBAL_PROMPT);
            messageObject.put("tree", QaTreeSerializeUtil.serialize(conversationSession.getQaTree()));
            messageObject.put("input", retryObject.toString());

            log.info("处理重试消息 - sessionId: {}, nodeId: {}, 原因: {}", sessionId, nodeId, whyRetry);
            return messageObject.toString();
            
        } catch (JsonProcessingException e) {
            log.error("处理重试消息失败 - sessionId: {}, nodeId: {}, 错误: {}", sessionId, nodeId, e.getMessage(), e);
            throw new RuntimeException("重试消息处理失败", e);
        }
    }

    @Override
    public void processAndSendMessage(ConversationSession session, String processedMessage) {
        try {
            log.info("发送消息给AI服务 - sessionId: {}", session.getSessionId());

            // 调用ConversationService处理消息
            conversationService.processUserMessage(
                session.getUserId(),
                processedMessage,
                response -> {
                    log.info("AI服务响应成功 - sessionId: {}", session.getSessionId());

                    // 更新会话qaTree
                    QaTreeNode node = qaTreeDomain.appendNode(session.getQaTree(), response.getParentId(), response.getQuestion(), session);

                    // 更新会话当前节点
                    session.setCurrentNode(node.getId());
                    try {
                        // 通过SSE发送AI响应给前端
                        String fingerprint = session.getUserId();

                        // 构建要发送的响应数据，只包含用户指定的字段
                        JSONObject responseData = new JSONObject();
                        responseData.put("parentNodeId", response.getParentId());
                        responseData.put("currentNodeId", session.getCurrentNode());
                        responseData.put("sessionId", session.getSessionId());
                        responseData.put("updateTime", session.getUpdateTime());
                        responseData.put("question", response.getQuestion());

                        sseNotificationService
                                .sendSuccessMessage(fingerprint,responseData.toJSONString());
                        // 这里需要注入SseNotificationService
                        log.info("通过SSE发送AI响应 - 指纹: {}, 响应: {}", fingerprint, responseData);

                    } catch (Exception sseError) {
                        log.error("SSE消息发送失败 - sessionId: {}, 错误: {}", session.getSessionId(), sseError.getMessage(), sseError);
                    }
                }
            );

        } catch (Exception e) {
            log.error("发送消息给AI服务失败 - sessionId: {}, 错误: {}", session.getSessionId(), e.getMessage(), e);

            // 通过SSE发送错误消息给前端
            try {
                String fingerprint = session.getUserId();
                JSONObject errorData = new JSONObject();
                errorData.put("error", true);
                errorData.put("message", "AI服务调用失败: " + e.getMessage());
                errorData.put("sessionId", session.getSessionId());
                errorData.put("currentNodeId", session.getCurrentNode());

                sseNotificationService.sendErrorMessage(fingerprint, errorData.toJSONString());
                log.info("通过SSE发送错误消息 - 指纹: {}, 错误: {}", fingerprint, errorData);
            } catch (Exception sseError) {
                log.error("SSE错误消息发送失败 - sessionId: {}, 错误: {}", session.getSessionId(), sseError.getMessage(), sseError);
            }

        }
    }

}