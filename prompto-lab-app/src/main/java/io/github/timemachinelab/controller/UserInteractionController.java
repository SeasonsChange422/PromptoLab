package io.github.timemachinelab.controller;

import com.alibaba.fastjson2.JSON;
import io.github.timemachinelab.core.constant.AllPrompt;
import io.github.timemachinelab.core.session.application.MessageProcessingService;
import io.github.timemachinelab.core.session.application.SessionManagementService;
import io.github.timemachinelab.core.session.application.SseNotificationService;
import io.github.timemachinelab.core.session.domain.entity.ConversationSession;
import io.github.timemachinelab.core.session.infrastructure.ai.GenPromptOperation;
import io.github.timemachinelab.core.session.infrastructure.web.dto.GenPromptRequest;
import io.github.timemachinelab.core.session.infrastructure.web.dto.SetUserProfileRequest;
import io.github.timemachinelab.core.session.infrastructure.web.dto.UnifiedAnswerRequest;
import io.github.timemachinelab.entity.req.RetryRequest;
import io.github.timemachinelab.entity.resp.ApiResult;
import io.github.timemachinelab.entity.resp.RetryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户交互控制器
 * 提供用户交互相关的API接口
 * 
 * @author suifeng
 * @date 2025/1/20
 */
@Slf4j
@RestController
@RequestMapping("/api/user-interaction")
@Validated
public class UserInteractionController {
    @Resource
    private MessageProcessingService messageProcessingService;
    @Resource
    private SessionManagementService sessionManagementService;
    @Resource
    private SseNotificationService sseNotificationService;

