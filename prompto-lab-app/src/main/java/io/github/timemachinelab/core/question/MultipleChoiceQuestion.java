package io.github.timemachinelab.core.question;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 多选题类
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MultipleChoiceQuestion extends BaseQuestion {
    
    /**
     * 选项列表
     */
    private List<Option> options;
    
    /**
     * 问题答案
     */
    private List<String> answer;
    
    /**
     * 构造函数
     */
    public MultipleChoiceQuestion() {
        super(QuestionType.MULTI);
    }
}