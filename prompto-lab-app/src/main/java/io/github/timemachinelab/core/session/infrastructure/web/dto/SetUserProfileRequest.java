package io.github.timemachinelab.core.session.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
public class SetUserProfileRequest {

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /*
    * 用户画像
    * */
    @NotBlank(message = "用户画像不能为空")
    private String userProfile;

    /*
    * 用户id
    * */
    @NotBlank(message = "用户ID不能为空")
    private String userId;
}