    /**
     * 建立SSE连接
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamConversation(@RequestParam(required = false) String sessionId, 
                                        @RequestParam String userId) {
        log.info("建立SSE连接 - 会话ID: {}, 用户ID: {}", sessionId, userId);

        boolean isNewSession = false;
        ConversationSession session;
        
        try {
            if (sessionId == null || sessionId.isEmpty()) {
                // 新建会话
                session = sessionManagementService.createNewSession(userId);
                sessionId = session.getSessionId();
                isNewSession = true;
                log.info("创建新会话 - 用户ID: {}, 会话ID: {}", userId, sessionId);
            } else {
                // 使用现有会话
                session = sessionManagementService.validateAndGetSession(userId, sessionId);
                if (session == null) {
                    log.warn("会话不存在或无效 - 用户ID: {}, 会话ID: {}", userId, sessionId);
                    // 创建新会话作为fallback
                    session = sessionManagementService.createNewSession(userId != null ? userId : "anonymous_" + UUID.randomUUID().toString().substring(0, 8));
                    sessionId = session.getSessionId();
                    isNewSession = true;
                }
            }
            
            SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
            sseNotificationService.registerSseConnection(sessionId, emitter);
            
            // 连接建立时发送会话信息
            Map<String, Object> connectionData = new ConcurrentHashMap<>();
            connectionData.put("sessionId", sessionId);
            connectionData.put("userId", session.getUserId());
            connectionData.put("isNewSession", isNewSession);
            connectionData.put("timestamp", System.currentTimeMillis());
            
            // 根据会话状态返回nodeId
            if (isNewSession) {
                // 新会话返回根节点ID
                connectionData.put("nodeId", "1");
                log.info("新会话返回根节点ID: 1 - 会话: {}", sessionId);
            } else if (session.getQaTree() != null && session.getQaTree().getRoot() != null) {
                // 已存在会话，返回根节点ID（因为qaTree只有根节点）
                String rootNodeId = session.getQaTree().getRoot().getId();
                connectionData.put("nodeId", rootNodeId);
                log.info("已存在会话返回根节点ID: {} - 会话: {}", rootNodeId, sessionId);
                
                // 返回qaTree
                try {
                    String qaTreeJson = io.github.timemachinelab.util.QaTreeSerializeUtil.serialize(session.getQaTree());
                    connectionData.put("qaTree", qaTreeJson);
                } catch (Exception e) {
                    log.error("序列化qaTree失败: {}", e.getMessage());
                }
            } else {
                // 兜底情况，返回根节点ID
                connectionData.put("nodeId", "1");
                log.info("兜底返回根节点ID: 1 - 会话: {}", sessionId);
            }
            
            sseNotificationService.sendWelcomeMessage(sessionId, connectionData);
                 
             // 设置连接事件处理
             String finalSessionId = sessionId;
             emitter.onCompletion(() -> {
                 log.info("SSE连接完成: {}", finalSessionId);
             });

             emitter.onTimeout(() -> {
                 log.info("SSE连接超时: {}", finalSessionId);
                 sseNotificationService.removeSseConnection(finalSessionId);
             });
             
             emitter.onError((ex) -> {
                 log.error("SSE连接错误: {} - {}", finalSessionId, ex.getMessage());
                 sseNotificationService.removeSseConnection(finalSessionId);
             });
             
             return emitter;
                 
         } catch (Exception e) {
             log.error("建立SSE连接失败: {}", e.getMessage());
             SseEmitter errorEmitter = new SseEmitter(Long.MAX_VALUE);
             try {
                 errorEmitter.send(SseEmitter.event()
                     .name("error")
                     .data("连接建立失败: " + e.getMessage()));
             } catch (IOException ioException) {
                 log.error("发送错误消息失败: {}", ioException.getMessage());
             }
             return errorEmitter;
         }
    }

    /**
     * 重试接口
     * 
     * @param request 重试请求参数
     * @return 重试结果
     */
    @PostMapping("/retry")
    public ResponseEntity<ApiResult<RetryResponse>> retry(@Valid @RequestBody RetryRequest request) {
        try {
            log.info("收到重试请求 - nodeId: {}, sessionId: {}, whyretry: {}", 
                    request.getNodeId(), request.getSessionId(), request.getWhyretry());

            // 使用应用服务验证节点存在性
            //todo: 有可能水平越权 不传userId的话
            if (!sessionManagementService.validateNodeExists(request.getSessionId(), request.getNodeId())) {
                log.warn("节点不存在 - nodeId: {}, sessionId: {}", request.getNodeId(), request.getSessionId());
                return ResponseEntity.badRequest().body(ApiResult.error("指定的节点不存在"));
            }
            
            // 使用应用服务获取问题内容
            String question = sessionManagementService.getNodeQuestion(request.getSessionId(), request.getNodeId());
            if (question == null) {
                log.warn("节点问题内容为空 - nodeId: {}, sessionId: {}", request.getNodeId(), request.getSessionId());
                return ResponseEntity.badRequest().body(ApiResult.error("节点问题内容为空"));
            }

            // 获取会话对象
            ConversationSession session = sessionManagementService.getSessionById(request.getSessionId());
            if (session == null) {
                log.warn("会话不存在 - sessionId: {}", request.getSessionId());
                return ResponseEntity.badRequest().body(ApiResult.error("会话不存在"));
            }
            
            // 移除要重试的节点（AI会基于parentId重新创建节点）
            boolean nodeRemoved = sessionManagementService.removeNode(request.getSessionId(), request.getNodeId());
            if (!nodeRemoved) {
                log.warn("移除节点失败，但继续处理重试 - sessionId: {}, nodeId: {}", 
                        request.getSessionId(), request.getNodeId());
            }
            
            // 使用MessageProcessingService处理重试消息
            String processedMessage = messageProcessingService.processRetryMessage(
                    request.getSessionId(),
                    request.getNodeId(),
                    request.getWhyretry(),
                    session
            );
            
            // 发送处理后的消息给AI服务
            messageProcessingService.processAndSendMessage(session, processedMessage);
            
            // 构建响应数据
            RetryResponse response = RetryResponse.builder()
                    .nodeId(request.getNodeId())
                    .sessionId(request.getSessionId())
                    .whyretry(request.getWhyretry())
                    .processTime(System.currentTimeMillis())
                    .build();
            
            log.info("重试请求处理成功 - nodeId: {}, sessionId: {}", 
                    request.getNodeId(), request.getSessionId());
            
            return ResponseEntity.ok(ApiResult.success("重试请求处理成功", response));
            
        } catch (Exception e) {
            log.error("重试请求处理失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResult.serverError("重试请求处理失败: " + e.getMessage()));
        }
    }

    /**
     * 处理统一答案请求
     * 支持单选、多选、输入框、表单等多种问题类型的回答
     */
    @PostMapping("/message")
    public ResponseEntity<String> processAnswer(@Validated @RequestBody UnifiedAnswerRequest request) {
        try {
            log.info("接收到答案请求 - 会话ID: {}, 节点ID: {}, 问题类型: {}",
                    request.getSessionId(),
                    request.getNodeId(),
                    request.getQuestionType());

            // 2. 会话管理和验证
            String userId = request.getUserId();
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("缺少必需的userId参数");
                return ResponseEntity.badRequest().body("userId参数是必需的");
            }

            // 3. 验证会话是否存在
            ConversationSession session = sessionManagementService.validateAndGetSession(userId, request.getSessionId());
            if (session == null) {
                log.warn("会话不存在或无效 - 用户ID: {}, 会话ID: {}", userId, request.getSessionId());
                return ResponseEntity.badRequest().body("会话不存在或无效");
            }

            // 4. nodeId验证逻辑
            String nodeId = request.getNodeId();
            if (nodeId == null || nodeId.trim().isEmpty()) {
                // nodeId为空，表示这是新建会话的第一个问题
                if (session.getQaTree() != null && session.getQaTree().getRoot() != null) {
                    log.warn("会话已存在qaTree，但nodeId为空 - 会话: {}", session.getSessionId());
                    return ResponseEntity.badRequest().body("现有会话必须提供nodeId");
                }
                log.info("新建会话的第一个问题 - 会话: {}", session.getSessionId());
            } else if ("1".equals(nodeId)) {
                // nodeId为'1'，表示这是根节点的回答
                if (session.getQaTree() == null || session.getQaTree().getRoot() == null) {
                    log.info("根节点回答，但qaTree未初始化 - 会话: {}", session.getSessionId());
                    // 允许继续处理，后续会创建qaTree
                } else {
                    log.info("根节点回答 - 会话: {}", session.getSessionId());
                }
            } else {
                // nodeId不为空且不是'root'，验证是否属于该会话
                if (!sessionManagementService.validateNodeId(session.getSessionId(), nodeId)) {
                    log.warn("无效的节点ID - 会话: {}, 节点: {}", session.getSessionId(), nodeId);
                    return ResponseEntity.badRequest().body("无效的节点ID");
                }
                log.info("更新现有节点 - 会话: {}, 节点: {}", session.getSessionId(), nodeId);
            }

            // 3. 验证答案格式
            if (!messageProcessingService.validateAnswer(request)) {
                log.warn("答案格式验证失败: {}", request);
                return ResponseEntity.badRequest().body("答案格式不正确");
            }

            // 答案更新逻辑已在MessageProcessingService中处理
            messageProcessingService.processAnswer(request);
            // 4. 处理答案并转换为消息
            String processedMessage = messageProcessingService.preprocessMessage(
                    null, // 没有额外的原始消息
                    request,
                    session
            );

            // 5. 发送处理后的消息给AI服务
            messageProcessingService.processAndSendMessage(session, processedMessage);


            return ResponseEntity.ok("答案处理成功");

        } catch (Exception e) {
            log.error("处理答案失败 - 会话ID: {}, 错误: {}", request.getSessionId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("答案处理失败: " + e.getMessage());
        }
    }

    @PostMapping("/gen-prompt")
    public ResponseEntity<String>  genPrompt(@RequestBody GenPromptRequest request) {
        GenPromptOperation.GpResponse gpResponse = new GenPromptOperation.GpResponse();
        gpResponse.setGenPrompt(AllPrompt.GEN_PROMPT_AGENT_PROMPT);
        sseNotificationService.sendWelcomeMessage(request.getSessionId(), JSON.toJSONString(gpResponse));
        return ResponseEntity.ok("生成提示词");
    }

    
    /**
     * 获取SSE连接状态
     */
    @GetMapping("/sse-status")
    public ResponseEntity<Map<String, Object>> getSseStatus() {
        return ResponseEntity.ok(sseNotificationService.getSseStatus());
    }

    /*
    * 设置用户画像
    * */

    @PostMapping("/set-user-profile")
    public ResponseEntity<Boolean> setUserProfile(@RequestBody SetUserProfileRequest request) {
        return ResponseEntity.ok(sessionManagementService.setUserProfile(request.getSessionId(), request.getUserId(), request.getUserProfile()));
    }
}