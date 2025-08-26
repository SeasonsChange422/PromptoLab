package io.github.timemachinelab.util;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.timemachinelab.core.qatree.QaTree;
import io.github.timemachinelab.core.qatree.QaTreeNode;
import io.github.timemachinelab.core.serializable.JsonNode;
import io.github.timemachinelab.core.question.*;
import io.github.timemachinelab.core.serializable.TempFormQuestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class QaTreeSerializeUtil {

    public static String serialize(QaTree t) throws JsonProcessingException {
        if (t == null || t.getRoot() == null) {
            return "[]";
        }

        List<Map<String, Object>> result = new ArrayList<>();

        firstOrderTraversalEnhanced(t.getRoot(), null, result);

        return JSONObject.toJSONString(result);
    }
    
    /**
     * 增强版遍历方法，返回SSE兼容的格式
     */
    private static void firstOrderTraversalEnhanced(QaTreeNode node, String parentId, List<Map<String, Object>> result) throws JsonProcessingException {
        if (node == null) {
            return;
        }
        
        // 获取子节点列表
        List<QaTreeNode> children = new ArrayList<>();
        if (node.getChildren() != null) {
            children.addAll(node.getChildren().values());
        }
        
        // 创建增强的节点数据
        Map<String, Object> enhancedNode = createEnhancedNode(node, parentId);
        result.add(enhancedNode);

        // 先序遍历
        for (QaTreeNode child : children) {
            firstOrderTraversalEnhanced(child, node.getId(), result);
        }
    }
    
    /**
     * 创建SSE兼容的增强节点数据
     */
    private static Map<String, Object> createEnhancedNode(QaTreeNode node, String parentId) {
        Map<String, Object> enhancedNode = new HashMap<>();
        enhancedNode.put("nodeId", node.getId());
        enhancedNode.put("parentId", parentId);
        
        String answer = "";
        Map<String, Object> questionData = null;
        
        BaseQuestion qa = node.getQa();
        if (qa != null) {
            // 根据问题类型创建questionData
            QuestionType type = QuestionType.fromString(qa.getType());
            switch (type) {
                case INPUT:
                    InputQuestion inputQA = (InputQuestion) qa;
                    questionData = createInputQuestionData(inputQA);
                    answer = inputQA.getAnswer() != null ? inputQA.getAnswer() : "";
                    break;
                case SINGLE:
                    SingleChoiceQuestion singleQA = (SingleChoiceQuestion) qa;
                    questionData = createSingleQuestionData(singleQA);
                    answer = formatSingleAnswer(singleQA);
                    break;
                case MULTI:
                    MultipleChoiceQuestion multiQA = (MultipleChoiceQuestion) qa;
                    questionData = createMultiQuestionData(multiQA);
                    answer = formatMultiAnswer(multiQA);
                    break;
                case FORM:
                    FormQuestion formQA = (FormQuestion) qa;
                    questionData = createFormQuestionData(formQA);
                    answer = formQA.getAnswer() != null ? JSONObject.toJSONString(formQA.getAnswer()) : "";
                    break;
                default:
                    // 普通文本问题
                    questionData = createTextQuestionData(qa.getQuestion());
                    break;
            }
        }
        
        enhancedNode.put("questionData", questionData);
        enhancedNode.put("answer", answer);
        
        return enhancedNode;
    }
    
    /**
      * 创建输入问题数据
      */
     private static Map<String, Object> createInputQuestionData(InputQuestion inputQA) {
         Map<String, Object> questionData = new HashMap<>();
         questionData.put("type", "input");
         questionData.put("question", inputQA.getQuestion() != null ? inputQA.getQuestion() : "");
         questionData.put("desc", inputQA.getDesc() != null ? inputQA.getDesc() : "");
         return questionData;
     }
     
     /**
      * 创建单选问题数据
      */
     private static Map<String, Object> createSingleQuestionData(SingleChoiceQuestion singleQA) {
         Map<String, Object> questionData = new HashMap<>();
         questionData.put("type", "single");
         questionData.put("question", singleQA.getQuestion() != null ? singleQA.getQuestion() : "");
         questionData.put("desc", singleQA.getDesc() != null ? singleQA.getDesc() : "");
         questionData.put("options", singleQA.getOptions() != null ? singleQA.getOptions() : new ArrayList<>());
         return questionData;
     }
     
     /**
      * 创建多选问题数据
      */
     private static Map<String, Object> createMultiQuestionData(MultipleChoiceQuestion multiQA) {
         Map<String, Object> questionData = new HashMap<>();
         questionData.put("type", "multi");
         questionData.put("question", multiQA.getQuestion() != null ? multiQA.getQuestion() : "");
         questionData.put("desc", multiQA.getDesc() != null ? multiQA.getDesc() : "");
         questionData.put("options", multiQA.getOptions() != null ? multiQA.getOptions() : new ArrayList<>());
         return questionData;
     }
     
     /**
      * 创建表单问题数据
      */
     private static Map<String, Object> createFormQuestionData(FormQuestion formQA) {
         Map<String, Object> questionData = new HashMap<>();
         questionData.put("type", "form");
         questionData.put("question", formQA.getQuestion() != null ? formQA.getQuestion() : "");
         questionData.put("desc", formQA.getDesc() != null ? formQA.getDesc() : "");
         questionData.put("fields", formQA.getFields() != null ? formQA.getFields() : new ArrayList<>());
         return questionData;
     }
     
     /**
      * 创建普通文本问题数据
      */
     private static Map<String, Object> createTextQuestionData(String question) {
         Map<String, Object> questionData = new HashMap<>();
         questionData.put("type", "text");
         questionData.put("question", question != null ? question : "");
         questionData.put("desc", "");
         return questionData;
     }
     
     /**
      * 格式化单选答案
      */
     private static String formatSingleAnswer(SingleChoiceQuestion singleQA) {
         if (singleQA.getAnswer() != null && !singleQA.getAnswer().isEmpty()) {
             List<String> answerLabels = new ArrayList<>();
             for (String answerId : singleQA.getAnswer()) {
                 String label = findOptionLabel(singleQA.getOptions(), answerId);
                 answerLabels.add(label != null ? label : answerId);
             }
             return String.join(",", answerLabels);
         }
         return "";
     }
     
     /**
      * 格式化多选答案
      */
     private static String formatMultiAnswer(MultipleChoiceQuestion multiQA) {
         if (multiQA.getAnswer() != null && !multiQA.getAnswer().isEmpty()) {
             List<String> answerLabels = new ArrayList<>();
             for (String answerId : multiQA.getAnswer()) {
                 String label = findOptionLabel(multiQA.getOptions(), answerId);
                 answerLabels.add(label != null ? label : answerId);
             }
             return String.join(",", answerLabels);
         }
         return "";
     }
     
     /**
      * 根据选项id查找对应的标签
      */
     private static String findOptionLabel(List<Option> options, String id) {
         if (options == null || id == null) {
             return null;
         }
         for (Option option : options) {
             if (id.equals(option.getId())) {
                 return option.getLabel();
             }
         }
         return null;
     }
     
     // 保留原有的序列化方法作为备用
     private static void firstOrderTraversal(QaTreeNode node, String parentId, List<JsonNode> result) throws JsonProcessingException {
         if (node == null) {
             return;
         }
         
         // 获取子节点列表
         List<QaTreeNode> children = new ArrayList<>();

         if (node.getChildren() != null) {
             children.addAll(node.getChildren().values());
         }
         
         // 访问当前节点
         JsonNode jsonNode = JsonNode.Convert2JsonNode(node, parentId);

         result.add(jsonNode);

         // 先序遍历
         for (QaTreeNode child : children) {
             firstOrderTraversal(child, node.getId(), result);
         }
     }
}
