package com.capstone.rentit.login.provider;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final StringRedisTemplate redis;

    private final Key key;
    private final long jwtExpirationInMs;

    private final Duration refreshTtl;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration,
            @Value("${jwt.refresh-ttl-min}") long refreshTtlMin,
            StringRedisTemplate redis) {

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationInMs = expiration;
        this.refreshTtl = Duration.ofMinutes(refreshTtlMin);
        this.redis = redis;
    }

    public String generateToken(Authentication authentication) {

        return buildToken(authentication.getName(), jwtExpirationInMs);
    }

    public String generateRefreshToken(Authentication authentication) {
        String token = buildToken(authentication.getName(), refreshTtl.toMillis());

        // key = "refresh:{token}", value = username, TTL = refreshTtl
        redis.opsForValue().set(refreshKey(token), authentication.getName(), refreshTtl);
        return token;
    }

    private String buildToken(String subject, long ttlMs) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ttlMs))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /** ── 리프레시 토큰 검증 (Redis 존재 여부 + JWT 구조 검증) ─── */
    public boolean validateRefreshToken(String token) {
        if (!validateToken(token)) return false;
        return redis.hasKey(refreshKey(token));
    }

    public String getUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public void revokeRefreshToken(String token) {
        redis.delete(refreshKey(token));
    }

    private String refreshKey(String token) { return "refresh:" + token; }
}
