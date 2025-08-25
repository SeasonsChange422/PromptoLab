package io.github.timemachinelab.core.fingerprint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户指纹服务
 * 用于生成和管理用户指纹信息
 * 
 * @author welsir
 * @date 2025/1/20
 */
@Slf4j
@Service
public class FingerprintService {
    
    /**
     * 用户指纹缓存
     */
    private final Map<String, UserFingerprint> fingerprintCache = new ConcurrentHashMap<>();
    
    /**
     * 获取或创建用户指纹
     * 
     * @param request HTTP请求对象
     * @return 用户指纹信息
     */
    public UserFingerprint getOrCreateUserFingerprint(HttpServletRequest request) {
        try {
            // 生成指纹ID
            String fingerprintId = generateFingerprint(request);
            
            // 检查缓存中是否已存在
            UserFingerprint existingFingerprint = fingerprintCache.get(fingerprintId);
            
            if (existingFingerprint != null) {
                // 更新访问信息
                existingFingerprint.setVisitCount(existingFingerprint.getVisitCount() + 1);
                existingFingerprint.setLastVisitTime(LocalDateTime.now());
                
                log.debug("用户指纹已存在: {}, 访问次数: {}", fingerprintId, existingFingerprint.getVisitCount());
                return existingFingerprint;
            }
            
            // 创建新的用户指纹
            UserFingerprint newFingerprint = UserFingerprint.builder()
                    .fingerprint(fingerprintId)
                    .displayName(generateDisplayName())
                    .visitCount(1)
                    .firstVisitTime(LocalDateTime.now())
                    .lastVisitTime(LocalDateTime.now())
                    .ipAddress(getClientIpAddress(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .build();
            
            // 存入缓存
            fingerprintCache.put(fingerprintId, newFingerprint);
            
            log.info("创建新用户指纹: {}, 显示名称: {}", fingerprintId, newFingerprint.getDisplayName());
            return newFingerprint;
            
        } catch (Exception e) {
            log.error("生成用户指纹失败: {}", e.getMessage(), e);
            // 返回默认指纹
            return createDefaultFingerprint();
        }
    }
    
    /**
     * 生成用户指纹ID
     * 
     * @param request HTTP请求对象
     * @return 指纹ID
     */
    private String generateFingerprint(HttpServletRequest request) {
        try {
            StringBuilder fingerprintData = new StringBuilder();
            
            // 获取客户端IP
            String clientIp = getClientIpAddress(request);
            fingerprintData.append(clientIp);
            
            // 获取User-Agent
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                fingerprintData.append(userAgent);
            }
            
            // 获取Accept-Language
            String acceptLanguage = request.getHeader("Accept-Language");
            if (acceptLanguage != null) {
                fingerprintData.append(acceptLanguage);
            }
            
            // 使用MD5生成指纹
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(fingerprintData.toString().getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5算法不可用，使用UUID作为指纹: {}", e.getMessage());
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
    
    /**
     * 获取客户端真实IP地址
     * 
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };
        
        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 多个IP时取第一个
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 生成用户显示名称
     * 
     * @return 显示名称
     */
    private String generateDisplayName() {
        String[] adjectives = {"聪明的", "勇敢的", "友善的", "创新的", "睿智的", "活跃的", "优雅的", "坚强的"};
        String[] nouns = {"探索者", "创造者", "思考者", "学习者", "建设者", "梦想家", "实践者", "先锋"};
        
        int adjIndex = (int) (Math.random() * adjectives.length);
        int nounIndex = (int) (Math.random() * nouns.length);
        
        return adjectives[adjIndex] + nouns[nounIndex];
    }
    
    /**
     * 创建默认指纹（当生成失败时使用）
     * 
     * @return 默认用户指纹
     */
    private UserFingerprint createDefaultFingerprint() {
        String defaultId = UUID.randomUUID().toString().replace("-", "");
        
        return UserFingerprint.builder()
                .fingerprint(defaultId)
                .displayName("匿名用户")
                .visitCount(1)
                .firstVisitTime(LocalDateTime.now())
                .lastVisitTime(LocalDateTime.now())
                .ipAddress("unknown")
                .userAgent("unknown")
                .build();
    }
    
    /**
     * 获取已存在的用户指纹（不创建新指纹）
     * 
     * @param request HTTP请求对象
     * @return 用户指纹信息，如果不存在则返回null
     */
    public UserFingerprint getExistingUserFingerprint(HttpServletRequest request) {
        try {
            // 生成指纹ID
            String fingerprintId = generateFingerprint(request);
            
            // 只从缓存中获取，不创建新的
            UserFingerprint existingFingerprint = fingerprintCache.get(fingerprintId);
            
            if (existingFingerprint != null) {
                // 更新访问信息
                existingFingerprint.setVisitCount(existingFingerprint.getVisitCount() + 1);
                existingFingerprint.setLastVisitTime(LocalDateTime.now());
                
                log.debug("获取已存在用户指纹: {}, 访问次数: {}", fingerprintId, existingFingerprint.getVisitCount());
                return existingFingerprint;
            }
            
            log.debug("用户指纹不存在: {}", fingerprintId);
            return null;
            
        } catch (Exception e) {
            log.error("获取用户指纹失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取指纹缓存统计信息
     * 
     * @return 缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalFingerprints", fingerprintCache.size());
        stats.put("cacheSize", fingerprintCache.size());
        return stats;
    }
    
    /**
     * 清理过期的指纹缓存
     * 可以定期调用此方法清理长时间未访问的指纹
     */
    public void cleanupExpiredFingerprints() {
        LocalDateTime expireTime = LocalDateTime.now().minusHours(24); // 24小时未访问则清理
        
        fingerprintCache.entrySet().removeIf(entry -> {
            UserFingerprint fingerprint = entry.getValue();
            return fingerprint.getLastVisitTime().isBefore(expireTime);
        });
        
        log.info("清理过期指纹缓存完成，当前缓存大小: {}", fingerprintCache.size());
    }
}