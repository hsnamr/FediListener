package com.activitypub.listener.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Validates JWT from Authorization header and sets SecurityContext with user id. §5.1
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.public-key:}")
    private String jwtPublicKey;

    @Value("${jwt.issuer:AMRITECH}")
    private String jwtIssuer;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && StringUtils.hasText(jwtPublicKey)) {
            try {
                SecretKey key = Keys.hmacShaKeyFor(jwtPublicKey.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .requireIssuer(jwtIssuer)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String subject = claims.getSubject();
                Long userId = subject != null ? parseUserId(subject) : null;
                if (userId != null) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                log.debug("Invalid or expired JWT: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        String alt = request.getHeader("luc-authorization");
        if (StringUtils.hasText(alt) && alt.startsWith("Bearer ")) {
            return alt.substring(7);
        }
        return StringUtils.hasText(alt) ? alt : null;
    }

    private static Long parseUserId(String subject) {
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
