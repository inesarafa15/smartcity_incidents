package com.smartcity.incident_management.controllers;

import com.smartcity.incident_management.security.OAuth2UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/oauth2")
public class OAuth2Controller {
    
    @GetMapping("/callback")
    public String callback() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2UserPrincipal) {
            // L'authentification OAuth2 est déjà gérée par OAuth2AuthenticationSuccessHandler
            // Rediriger vers le dashboard qui gérera la redirection selon le rôle
            return "redirect:/dashboard";
        }
        
        // En cas d'erreur, rediriger vers la page de connexion
        return "redirect:/connexion?error=oauth2";
    }
}

