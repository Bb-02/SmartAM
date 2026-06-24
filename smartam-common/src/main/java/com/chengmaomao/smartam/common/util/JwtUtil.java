package com.chengmaomao.smartam.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public final class JwtUtil {

    private static String SECRET;
    private static long EXPIRATION_MS;

    public JwtUtil(@Value("${jwt.secret:smartam-jwt-secret-key-2026-min-256-bits!!}") String secret,
                   @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        JwtUtil.SECRET = secret;
        JwtUtil.EXPIRATION_MS = expirationMs;
    }

    private static SecretKey getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /** 签发 Token */
    public static String generate(Long userId, Long tenantId, Long regionId, Long deptId,
                                  String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .claims(Map.of("userId", userId, "tenantId", tenantId,
                        "regionId", regionId != null ? regionId : 0,
                        "deptId", deptId != null ? deptId : 0))
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + EXPIRATION_MS))
                .signWith(getKey())
                .compact();
    }

    /** 解析 Token */
    public static Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
