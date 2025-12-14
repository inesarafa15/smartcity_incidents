package com.smartcity.incident_management.services.oauth2;

import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.RoleType;
import com.smartcity.incident_management.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Récupérer les informations de l'utilisateur depuis Google
        String userInfoUri = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUri();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userRequest.getAccessToken().getTokenValue());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    userInfoUri,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );
            
            Map<String, Object> attributes = response.getBody();
            
            if (attributes == null) {
                OAuth2Error oauth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Impossible de récupérer les informations utilisateur", null);
                throw new OAuth2AuthenticationException(oauth2Error);
            }
            
            String email = (String) attributes.get("email");
            String name = (String) attributes.get("name");
            
            if (email == null || email.isEmpty()) {
                OAuth2Error oauth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Email non disponible depuis Google", null);
                throw new OAuth2AuthenticationException(oauth2Error);
            }
            
            // Chercher ou créer l'utilisateur
            Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                    .orElseGet(() -> {
                        // Créer un nouveau citoyen
                        Utilisateur nouveau = new Utilisateur();
                        nouveau.setEmail(email);
                        
                        // Extraire prénom et nom depuis le nom complet
                        if (name != null && !name.isEmpty()) {
                            String[] parts = name.split(" ", 2);
                            nouveau.setPrenom(parts[0]);
                            nouveau.setNom(parts.length > 1 ? parts[1] : parts[0]);
                        } else {
                            nouveau.setPrenom("Utilisateur");
                            nouveau.setNom("Google");
                        }
                        
                        nouveau.setRole(RoleType.CITOYEN);
                        nouveau.setActif(true);
                        // Pas de mot de passe pour les utilisateurs OAuth2
                        nouveau.setMotDePasseHash("OAUTH2_USER");
                        
                        return utilisateurRepository.save(nouveau);
                    });
            
            // Vérifier que l'utilisateur est un citoyen (OAuth2 uniquement pour les citoyens)
            if (utilisateur.getRole() != RoleType.CITOYEN) {
                OAuth2Error oauth2Error = new OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED, "L'authentification Google est uniquement disponible pour les citoyens", null);
                throw new OAuth2AuthenticationException(oauth2Error);
            }
            
            // Mettre à jour les informations si nécessaire
            if (name != null && !name.isEmpty()) {
                String[] parts = name.split(" ", 2);
                utilisateur.setPrenom(parts[0]);
                utilisateur.setNom(parts.length > 1 ? parts[1] : parts[0]);
            }
            
            utilisateur.setActif(true);
            utilisateurRepository.save(utilisateur);
            
            // Créer un OAuth2UserPrincipal avec les attributs récupérés
            return new com.smartcity.incident_management.security.OAuth2UserPrincipal(utilisateur, attributes);
            
        } catch (OAuth2AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            OAuth2Error oauth2Error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, "Erreur lors de la récupération des informations utilisateur: " + e.getMessage(), null);
            throw new OAuth2AuthenticationException(oauth2Error, e);
        }
    }
}

