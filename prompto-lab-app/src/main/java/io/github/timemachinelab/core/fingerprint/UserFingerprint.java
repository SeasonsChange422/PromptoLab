package io.github.timemachinelab.core.fingerprint;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 用户指纹信息
 * 用于标识和跟踪用户会话
 * 
 * @author welsir
 * @date 2025/1/20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFingerprint {
    
    /**
     * 用户指纹ID
     */
    private String fingerprint;
    
    /**
     * 用户显示名称
     */
    private String displayName;
    
    /**
     * 访问次数
     */
    private Integer visitCount;
    
    /**
     * 首次访问时间
     */
    private LocalDateTime firstVisitTime;
    
    /**
     * 最后访问时间
     */
    private LocalDateTime lastVisitTime;
    
    /**
     * 用户IP地址
     */
    private String ipAddress;
    
    /**
     * 用户代理信息
     */
    private String userAgent;
    
    /**
     * 是否为新用户
     */
    public boolean isNewUser() {
        return visitCount != null && visitCount == 1;
    }
}