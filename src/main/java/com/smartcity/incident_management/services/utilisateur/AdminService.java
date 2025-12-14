package com.smartcity.incident_management.services.utilisateur;

import com.smartcity.incident_management.dto.RapportDTO;
import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.PrioriteIncident;
import com.smartcity.incident_management.enums.RoleType;
import com.smartcity.incident_management.enums.StatutIncident;
import com.smartcity.incident_management.exceptions.ResourceNotFoundException;
import com.smartcity.incident_management.exceptions.UnauthorizedException;
import com.smartcity.incident_management.repository.IncidentRepository;
import com.smartcity.incident_management.repository.UtilisateurRepository;
import com.smartcity.incident_management.services.email.EmailService;
import com.smartcity.incident_management.services.utilisateur.RapportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AdminService {
    
    @Autowired
    private IncidentRepository incidentRepository;
    
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    
    @Autowired
    private RapportService rapportService;
    
    @Autowired
    private EmailService emailService;
    
    // ========== VÉRIFICATION DÉPARTEMENT ==========
    
    private void verifierDepartement(Utilisateur admin, Long departementId) {
        if (admin.getDepartement() == null) {
            throw new UnauthorizedException("Vous n'êtes pas affecté à un département");
        }
        if (!admin.getDepartement().getId().equals(departementId)) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à accéder à ce département");
        }
    }
    
    // ========== GESTION DES INCIDENTS DU DÉPARTEMENT ==========
    
    public Page<Incident> incidentsDuDepartement(Utilisateur admin, int page, int size, String sortBy, String sortDir) {
        if (admin.getDepartement() == null) {
            throw new UnauthorizedException("Vous n'êtes pas affecté à un département");
        }
        
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return incidentRepository.findByDepartementId(admin.getDepartement().getId(), pageable);
    }
    
    public List<Incident> incidentsEnAttente(Utilisateur admin) {
        if (admin.getDepartement() == null) {
            throw new UnauthorizedException("Vous n'êtes pas affecté à un département");
        }
        
        return incidentRepository.findByStatutAndDepartementId(
                StatutIncident.SIGNALE, 
                admin.getDepartement().getId()
        );
    }
    
    public Incident affecterIncidentAAgent(Long incidentId, Long agentId, Utilisateur admin) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));
        
        if (admin.getDepartement() == null) {
            throw new UnauthorizedException("Vous n'êtes pas affecté à un département");
        }
        
        if (!incident.getDepartement().getId().equals(admin.getDepartement().getId())) {
            throw new UnauthorizedException("Cet incident n'appartient pas à votre département");
        }
        
        Utilisateur agent = utilisateurRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent non trouvé"));
        
        if (agent.getRole() != RoleType.AGENT_MUNICIPAL) {
            throw new UnauthorizedException("L'utilisateur n'est pas un agent municipal");
        }
        
        if (agent.getDepartement() == null || !agent.getDepartement().getId().equals(admin.getDepartement().getId())) {
            throw new UnauthorizedException("L'agent n'appartient pas à votre département");
        }
        
        incident.setAgentAssigne(agent);
        incident.setStatut(StatutIncident.PRIS_EN_CHARGE);
        
        Incident saved = incidentRepository.save(incident);
        
        // Envoyer un email au citoyen
        try {
            emailService.envoyerEmailAssignationAgent(incident.getAuteur(), saved, agent);
            emailService.envoyerEmailChangementStatut(incident.getAuteur(), saved, StatutIncident.SIGNALE, StatutIncident.PRIS_EN_CHARGE);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
        
        return saved;
    }
    
    // ========== AGENTS DISPONIBLES DU DÉPARTEMENT ==========
    
    public List<Utilisateur> agentsDisponibles(Utilisateur admin) {
        if (admin.getDepartement() == null) {
            throw new UnauthorizedException("Vous n'êtes pas affecté à un département");
        }
        
        return utilisateurRepository.findByDepartementId(admin.getDepartement().getId())
                .stream()
                .filter(u -> u.getRole() == RoleType.AGENT_MUNICIPAL && u.isActif())
                .toList();
    }
    
    // ========== STATISTIQUES DU DÉPARTEMENT ==========
    
    public Map<String, Object> getStatistiquesDepartement(Utilisateur admin) {
        if (admin.getDepartement() == null) {
            throw new UnauthorizedException("Vous n'êtes pas affecté à un département");
        }
        
        Map<String, Object> stats = new HashMap<>();
        Long departementId = admin.getDepartement().getId();
        
        // Incidents par statut
        Map<String, Long> parStatut = new HashMap<>();
        for (StatutIncident statut : StatutIncident.values()) {
            long count = incidentRepository.findByStatutAndDepartementId(statut, departementId).size();
            parStatut.put(statut.name(), count);
        }
        stats.put("incidentsParStatut", parStatut);
        
        // Incidents par priorité
        List<Incident> incidents = incidentRepository.findByDepartementId(departementId);
        Map<String, Long> parPriorite = new HashMap<>();
        for (PrioriteIncident priorite : PrioriteIncident.values()) {
            long count = incidents.stream()
                    .filter(i -> i.getPriorite() == priorite)
                    .count();
            parPriorite.put(priorite.name(), count);
        }
        stats.put("incidentsParPriorite", parPriorite);
        
        // Agents disponibles
        stats.put("agentsDisponibles", agentsDisponibles(admin).size());
        stats.put("totalAgents", utilisateurRepository.findByDepartementId(departementId)
                .stream()
                .filter(u -> u.getRole() == RoleType.AGENT_MUNICIPAL)
                .count());
        
        // Incidents en attente
        stats.put("incidentsEnAttente", incidentsEnAttente(admin).size());
        
        return stats;
    }
    
    // ========== RAPPORTS DU DÉPARTEMENT ==========
    
    public void genererRapportDepartement(Utilisateur admin, RapportDTO dto) {
        if (admin.getDepartement() == null) {
            throw new UnauthorizedException("Vous n'êtes pas affecté à un département");
        }
        
        // S'assurer que le rapport concerne uniquement le département de l'admin
        if (dto.getDepartementId() != null && !dto.getDepartementId().equals(admin.getDepartement().getId())) {
            throw new UnauthorizedException("Vous ne pouvez générer des rapports que pour votre département");
        }
        
        // Si aucun département n'est spécifié, utiliser celui de l'admin
        if (dto.getDepartementId() == null) {
            dto.setDepartementId(admin.getDepartement().getId());
        }
        
        rapportService.genererRapport(admin, dto);
    }
}

