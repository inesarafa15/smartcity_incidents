package com.smartcity.incident_management.security;

import com.smartcity.incident_management.entities.Utilisateur;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserDetailsImpl implements UserDetails {
    
    private final Utilisateur utilisateur;
    
    public UserDetailsImpl(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name()));
    }
    
    @Override
    public String getPassword() {
        return utilisateur.getMotDePasseHash();
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


