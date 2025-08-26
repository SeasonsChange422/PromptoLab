package io.github.timemachinelab.testnode.ainode;


import com.suifeng.sfchain.annotation.AIOp;
import com.suifeng.sfchain.core.BaseAIOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 描述: AI对话Demo节点
 * 专门用于演示一个简单的AI对话功能
 * 
 * @author suifeng
 * 日期: 2025/8/13
 */
@AIOp(value = "AI_CHAT_OP",
      description = "实现一个简单的AI对话功能示例",
      defaultModel = "gpt-5-chat")
@Component
@Slf4j
public class AIChatOperation extends BaseAIOperation<AIChatOperation.ChatRequest, AIChatOperation.ChatResponse> {

    /**
     * 构建对话提示
     */
    @Override
    public String buildPrompt(ChatRequest input) {
        return String.format("""
             描述：
                 请根据当前用户身份画像、规则和需求的明确程度，围绕着用户需求，选择最合适的交互方式来进一步挖掘或确认需求，当出现不符合用户身份画像的问题时，对对应问题中的关键词做出一定解释。你的目标是通过合适的形式挖掘出更多的信息帮助用户生成更清晰、可执行的AI提示词。请先分析当前上下文，判断用户需求处于哪个阶段，然后直接以选定的形式对应的输出格式进行返回，无需额外解释选择原因。
             触发规则：
           
             规则：
                 规则分为通用规则和条件触发规则，通用规则无需触发，是每次回答必须满足的规则；条件触发规则是由对应触发规则所触发。
                 通用规则：
                     TextQA: "如果已有部分信息但存在歧义或可深入的细节，结合当前用户身份画像，提出一个针对性问题，逐步细化用户的真实意图。"
                     SelectQA："如果用户已有初步意图但方向不明确，结合当前用户身份画像，提供2-4个典型选项带上对应选项（例如A:选项）供其选择，帮助其聚焦需求。"
                     FormQA："如果用户的需求尚不完整或较为模糊，使用此形式嵌套2-5个其他形式引导用户系统性地填写关键要素，以收集生成提示词所需的全部信息。"
                 条件触发规则：
            
            
                 触发规则：
                 
             ### 输入格式规范
             1、parentId 是回答的问题对应的id
            {
              "prentId": "父节点id",
              "answer": {
                "id1": "对应答案"
              }
            }
                                 
             输出格式：
                 1. 请准确回答用户的问题
                 2. 保持回答简洁、直接
                 3. 避免任何多余或无关的内容
                 4. 返回的答案必须是JSON格式
                 TextQA：
                 {
                   "type": "TextQA",
                   "data": {
                     "question": "问题"
                   }
                 }
                 SelectQA：
                 {
                   "type": "SelectQA",
                   "data": {
                     "question": "question",
                     "options": [
                       "option1",
                       "option2"
                     ]
                   }
                 }
                 FormQA：
                 {
                   "type": "FormQA",
                   "data": {
                     "formItems": [
                       {
                         "type": "TextQA || SelectQA",
                         "data": {}
                       }
                     ]
                   }
                 }
                """, input.getQuestion());
    }

    /**
     * 解析AI返回的JSON结果
     */
    @Override
    protected ChatResponse parseResult(String jsonContent, ChatRequest input) {
        try {
            // 使用通用的JSON解析工具转换为对象
            return objectMapper.readValue(jsonContent, ChatResponse.class);
        } catch (Exception e) {
            log.warn("解析AI对话结果失败，使用默认响应: {}", e.getMessage());
            ChatResponse response = new ChatResponse();
            response.setAnswer("抱歉，我暂时无法回答这个问题。");
            return response;
        }
    }

    /**
     * 对话请求数据结构
     */
    @Data
    public static class ChatRequest {
        /**
         * 用户提问内容
         */
        private String question;

        public ChatRequest() {
            this.question = "你好，AI！";
        }

        public ChatRequest(String question) {
            this.question = question;
        }
    }

    /**
     * 对话响应数据结构
     */
    @Data
    public static class ChatResponse {
        /**
         * AI的回答内容
         */
        private String answer;
    }
}