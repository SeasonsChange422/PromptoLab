package io.github.timemachinelab.core.qatree;

import io.github.timemachinelab.core.question.BaseQuestion;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class QaTreeNode {

    private final String id;
    private final LocalDateTime createTime;

    private Map<String, QaTreeNode> children;

    private BaseQuestion qa;

    public QaTreeNode(BaseQuestion qa) {
        this.id = UUID.randomUUID().toString();
        this.createTime = LocalDateTime.now();
        this.children = new HashMap<>();
        this.qa = qa;
    }
    
    /**
     * 使用指定ID创建节点的构造函数
     * @param qa 问题对象
     * @param nodeId 指定的节点ID
     */
    public QaTreeNode(BaseQuestion qa, String nodeId) {
        this.id = nodeId;
        this.createTime = LocalDateTime.now();
        this.children = new HashMap<>();
        this.qa = qa;
    }

    /**
     * @deprecated 此方法使用UUID生成节点ID，不符合自增ID规范。
     * 请使用 QaTreeDomain.appendNode(tree, parentId, qa, session) 方法代替。
     */
    @Deprecated
    public void append(BaseQuestion qa) {
        QaTreeNode node = new QaTreeNode(qa);
        this.append(node);
    }

    public void append(QaTreeNode node) {
        children.put(node.getId(), node);
    }

    public QaTreeNode getChild(String id) {
        return children.get(id);
    }

    public boolean hasChild(String id) {
        return children.containsKey(id);
    }

    public void removeChild(String id) {
        children.remove(id);
    }

}