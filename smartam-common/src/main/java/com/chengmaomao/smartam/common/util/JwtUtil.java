package com.chengmaomao.smartam.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public final class JwtUtil {

    private JwtUtil() {}

    // 密钥（后续迁移到配置）
    private static final String SECRET = "smartam-jwt-secret-key-2026-min-256-bits!!";
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L; // 24小时

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
