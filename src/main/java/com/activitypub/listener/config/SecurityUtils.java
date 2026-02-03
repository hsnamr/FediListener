package com.activitypub.listener.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helper to get current user id from SecurityContext (set by JWT filter). §5.2
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Current user id from JWT (SecurityContext), or null if not authenticated.
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
