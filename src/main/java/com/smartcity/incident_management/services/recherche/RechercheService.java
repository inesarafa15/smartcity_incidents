package com.smartcity.incident_management.services.recherche;

import com.smartcity.incident_management.dto.IncidentFiltreDTO;
import com.smartcity.incident_management.dto.UtilisateurFiltreDTO;
import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.PrioriteIncident;
import com.smartcity.incident_management.enums.StatutIncident;
import com.smartcity.incident_management.repository.IncidentRepository;
import com.smartcity.incident_management.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class RechercheService {
    
    @Autowired
    private IncidentRepository incidentRepository;
    
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    
    /**
     * Recherche avancée d'incidents avec filtres
     */
    public Page<Incident> rechercherIncidents(IncidentFiltreDTO filtres) {
        Sort sort = filtres.getSortDir().equalsIgnoreCase("DESC") ? 
                Sort.by(filtres.getSortBy()).descending() : Sort.by(filtres.getSortBy()).ascending();
        Pageable pageable = PageRequest.of(filtres.getPage(), filtres.getSize(), sort);
        
        return incidentRepository.findWithFilters(
                filtres.getStatut(),
                filtres.getPriorite(),
                filtres.getQuartierId(),
                filtres.getDepartementId(),
                filtres.getDateDebut(),
                filtres.getDateFin(),
                pageable
        );
    }
    
    /**
     * Recherche avancée d'utilisateurs avec filtres
     */
    public Page<Utilisateur> rechercherUtilisateurs(UtilisateurFiltreDTO filtres) {
        Sort sort = filtres.getSortDir().equalsIgnoreCase("DESC") ? 
                Sort.by(filtres.getSortBy()).descending() : Sort.by(filtres.getSortBy()).ascending();
        Pageable pageable = PageRequest.of(filtres.getPage(), filtres.getSize(), sort);
        
        // Si recherche textuelle, utiliser la méthode de recherche
        if (filtres.getRecherche() != null && !filtres.getRecherche().trim().isEmpty()) {
            return utilisateurRepository.findWithFilters(
                    filtres.getRole(),
                    filtres.getDepartementId(),
                    filtres.getActif(),
                    filtres.getRecherche().trim(),
                    pageable
            );
        }
        
        // Sinon, utiliser les filtres simples
        if (filtres.getRole() != null && filtres.getDepartementId() != null && filtres.getActif() != null) {
            return utilisateurRepository.findByRoleAndDepartementIdAndActif(
                    filtres.getRole(),
                    filtres.getDepartementId(),
                    filtres.getActif(),
                    pageable
            );
        } else if (filtres.getRole() != null && filtres.getDepartementId() != null) {
            return utilisateurRepository.findByRoleAndDepartementId(
                    filtres.getRole(),
                    filtres.getDepartementId(),
                    pageable
            );
        } else if (filtres.getRole() != null && filtres.getActif() != null) {
            return utilisateurRepository.findByRoleAndActif(
                    filtres.getRole(),
                    filtres.getActif(),
                    pageable
            );
        } else if (filtres.getRole() != null) {
            return utilisateurRepository.findByRole(filtres.getRole(), pageable);
        } else if (filtres.getDepartementId() != null && filtres.getActif() != null) {
            return utilisateurRepository.findByDepartementIdAndActif(
                    filtres.getDepartementId(),
                    filtres.getActif(),
                    pageable
            );
        } else if (filtres.getDepartementId() != null) {
            return utilisateurRepository.findByDepartementId(filtres.getDepartementId(), pageable);
        } else if (filtres.getActif() != null) {
            return utilisateurRepository.findByActif(filtres.getActif(), pageable);
        }
        
        // Aucun filtre, retourner tous les utilisateurs
        return utilisateurRepository.findAll(pageable);
    }
    
    /**
     * Obtenir les statistiques pour les incidents filtrés
     */
    public Map<String, Object> getStatistiquesIncidents(IncidentFiltreDTO filtres) {
        Page<Incident> incidents = rechercherIncidents(filtres);
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total", incidents.getTotalElements());
        stats.put("parStatut", incidents.getContent().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        i -> i.getStatut().name(),
                        java.util.stream.Collectors.counting()
                )));
        stats.put("parPriorite", incidents.getContent().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        i -> i.getPriorite().name(),
                        java.util.stream.Collectors.counting()
                )));
        
        return stats;
    }
    
    /**
     * Obtenir les statistiques pour les utilisateurs filtrés
     */
    public Map<String, Object> getStatistiquesUtilisateurs(UtilisateurFiltreDTO filtres) {
        Page<Utilisateur> utilisateurs = rechercherUtilisateurs(filtres);
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total", utilisateurs.getTotalElements());
        stats.put("parRole", utilisateurs.getContent().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        u -> u.getRole().name(),
                        java.util.stream.Collectors.counting()
                )));
        stats.put("actifs", utilisateurs.getContent().stream()
                .filter(Utilisateur::isActif)
                .count());
        stats.put("inactifs", utilisateurs.getContent().stream()
                .filter(u -> !u.isActif())
                .count());
        
        return stats;
    }
}

