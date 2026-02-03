package com.activitypub.listener.config;

import com.activitypub.listener.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Security config: JWT authentication; permit actuator and public API. §5.1, §5.2
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${jwt.public-key:}")
    private String jwtPublicKey;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> {
                    e.authenticationEntryPoint(json401());
                    e.accessDeniedHandler(json403());
                })
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/**").permitAll();
                    auth.requestMatchers("/public-api/**").permitAll();
                    auth.requestMatchers("/error").permitAll();
                    if (jwtPublicKey != null && !jwtPublicKey.isBlank()) {
                        auth.anyRequest().authenticated();
                    } else {
                        auth.anyRequest().permitAll();
                    }
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private AuthenticationEntryPoint json401() {
        return (request, response, ex) -> writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "UNAUTHORIZED");
    }

    private AccessDeniedHandler json403() {
        return (request, response, ex) -> writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden", "FORBIDDEN");
    }

    private void writeJsonError(HttpServletResponse response, int status, String message, String code) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.<Void>builder().error(message).code(code).build();
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
