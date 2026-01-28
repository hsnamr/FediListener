package com.activitypub.listener.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class ApiVersionInterceptor implements HandlerInterceptor {
    
    private static final String API_VERSION_HEADER = "API-Version";
    private static final String DEFAULT_VERSION = "v1";
    
    @Value("${api.version.header:API-Version}")
    private String versionHeader;
    
    @Value("${api.version.default:v1}")
    private String defaultVersion;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestedVersion = request.getHeader(versionHeader);
        
        if (requestedVersion == null || requestedVersion.isEmpty()) {
            // Set default version if not provided
            requestedVersion = defaultVersion;
            log.debug("No API version header provided, using default: {}", defaultVersion);
        }
        
        // Validate version format
        if (!isValidVersion(requestedVersion)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            try {
                response.getWriter().write(
                    "{\"error\":\"Invalid API version format. Use format 'v1', 'v2', etc.\",\"code\":\"INVALID_VERSION\"}"
                );
            } catch (Exception e) {
                log.error("Error writing error response", e);
            }
            return false;
        }
        
        // Check if version is supported
        if (!isVersionSupported(requestedVersion)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json");
            try {
                response.getWriter().write(
                    String.format("{\"error\":\"API version '%s' is not supported. Supported versions: v1\",\"code\":\"VERSION_NOT_SUPPORTED\"}", requestedVersion)
                );
            } catch (Exception e) {
                log.error("Error writing error response", e);
            }
            return false;
        }
        
        // Store version in request attribute for use in controllers
        request.setAttribute("apiVersion", requestedVersion);
        
        // Add version to response header
        response.setHeader(versionHeader, requestedVersion);
        
        log.debug("API version requested: {}", requestedVersion);
        return true;
    }
    
    private boolean isValidVersion(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        // Validate format: v1, v2, v1.0, etc.
        return version.matches("^v\\d+(\\.\\d+)?$");
    }
    
    private boolean isVersionSupported(String version) {
        // Currently only v1 is supported
        // Add more versions as they become available
        return "v1".equals(version);
    }
}
