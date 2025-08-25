package io.github.timemachinelab.core.session.infrastructure.web.dto;

import io.github.timemachinelab.core.qatree.QaTree;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationHistoryResponse {

    private String sessionId;
    private String userId;
    private String currentNode;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private QaTree qaTree;
}