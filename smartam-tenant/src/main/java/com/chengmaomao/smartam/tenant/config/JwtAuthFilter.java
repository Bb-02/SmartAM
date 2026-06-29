package com.chengmaomao.smartam.tenant.config;

import com.chengmaomao.smartam.common.security.JwtUser;
import com.chengmaomao.smartam.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ") || header.length() <= 7) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = JwtUtil.parse(header.substring(7));
            JwtUser user = new JwtUser(
                    claims.get("userId", Long.class),
                    claims.get("tenantId", Long.class),
                    claims.get("regionId", Long.class),
                    claims.get("deptId", Long.class),
                    claims.getSubject(),
                    claims.get("role", String.class));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"认证失败，请重新登录\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
