package io.github.timemachinelab.core.qatree;

import io.github.timemachinelab.core.question.BaseQuestion;
import io.github.timemachinelab.core.question.InputQuestion;
import io.github.timemachinelab.core.question.SingleChoiceQuestion;
import io.github.timemachinelab.core.question.MultipleChoiceQuestion;
import io.github.timemachinelab.core.question.FormQuestion;
import io.github.timemachinelab.core.session.domain.entity.ConversationSession;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QaTreeDomain {

    public QaTree createTree(String userStartQuestion) {
        InputQuestion startQA = new InputQuestion();
        startQA.setQuestion(userStartQuestion);
        startQA.setAnswer(userStartQuestion);
        QaTreeNode startNode = new QaTreeNode(startQA);

        return new QaTree(startNode);
    }
    
    /**
     * 使用ConversationSession的自增ID创建QaTree
     * @param question 用户开始问题
     * @param session 会话对象，用于获取自增ID
     * @return 创建的QaTree
     */
    public QaTree createTree(String question, ConversationSession session) {
        InputQuestion startQA = new InputQuestion();
        startQA.setQuestion(question);
        // 使用会话的自增ID创建根节点
        String rootNodeId = session.getNextNodeId();
        QaTreeNode startNode = new QaTreeNode(startQA, rootNodeId);

        return new QaTree(startNode);
    }

    public QaTree appendNode(QaTree tree, String parentId, BaseQuestion qa) {
        tree.addNode(parentId, new QaTreeNode(qa));
        return tree;
    }
    
    /**
     * 使用ConversationSession的自增ID向QaTree添加节点
     * @param tree QA树
     * @param parentId 父节点ID
     * @param qa 问题对象
     * @param session 会话对象，用于获取自增ID
     * @return 更新后的QaTree
     */
    public QaTreeNode appendNode(QaTree tree, String parentId, BaseQuestion qa, ConversationSession session) {
        String nodeId = session.getNextNodeId();
        tree.addNode(parentId, new QaTreeNode(qa, nodeId));
        return tree.getNodeById(nodeId);
    }

    /**
     * 更新指定节点的答案
     * @param tree QA树
     * @param nodeId 节点ID
     * @param answer 新的答案内容
     * @return 是否更新成功
     */
    public boolean updateNodeAnswer(QaTree tree, String nodeId, Object answer) {
        QaTreeNode node = tree.getNodeById(nodeId);
        if (node == null) {
            return false;
        }
        
        BaseQuestion qa = node.getQa();
        if (qa == null) {
            return false;
        }
        
        // 根据问题类型设置答案
        if (qa instanceof InputQuestion) {
            ((InputQuestion) qa).setAnswer((String) answer);
        } else if (qa instanceof SingleChoiceQuestion) {
            ((SingleChoiceQuestion) qa).setAnswer((List<String>) answer);
        } else if (qa instanceof MultipleChoiceQuestion) {
            ((MultipleChoiceQuestion) qa).setAnswer((List<String>) answer);
        } else if (qa instanceof FormQuestion) {
            ((FormQuestion) qa).setAnswer((List<FormQuestion.AnswerItem>) answer);
        }
        
        return true;
    }
    
    /**
     * 获取指定节点的问题内容
     * @param tree QA树
     * @param nodeId 节点ID
     * @return 问题内容，如果节点不存在或问题为空则返回null
     */
    public String getNodeQuestion(QaTree tree, String nodeId) {
        if (tree == null || nodeId == null) {
            return null;
        }
        
        QaTreeNode node = tree.getNodeById(nodeId);
        if (node == null || node.getQa() == null) {
            return null;
        }
        
        return node.getQa().getQuestion();
    }
    
    /**
     * 验证节点是否存在
     * @param tree QA树
     * @param nodeId 节点ID
     * @return 节点是否存在
     */
    public boolean nodeExists(QaTree tree, String nodeId) {
        if (tree == null || nodeId == null) {
            return false;
        }
        
        return tree.getNodeById(nodeId) != null;
    }
    
    /**
     * 验证节点是否存在（别名方法）
     * @param tree QA树
     * @param nodeId 节点ID
     * @return 节点是否存在
     */
    public boolean containsNode(QaTree tree, String nodeId) {
        return nodeExists(tree, nodeId);
    }
    
    /**
     * 移除指定节点及其所有子节点
     * @param tree QA树
     * @param nodeId 要移除的节点ID
     * @return 是否移除成功
     */
    public boolean removeNode(QaTree tree, String nodeId) {
        if (tree == null || nodeId == null) {
            return false;
        }
        
        QaTreeNode nodeToRemove = tree.getNodeById(nodeId);
        if (nodeToRemove == null) {
            return false;
        }
        
        // 不能移除根节点
        if (tree.getRoot() != null && tree.getRoot().getId().equals(nodeId)) {
            return false;
        }
        
        // 从树中移除节点（包括从父节点的children中移除和从nodeMap中移除）
        return tree.removeNode(nodeId);
    }

    /**
     * 过滤QA树，返回只包含有答案的节点的新树
     * 如果节点没有答案，则在返回的树中过滤掉该节点
     * @param originalTree 原始QA树
     * @param currentNodeId 当前节点ID，该节点需要被过滤（因为没有对应的回答）
     * @return 过滤后的QA树副本
     */
    public QaTree filterQaTreeByAnswer(QaTree originalTree, String currentNodeId) {
        if (originalTree == null || originalTree.getRoot() == null) {
            return originalTree;
        }

        // 创建新的根节点副本
        QaTreeNode originalRoot = originalTree.getRoot();
        QaTreeNode qaNode = originalTree.getNodeById(currentNodeId);
        QaTreeNode newRoot = createNodeCopy(originalRoot, qaNode);
        if (newRoot == null) {
            return null; // 如果根节点被过滤，则返回空树
        }
        QaTree filteredTree = new QaTree(newRoot);

        // 递归过滤子节点
        filterAndCopyChildren(originalRoot, newRoot, filteredTree, qaNode);

        return filteredTree;
    }

    /**
     * 创建节点副本
     */
    private QaTreeNode createNodeCopy(QaTreeNode original, QaTreeNode qaNode) {
        if (original == null || original.getQa() == null || original.equals(qaNode)) {
            return null;
        }

        BaseQuestion originalQa = original.getQa();
        BaseQuestion newQa = null;

        // 根据问题类型创建副本
        if (originalQa instanceof InputQuestion) {
            InputQuestion inputQ = new InputQuestion();
            inputQ.setQuestion(originalQa.getQuestion());
            inputQ.setType(originalQa.getType());
            inputQ.setDesc(originalQa.getDesc());
            inputQ.setQuestionId(originalQa.getQuestionId());
            inputQ.setAnswer(((InputQuestion) originalQa).getAnswer());
            newQa = inputQ;
        } else if (originalQa instanceof SingleChoiceQuestion) {
            SingleChoiceQuestion singleQ = new SingleChoiceQuestion();
            singleQ.setQuestion(originalQa.getQuestion());
            singleQ.setType(originalQa.getType());
            singleQ.setDesc(originalQa.getDesc());
            singleQ.setQuestionId(originalQa.getQuestionId());
            singleQ.setOptions(((SingleChoiceQuestion) originalQa).getOptions());
            singleQ.setAnswer(((SingleChoiceQuestion) originalQa).getAnswer());
            newQa = singleQ;
        } else if (originalQa instanceof MultipleChoiceQuestion) {
            MultipleChoiceQuestion multiQ = new MultipleChoiceQuestion();
            multiQ.setQuestion(originalQa.getQuestion());
            multiQ.setType(originalQa.getType());
            multiQ.setDesc(originalQa.getDesc());
            multiQ.setQuestionId(originalQa.getQuestionId());
            multiQ.setOptions(((MultipleChoiceQuestion) originalQa).getOptions());
            multiQ.setAnswer(((MultipleChoiceQuestion) originalQa).getAnswer());
            newQa = multiQ;
        } else if (originalQa instanceof FormQuestion) {
            FormQuestion formQ = new FormQuestion();
            formQ.setQuestion(originalQa.getQuestion());
            formQ.setType(originalQa.getType());
            formQ.setDesc(originalQa.getDesc());
            formQ.setQuestionId(originalQa.getQuestionId());
            formQ.setFields(((FormQuestion) originalQa).getFields());
            formQ.setAnswer(((FormQuestion) originalQa).getAnswer());
            newQa = formQ;
        }

        return newQa != null ? new QaTreeNode(newQa, original.getId()) : null;
    }

    /**
     * 递归过滤并复制子节点
     */
    private void filterAndCopyChildren(QaTreeNode originalParent, QaTreeNode newParent, QaTree filteredTree, QaTreeNode qaNode) {
        if (originalParent == null || originalParent.getChildren() == null) {
            return;
        }

        for (QaTreeNode originalChild : originalParent.getChildren().values()) {
            // 如果是当前节点，跳过（过滤掉）
            if (qaNode != null && originalChild.equals(qaNode)) {
                continue;
            }

            // 检查节点是否有答案
            if (originalChild.getQa() != null && hasAnswer(originalChild)) {
                // 创建子节点副本
                QaTreeNode newChild = createNodeCopy(originalChild, qaNode);
                if (newChild != null) {
                    // 添加到新树中
                    filteredTree.addNode(newParent.getId(), newChild);
                    // 递归处理子节点的子节点
                    filterAndCopyChildren(originalChild, newChild, filteredTree, qaNode);
                }
            }
        }
    }

    /**
     * 检查节点是否有答案
     */
    private boolean hasAnswer(QaTreeNode node) {
        if (node == null || node.getQa() == null) {
            return false;
        }

        BaseQuestion qa = node.getQa();

        if (qa instanceof InputQuestion) {
            String answer = ((InputQuestion) qa).getAnswer();
            return answer != null && !answer.trim().isEmpty();
        } else if (qa instanceof SingleChoiceQuestion) {
            List<String> answer = ((SingleChoiceQuestion) qa).getAnswer();
            return answer != null && !answer.isEmpty();
        } else if (qa instanceof MultipleChoiceQuestion) {
            List<String> answer = ((MultipleChoiceQuestion) qa).getAnswer();
            return answer != null && !answer.isEmpty();
        } else if (qa instanceof FormQuestion) {
            List<FormQuestion.AnswerItem> answer = ((FormQuestion) qa).getAnswer();
            return answer != null && !answer.isEmpty();
        }

        return false;
    }
}
