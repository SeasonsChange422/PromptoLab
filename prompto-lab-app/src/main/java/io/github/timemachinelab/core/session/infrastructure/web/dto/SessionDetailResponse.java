package io.github.timemachinelab.core.session.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionDetailResponse {

    private String sessionId;
    private LocalDateTime lastNodeCreateTime;
    private String lastNodeQuestion;
    private LocalDateTime updateTime;
}