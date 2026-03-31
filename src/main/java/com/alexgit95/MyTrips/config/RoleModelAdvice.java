package com.alexgit95.MyTrips.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expose les rôles de l'utilisateur connecté dans tous les modèles Thymeleaf.
 */
@ControllerAdvice
public class RoleModelAdvice {

    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication auth) {
        return hasRole(auth, "ROLE_ADMIN");
    }

    @ModelAttribute("isReporter")
    public boolean isReporter(Authentication auth) {
        return hasRole(auth, "ROLE_REPORTER");
    }

    @ModelAttribute("isGuest")
    public boolean isGuest(Authentication auth) {
        return hasRole(auth, "ROLE_GUEST");
    }

    @ModelAttribute("canEditPlanner")
    public boolean canEditPlanner(Authentication auth) {
        return hasRole(auth, "ROLE_ADMIN") || hasRole(auth, "ROLE_REPORTER");
    }

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(role));
    }
}
