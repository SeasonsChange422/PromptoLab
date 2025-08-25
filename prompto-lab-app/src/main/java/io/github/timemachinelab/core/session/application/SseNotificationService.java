package io.github.timemachinelab.core.session.application;

import io.github.timemachinelab.core.qatree.QaTree;
import io.github.timemachinelab.core.qatree.QaTreeDomain;
import io.github.timemachinelab.core.session.domain.entity.ConversationSession;
import io.github.timemachinelab.core.session.infrastructure.ai.QuestionGenerationOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE通知服务
 * 负责管理SSE连接和发送消息给客户端
 *
 * @author suifeng
 * 日期: 2025/1/27
 */
@Service
@Slf4j
public class SseNotificationService {

    @Resource
    private SessionManagementService sessionManagementService;
    @Resource
    private QaTreeDomain qaTreeDomain;

    // SSE连接管理 - 基于用户指纹的一对一关系
    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    /**
     * 注册SSE连接
     *
     * @param fingerprint 用户指纹
     * @param emitter SSE发射器
     */
    public void registerSseConnection(String fingerprint, SseEmitter emitter) {
        // 如果用户已有连接，先移除旧连接
        SseEmitter oldEmitter = sseEmitters.get(fingerprint);
        if (oldEmitter != null) {
            oldEmitter.complete(); // 完成旧连接
        }

        sseEmitters.put(fingerprint, emitter);
        log.info("SSE连接已注册 - 用户指纹: {}", fingerprint);
        
        // 添加连接完成和超时的回调
        emitter.onCompletion(() -> {
            sseEmitters.remove(fingerprint);
            log.info("SSE连接已完成 - 用户指纹: {}", fingerprint);
        });
        
        emitter.onTimeout(() -> {
            sseEmitters.remove(fingerprint);
            log.info("SSE连接已超时 - 用户指纹: {}", fingerprint);
        });
    }

    /**
     * 移除SSE连接
     *
     * @param fingerprint 用户指纹
     */
    public void removeSseConnection(String fingerprint) {
        SseEmitter emitter = sseEmitters.remove(fingerprint);
        if (emitter != null) {
            emitter.complete();
        }
        log.info("SSE连接已移除 - 用户指纹: {}", fingerprint);
    }

    /**
     * 检查SSE连接是否存在
     *
     * @param fingerprint 用户指纹
     * @return 连接是否存在
     */
    public boolean isConnectionExists(String fingerprint) {
        return sseEmitters.containsKey(fingerprint);
    }

    /**
     * 获取SSE连接状态
     *
     * @return 连接状态信息
     */
    public Map<String, Object> getSseStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("connectedFingerprints", sseEmitters.keySet()); // 改为显示指纹列表
        status.put("totalConnections", sseEmitters.size());
        status.put("timestamp", System.currentTimeMillis());
        return status;
    }

    /**
     * 通用SSE消息发送方法
     *
     * @param fingerprint 用户指纹
     * @param eventName 事件名称
     * @param data 消息数据
     */
    private void sendSseEvent(String fingerprint, String eventName, Object data) {
        SseEmitter emitter = sseEmitters.get(fingerprint);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
                log.info("SSE事件发送成功 - 用户指纹: {}, 事件: {}", fingerprint, eventName);
            } catch (IOException e) {
                log.warn("SSE事件发送失败 - 用户指纹: {}, 事件: {}, 错误: {}", fingerprint, eventName, e.getMessage());
                // 移除失效的连接
                sseEmitters.remove(fingerprint);
            } catch (IllegalStateException e) {
                log.warn("SSE连接已关闭 - 用户指纹: {}, 事件: {}, 错误: {}", fingerprint, eventName, e.getMessage());
                // 移除已关闭的连接
                sseEmitters.remove(fingerprint);
            }
        } else {
            log.warn("SSE连接不存在 - 用户指纹: {}, 事件: {}", fingerprint, eventName);
        }
    }


    /**
     * 发送连接数据
     *
     * @param fingerprint 用户指纹
     * @param connectionData 连接数据
     */
    public void sendWelcomeMessage(String fingerprint, Map<String, Object> connectionData) {
        sendSseEvent(fingerprint, "connected", connectionData);
    }
    
    /**
     * 发送错误消息给客户端
     * 确保错误时能够立即响应，不会卡住
     * 
     * @param fingerprint 用户指纹
     */
    public void sendErrorMessage(String fingerprint, String msg) {
        sendMessage(fingerprint,msg,"500",false);
    }

    private void sendMessage(String fingerprint, String msg,String code,boolean success) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", success);
        errorResponse.put("code", code);
        errorResponse.put("data", msg);
        errorResponse.put("timestamp", System.currentTimeMillis());

        sendSseEvent(fingerprint, success?"success":"error", errorResponse);
        log.info("消息已发送 - 用户指纹: {}, 代码: {}, 消息: {}", fingerprint, code, msg);
    }

    public void sendSuccessMessage(String fingerprint,String msg) {
        sendMessage(fingerprint,msg,"200",true);
    }
}