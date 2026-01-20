package com.taskqueue.www.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<CustomUserDetails> currentUserOptional() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();

        if (!(principal instanceof CustomUserDetails)) {
            return Optional.empty();
        }

        return Optional.of((CustomUserDetails) principal);
    }

    public static Long currentUserId() {
        return currentUserOptional()
                .map(CustomUserDetails::getId)
                .orElseThrow(() -> new AccessDeniedException("Unauthenticated"));
    }

    public static boolean isAdmin() {
        return currentUserOptional()
                .map(u -> u.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))
                .orElse(false);
    }
}
