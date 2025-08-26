package io.github.timemachinelab.core.session.domain.entity;

import io.github.timemachinelab.core.qatree.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class ConversationSession {
    
    private final String sessionId;
    private final String userId;
    /**
     * -- SETTER --
     *  设置QaTree（仅用于初始化）
     *
     * @param qaTree QA树对象
     */
    @Setter
    private QaTree qaTree; // 移除final，允许后续设置
    private final LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 当前节点ID，用于表示当前question和answer处于哪个节点
    private String currentNode;
    
    // 用户信息
    private String user;
    // 用户目标
    private String userTarget;
    // AI模型
    private String aiModel;
    
    // 节点ID自增计数器，从1开始
    private final AtomicInteger nodeIdCounter = new AtomicInteger(0);
    
    public ConversationSession(String userId, String sessionId, QaTree qaTree) {
        this.qaTree = qaTree;
        this.sessionId = sessionId;
        this.userId = userId;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        // 设置默认值
        this.user = userId;
        this.userTarget = "通用对话";
        this.aiModel = "gpt-4-turbo";
    }
    
    /**
     * 获取下一个节点ID（自增）
     * @return 自增的节点ID字符串
     */
    public String getNextNodeId() {
        return String.valueOf(nodeIdCounter.incrementAndGet());
    }
}