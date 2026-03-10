package com.admin.security;

import com.admin.common.RedisConstants;
import com.admin.utils.RedisUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类（集成 Redis）
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 生成 token
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", userDetails.getUsername());
        String token = createToken(claims, userDetails.getUsername());
        
        // 将 token 存入 Redis
        String key = RedisConstants.TOKEN_PREFIX + userDetails.getUsername();
        redisUtil.set(key, token, RedisConstants.TOKEN_EXPIRE_TIME);
        
        return token;
    }

    /**
     * 创建 token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * 从 token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * 从 token 中获取 Claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证 token 是否有效
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = getUsernameFromToken(token);
            
            // 检查 token 是否在黑名单中
            String blacklistKey = RedisConstants.TOKEN_BLACKLIST_PREFIX + token;
            if (redisUtil.hasKey(blacklistKey)) {
                return false;
            }
            
            // 检查 Redis 中的 token 是否匹配
            String redisKey = RedisConstants.TOKEN_PREFIX + username;
            Object redisToken = redisUtil.get(redisKey);
            
            return username.equals(userDetails.getUsername()) 
                    && !isTokenExpired(token)
                    && token.equals(redisToken);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断 token 是否过期
     */
    private boolean isTokenExpired(String token) {
        Date expiration = getClaimsFromToken(token).getExpiration();
        return expiration.before(new Date());
    }

    /**
     * 刷新 token
     */
    public String refreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String username = claims.getSubject();
            
            // 生成新 token
            String newToken = createToken(new HashMap<>(claims), username);
            
            // 更新 Redis
            String key = RedisConstants.TOKEN_PREFIX + username;
            redisUtil.set(key, newToken, RedisConstants.TOKEN_EXPIRE_TIME);
            
            // 将旧 token 加入黑名单
            String blacklistKey = RedisConstants.TOKEN_BLACKLIST_PREFIX + token;
            long remainingTime = getClaimsFromToken(token).getExpiration().getTime() - System.currentTimeMillis();
            if (remainingTime > 0) {
                redisUtil.set(blacklistKey, "1", remainingTime / 1000);
            }
            
            return newToken;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 删除 token（登出）
     */
    public void deleteToken(String username, String token) {
        // 从 Redis 删除 token
        String key = RedisConstants.TOKEN_PREFIX + username;
        redisUtil.del(key);
        
        // 将 token 加入黑名单
        String blacklistKey = RedisConstants.TOKEN_BLACKLIST_PREFIX + token;
        try {
            long remainingTime = getClaimsFromToken(token).getExpiration().getTime() - System.currentTimeMillis();
            if (remainingTime > 0) {
                redisUtil.set(blacklistKey, "1", remainingTime / 1000);
            }
        } catch (Exception e) {
            // token 已过期，无需加入黑名单
        }
    }
}
