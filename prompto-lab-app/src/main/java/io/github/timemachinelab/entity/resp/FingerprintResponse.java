package io.github.timemachinelab.entity.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户指纹响应DTO
 * 
 * @author suifeng
 * @date 2025/1/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FingerprintResponse {
    
    /**
     * 用户指纹
     */
    private String fingerprint;
    
    /**
     * 是否为新用户
     */
    private Boolean isNewUser;
    
    /**
     * 是否为返回用户
     */
    private Boolean isReturningUser;
    
    /**
     * 用户显示名称
     */
    private String displayName;
    
    /**
     * 访问次数
     */
    private Integer visitCount;
    
    /**
     * 首次访问时间戳
     */
    private Long firstSeen;
    
    /**
     * 最后访问时间戳
     */
    private Long lastSeen;
    
    /**
     * 生成时间戳
     */
    private Long timestamp;
}