package com.smartcity.incident_management.security;

import com.smartcity.incident_management.entities.Utilisateur;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class OAuth2UserPrincipal implements OAuth2User, UserDetails {
    
    private final Utilisateur utilisateur;
    private final Map<String, Object> attributes;
    
    public OAuth2UserPrincipal(Utilisateur utilisateur, Map<String, Object> attributes) {
        this.utilisateur = utilisateur;
        this.attributes = attributes;
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name()));
    }
    
    @Override
    public String getName() {
        return utilisateur.getEmail();
    }
    
    @Override
    public String getPassword() {
        // Pour OAuth2, on retourne un hash spécial qui ne sera jamais vérifié
        return "{noop}OAUTH2_USER";
    }
    
    @Override
    public String getUsername() {
        return utilisateur.getEmail();
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return utilisateur.isActif();
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return utilisateur.isActif();
    }
    
    public Utilisateur getUtilisateur() {
        return utilisateur;
    }
}

