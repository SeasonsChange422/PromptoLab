package io.github.timemachinelab.core.session.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一答案请求DTO
 * 用于接收前端传来的各种类型的问题回答
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedAnswerRequest {
    
    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;
    
    /**
     * 节点ID
     */
    private String nodeId;
    
    /**
     * 问题类型：single, multi, input, form
     */
    @NotBlank(message = "问题类型不能为空")
    private String questionType;
    
    /**
     * 原始答案数据
     * - 单选/多选：List<String>
     * - 输入框：String
     * - 表单：List<FormAnswerItem>
     */
    @NonNull
    private Object answer;
    
    /**
     * 额外的上下文信息
     */
    private Map<String, Object> context;

    /**
     * 用户ID
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;
    /**
     * 表单答案项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormAnswerItem {
        /**
         * 字段ID
         */
        private String id;
        
        /**
         * 字段值
         */
        private List<String> value;
    }
    
    /**
     * 获取单选/多选答案
     */
    @SuppressWarnings("unchecked")
    public List<String> getChoiceAnswer() {
        if (answer instanceof List) {
            return (List<String>) answer;
        }
        return null;
    }
    
    /**
     * 获取输入框答案
     */
    public String getInputAnswer() {
        if (answer instanceof String) {
            return (String) answer;
        }
        return null;
    }
    
    /**
     * 获取表单答案
     */
    @SuppressWarnings("unchecked")
    public List<FormAnswerItem> getFormAnswer() {
        if (answer instanceof List) {
            List<?> list = (List<?>) answer;
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                // 处理JSON反序列化的Map格式
                return list.stream()
                    .map(item -> {
                        if (item instanceof Map) {
                            Map<String, Object> map = (Map<String, Object>) item;
                            FormAnswerItem formItem = new FormAnswerItem();
                            formItem.setId((String) map.get("id"));
                            formItem.setValue((List<String>) map.get("value"));
                            return formItem;
                        }
                        return (FormAnswerItem) item;
                    })
                    .collect(java.util.stream.Collectors.toList());
            }
            return (List<FormAnswerItem>) answer;
        }
        return null;
    }
    
    /**
     * 根据问题类型获取对应的答案字符串
     * 
     * @return 答案字符串
     */
    public String getAnswerString() {
        if (questionType == null) {
            return "";
        }
        
        switch (questionType.toLowerCase()) {
            case "single":
                // 单选直接返回字符串（已经是id:content格式）
                String singleChoice = getInputAnswer();
                return singleChoice != null ? singleChoice : "";
                
            case "multi":
                // 多选返回逗号分隔的字符串（每个都是id:content格式）
                List<String> choices = getChoiceAnswer();
                return choices != null && !choices.isEmpty() ? String.join(", ", choices) : "";
                
            case "input":
                String input = getInputAnswer();
                return input != null ? input : "";
                
            case "form":
                List<FormAnswerItem> formItems = getFormAnswer();
                if (formItems != null && !formItems.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < formItems.size(); i++) {
                        FormAnswerItem item = formItems.get(i);
                        if (item.getValue() != null && !item.getValue().isEmpty()) {
                            sb.append(item.getId()).append(": ")
                              .append(String.join(", ", item.getValue()));
                            if (i < formItems.size() - 1) {
                                sb.append("; ");
                            }
                        }
                    }
                    return sb.toString();
                }
                return "";
                
            default:
                return "";
        }
    }
    
    /**
     * 转换为可读的文本格式
     */
    public String toReadableText() {
        StringBuilder sb = new StringBuilder();
        
        switch (questionType.toLowerCase()) {
            case "single":
                String singleChoice = getInputAnswer();
                if (singleChoice != null) {
                    sb.append("选择了：").append(singleChoice);
                }
                break;
                
            case "multi":
                List<String> choices = getChoiceAnswer();
                if (choices != null && !choices.isEmpty()) {
                    sb.append("选择了：").append(String.join(", ", choices));
                }
                break;
                
            case "input":
                String input = getInputAnswer();
                if (input != null) {
                    sb.append(input);
                }
                break;
                
            case "form":
                List<FormAnswerItem> formItems = getFormAnswer();
                if (formItems != null) {
                    for (FormAnswerItem item : formItems) {
                        sb.append(item.getId()).append(": ")
                          .append(String.join(", ", item.getValue()))
                          .append("; ");
                    }
                }
                break;
        }
        
        return sb.toString();
    }

    /**
     * 解析选项字符串，提取ID和内容
     * @param optionString 格式为"id:content"的字符串
     * @return 包含id和content的Map
     */
    public static Map<String, String> parseOptionString(String optionString) {
        Map<String, String> result = new HashMap<>();
        if (optionString != null && optionString.contains(":")) {
            String[] parts = optionString.split(":", 2);
            result.put("id", parts[0]);
            result.put("content", parts[1]);
        }
        return result;
    }

    /**
     * 获取选择题的所有选项ID
     * @return 选项ID列表
     */
    public List<String> getChoiceIds() {
        List<String> ids = new ArrayList<>();
        
        if ("single".equals(questionType)) {
            String singleChoice = getInputAnswer();
            if (singleChoice != null) {
                Map<String, String> parsed = parseOptionString(singleChoice);
                if (!parsed.isEmpty()) {
                    ids.add(parsed.get("id"));
                }
            }
        } else if ("multi".equals(questionType)) {
            List<String> choices = getChoiceAnswer();
            if (choices != null) {
                for (String choice : choices) {
                    Map<String, String> parsed = parseOptionString(choice);
                    if (!parsed.isEmpty()) {
                        ids.add(parsed.get("id"));
                    }
                }
            }
        }
        
        return ids;
    }

    /**
     * 获取选择题的所有选项内容
     * @return 选项内容列表
     */
    public List<String> getChoiceContents() {
        List<String> contents = new ArrayList<>();
        
        if ("single".equals(questionType)) {
            String singleChoice = getInputAnswer();
            if (singleChoice != null) {
                Map<String, String> parsed = parseOptionString(singleChoice);
                if (!parsed.isEmpty()) {
                    contents.add(parsed.get("content"));
                }
            }
        } else if ("multi".equals(questionType)) {
            List<String> choices = getChoiceAnswer();
            if (choices != null) {
                for (String choice : choices) {
                    Map<String, String> parsed = parseOptionString(choice);
                    if (!parsed.isEmpty()) {
                        contents.add(parsed.get("content"));
                    }
                }
            }
        }
        
        return contents;
    }
}