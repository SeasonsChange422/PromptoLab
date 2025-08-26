package io.github.timemachinelab.core.session.application;

import io.github.timemachinelab.core.qatree.QaTree;
import io.github.timemachinelab.core.qatree.QaTreeDomain;
import io.github.timemachinelab.core.session.domain.entity.ConversationSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 会话管理服务
 * 负责用户与会话的映射管理、nodeId验证等功能
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
@Service
@Slf4j
public class SessionManagementService {
    
    // 用户ID到会话ID列表的映射（一对多关系）
    private final Map<String, List<String>> userSessionMap = new ConcurrentHashMap<>();
    
    // 会话存储
    private final Map<String, ConversationSession> sessions = new ConcurrentHashMap<>();

    @Resource
    private QaTreeDomain qaTreeDomain;
    /**
     * 创建或获取用户会话
     * 如果sessionId为null，创建新会话；否则验证并返回现有会话
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID（可为null）
     * @return 会话对象
     */
    public ConversationSession getOrCreateSession(String userId, String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            // 创建新会话
            return createNewSession(userId);
        } else {
            // 验证现有会话
            return validateAndGetSession(userId, sessionId);
        }
    }
    
    /**
     * 验证nodeId是否属于指定会话
     * 
     * @param sessionId 会话ID
     * @param nodeId 节点ID
     * @return 是否有效
     */
    public boolean validateNodeId(String sessionId, String nodeId) {
        ConversationSession session = sessions.get(sessionId);
        if (session == null) {
            log.warn("会话不存在: {}", sessionId);
            return false;
        }
        
        // 检查节点是否存在于qaTree中
        boolean exists = session.getQaTree().getNodeById(nodeId) != null;
        if (!exists) {
            log.warn("节点不存在 - 会话: {}, 节点: {}", sessionId, nodeId);
        }
        
        return exists;
    }
    
    /**
     * 获取用户的所有会话
     * 
     * @param userId 用户ID
     * @return 会话列表
     */
    public List<ConversationSession> getUserSessions(String userId) {
        List<String> sessionIds = userSessionMap.get(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return new ArrayList<>();
        }
        return sessionIds.stream()
                .map(sessions::get)
                .filter(session -> session != null)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取用户最新的会话（最后创建的会话）
     * 
     * @param userId 用户ID
     * @return 最新的会话对象，如果没有会话则返回null
     */
    public ConversationSession getUserLatestSession(String userId) {
        List<String> sessionIds = userSessionMap.get(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return null;
        }
        // 返回列表中最后一个会话（最新创建的）
        String latestSessionId = sessionIds.get(sessionIds.size() - 1);
        return sessions.get(latestSessionId);
    }

    /**
     * 创建新会话
     */
    public ConversationSession createNewSession(String userId) {
        // 生成新的sessionId
        String newSessionId = UUID.randomUUID().toString();
        
        // 先创建会话对象（qaTree为null）
        ConversationSession session = new ConversationSession(userId, newSessionId, null);
        
        // 使用会话的自增ID创建QaTree，确保根节点ID=1
        QaTree tree = qaTreeDomain.createTree("你好，我有什么可以帮你？", session);
        
        // 设置QaTree到会话中
        session.setQaTree(tree);

        // 建立映射关系 - 添加到用户的会话列表中
        userSessionMap.computeIfAbsent(userId, k -> new ArrayList<>()).add(session.getSessionId());
        sessions.put(session.getSessionId(), session);

        log.info("创建新会话 - 用户: {}, 会话: {}, 根节点ID: 1", userId, session.getSessionId());
        return session;
    }
    
    /**
     * 验证并获取现有会话
     * 如果会话不存在或不属于该用户，返回null
     */
    public ConversationSession validateAndGetSession(String userId, String sessionId) {
        ConversationSession session = sessions.get(sessionId);
        
        if (session == null) {
            log.warn("会话不存在 - 用户: {}, 请求会话: {}", userId, sessionId);
            return null;
        }
        
        // 验证会话是否属于该用户
        if (!session.getUserId().equals(userId)) {
            log.warn("会话不属于该用户 - 用户: {}, 会话: {}, 会话所有者: {}", 
                    userId, sessionId, session.getUserId());
            return null;
        }
        
        // 确保用户会话映射中包含该会话ID（防止映射不一致）
        List<String> userSessions = userSessionMap.computeIfAbsent(userId, k -> new ArrayList<>());
        if (!userSessions.contains(sessionId)) {
            userSessions.add(sessionId);
        }
        
        log.debug("验证会话成功 - 用户: {}, 会话: {}", userId, sessionId);
        return session;
    }
    
    /**
     * 清理指定会话
     * 
     * @param sessionId 会话ID
     */
    public void removeSession(String sessionId) {
        ConversationSession session = sessions.remove(sessionId);
        if (session != null) {
            // 从用户会话列表中移除该会话ID
            List<String> userSessions = userSessionMap.get(session.getUserId());
            if (userSessions != null) {
                userSessions.remove(sessionId);
                // 如果用户没有其他会话了，移除整个映射
                if (userSessions.isEmpty()) {
                    userSessionMap.remove(session.getUserId());
                }
            }
            log.info("清理会话 - 用户: {}, 会话: {}", session.getUserId(), sessionId);
        }
    }
    
    /**
     * 清理用户的所有会话
     * 
     * @param userId 用户ID
     */
    public void removeAllUserSessions(String userId) {
        List<String> sessionIds = userSessionMap.remove(userId);
        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                sessions.remove(sessionId);
            }
            log.info("清理用户所有会话 - 用户: {}, 会话数量: {}", userId, sessionIds.size());
        }
    }
    
    /**
     * 根据会话ID获取会话对象
     * 
     * @param sessionId 会话ID
     * @return 会话对象，如果不存在则返回null
     */
    public ConversationSession getSessionById(String sessionId) {
        return sessions.get(sessionId);
    }
    
    /**
     * 检查用户是否拥有指定的会话
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 是否拥有该会话
     */
    public boolean userOwnsSession(String userId, String sessionId) {
        List<String> userSessions = userSessionMap.get(userId);
        return userSessions != null && userSessions.contains(sessionId);
    }
    
    /**
     * 获取指定会话中节点的问题内容
     * 
     * @param sessionId 会话ID
     * @param nodeId 节点ID
     * @return 问题内容，如果节点不存在或问题为空则返回null
     */
    public String getNodeQuestion(String sessionId, String nodeId) {
        ConversationSession session = sessions.get(sessionId);
        if (session == null) {
            log.warn("会话不存在: {}", sessionId);
            return null;
        }
        
        QaTree tree = session.getQaTree();
        return qaTreeDomain.getNodeQuestion(tree, nodeId);
    }
    
    /**
     * 验证指定会话中的节点是否存在
     * 
     * @param sessionId 会话ID
     * @param nodeId 节点ID
     * @return 节点是否存在
     */
    public boolean validateNodeExists(String sessionId, String nodeId) {
        ConversationSession session = sessions.get(sessionId);
        if (session == null) {
            log.warn("会话不存在: {}", sessionId);
            return false;
        }
        
        QaTree tree = session.getQaTree();
        return qaTreeDomain.nodeExists(tree, nodeId);
    }
    
    /**
     * 移除指定会话中的节点
     * 
     * @param sessionId 会话ID
     * @param nodeId 节点ID
     * @return 是否移除成功
     */
    public boolean removeNode(String sessionId, String nodeId) {
        ConversationSession session = sessions.get(sessionId);
        if (session == null) {
            log.warn("会话不存在: {}", sessionId);
            return false;
        }
        
        QaTree tree = session.getQaTree();
        if (tree == null) {
            log.warn("会话的QaTree不存在: {}", sessionId);
            return false;
        }
        
        boolean removed = qaTreeDomain.removeNode(tree, nodeId);
        if (removed) {
            log.info("成功移除节点 - 会话: {}, 节点: {}", sessionId, nodeId);
        } else {
            log.warn("移除节点失败 - 会话: {}, 节点: {}", sessionId, nodeId);
        }
        
        return removed;
    }
    
    /**
     * 获取会话统计信息
     */
    public Map<String, Object> getSessionStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalSessions", sessions.size());
        stats.put("activeUsers", userSessionMap.size());
        
        // 计算每个用户的会话数量分布
        Map<String, Integer> userSessionCounts = new ConcurrentHashMap<>();
        int totalUserSessions = 0;
        for (Map.Entry<String, List<String>> entry : userSessionMap.entrySet()) {
            int sessionCount = entry.getValue().size();
            userSessionCounts.put(entry.getKey(), sessionCount);
            totalUserSessions += sessionCount;
        }
        
        stats.put("userSessionCounts", userSessionCounts);
        stats.put("averageSessionsPerUser", userSessionMap.isEmpty() ? 0 : (double) totalUserSessions / userSessionMap.size());
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }

    public Boolean setUserProfile(String sessionId, String userId, String userProfile) {
        ConversationSession session = validateAndGetSession(userId, sessionId);
        if (session == null) {
            log.warn("会话不存在或无效, 用户ID: {} 会话ID: {}", userId, sessionId);
            return false;
        }

        session.setUser(userProfile);
        return true;
    }

    private QaTree createDefaultQaTree() {

        QaTree tree = qaTreeDomain.createTree("default");

        return tree;
    }
}