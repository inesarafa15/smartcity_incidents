package com.smartcity.incident_management.security;

import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.RoleType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
    
    public static Utilisateur getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            if (authentication.getPrincipal() instanceof UserDetailsImpl) {
                return ((UserDetailsImpl) authentication.getPrincipal()).getUtilisateur();
            } else if (authentication.getPrincipal() instanceof OAuth2UserPrincipal) {
                return ((OAuth2UserPrincipal) authentication.getPrincipal()).getUtilisateur();
            }
        }
        throw new RuntimeException("Utilisateur non authentifié");
    }
    
    public static boolean hasRole(RoleType role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + role.name()));
        }
        return false;
    }
    
    public static boolean isAdmin() {
        return hasRole(RoleType.ADMINISTRATEUR) || hasRole(RoleType.SUPER_ADMIN);
    }
    
    public static boolean isAgent() {
        return hasRole(RoleType.AGENT_MUNICIPAL);
    }
    
    public static boolean isCitoyen() {
        return hasRole(RoleType.CITOYEN);
    }
}


