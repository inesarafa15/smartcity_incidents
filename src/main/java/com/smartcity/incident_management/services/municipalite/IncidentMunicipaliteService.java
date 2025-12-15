package com.smartcity.incident_management.services.municipalite;

import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.entities.Notification;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.PrioriteIncident;
import com.smartcity.incident_management.enums.StatutIncident;
import com.smartcity.incident_management.enums.TypeNotification;
import com.smartcity.incident_management.exceptions.ResourceNotFoundException;
import com.smartcity.incident_management.exceptions.UnauthorizedException;
import com.smartcity.incident_management.repository.IncidentRepository;
import com.smartcity.incident_management.repository.NotificationRepository;
import com.smartcity.incident_management.services.email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class IncidentMunicipaliteService {
    
    @Autowired
    private IncidentRepository incidentRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private EmailService emailService;
    
    public Incident getIncidentById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé avec l'ID: " + id));
    }
    
    public Page<Incident> incidentsDuDepartement(Utilisateur agent, int page, int size, String sortBy, String sortDir) {
        if (agent.getDepartement() == null) {
            throw new UnauthorizedException("L'agent n'est pas assigné à un département");
        }
        
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return incidentRepository.findByDepartementId(agent.getDepartement().getId(), pageable);
    }
    
    public Incident prendreEnCharge(Long incidentId, Utilisateur agent) {
        Incident incident = getIncidentById(incidentId);
        
        // Vérifier que l'agent appartient au même département que l'incident
        if (agent.getDepartement() == null) {
            throw new UnauthorizedException("Vous n'êtes pas assigné à un département");
        }
        
        if (!incident.getDepartement().getId().equals(agent.getDepartement().getId())) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à gérer cet incident (département différent)");
        }
        
        // Vérifier que l'incident est dans un état valide pour cette transition
        if (incident.getStatut() != StatutIncident.SIGNALE) {
            throw new UnauthorizedException("L'incident doit être en statut SIGNALE pour être pris en charge");
        }
        
        StatutIncident ancienStatut = incident.getStatut();
        incident.setAgentAssigne(agent);
        incident.setStatut(StatutIncident.PRIS_EN_CHARGE);
        incident.setDateDerniereMiseAJour(LocalDateTime.now());
        
        Incident saved = incidentRepository.save(incident);
        
        // Créer une notification pour le citoyen
        creerNotification(
            incident.getAuteur(), 
            saved, 
            "Votre incident '" + saved.getTitre() + "' a été pris en charge par un agent municipal."
        );
        
        // Envoyer un email au citoyen
        try {
            emailService.envoyerEmailChangementStatut(incident.getAuteur(), saved, ancienStatut, StatutIncident.PRIS_EN_CHARGE);
            emailService.envoyerEmailAssignationAgent(incident.getAuteur(), saved, agent);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
            // Ne pas interrompre le flux en cas d'erreur d'email
        }
        
        return saved;
    }
    
    public Incident mettreEnResolution(Long incidentId, Utilisateur agent) {
        Incident incident = getIncidentById(incidentId);
        
        // Vérifier que l'incident est assigné à cet agent
        if (incident.getAgentAssigne() == null || !incident.getAgentAssigne().getId().equals(agent.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à modifier cet incident");
        }
        
        // Vérifier que l'incident est dans un état valide pour cette transition
        if (incident.getStatut() != StatutIncident.PRIS_EN_CHARGE) {
            throw new UnauthorizedException("L'incident doit être en statut PRIS_EN_CHARGE pour être mis en résolution");
        }
        
        // Vérifier que l'agent appartient au même département que l'incident
        if (agent.getDepartement() == null || !agent.getDepartement().getId().equals(incident.getDepartement().getId())) {
            throw new UnauthorizedException("Vous n'appartenez pas au département de cet incident");
        }
        
        StatutIncident ancienStatut = incident.getStatut();
        incident.setStatut(StatutIncident.EN_RESOLUTION);
        incident.setDateDerniereMiseAJour(LocalDateTime.now());
        
        Incident saved = incidentRepository.save(incident);
        
        // Créer une notification
        creerNotification(
            incident.getAuteur(), 
            saved, 
            "L'intervention pour votre incident '" + saved.getTitre() + "' est en cours de résolution."
        );
        
        // Envoyer un email au citoyen
        try {
            emailService.envoyerEmailChangementStatut(incident.getAuteur(), saved, ancienStatut, StatutIncident.EN_RESOLUTION);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
        
        return saved;
    }
    
    public Incident marquerResolu(Long incidentId, Utilisateur agent) {
        Incident incident = getIncidentById(incidentId);
        
        // Vérifier que l'incident est assigné à cet agent
        if (incident.getAgentAssigne() == null || !incident.getAgentAssigne().getId().equals(agent.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à modifier cet incident");
        }
        
        // Vérifier que l'incident est dans un état valide pour cette transition
        if (incident.getStatut() != StatutIncident.EN_RESOLUTION) {
            throw new UnauthorizedException("L'incident doit être en statut EN_RESOLUTION pour être marqué comme résolu");
        }
        
        // Vérifier que l'agent appartient au même département que l'incident
        if (agent.getDepartement() == null || !agent.getDepartement().getId().equals(incident.getDepartement().getId())) {
            throw new UnauthorizedException("Vous n'appartenez pas au département de cet incident");
        }
        
        StatutIncident ancienStatut = incident.getStatut();
        incident.setStatut(StatutIncident.RESOLU);
        incident.setDateDerniereMiseAJour(LocalDateTime.now());
        
        Incident saved = incidentRepository.save(incident);
        
        // Créer une notification
        creerNotification(
            incident.getAuteur(), 
            saved, 
            "Votre incident '" + saved.getTitre() + "' a été marqué comme résolu. Merci de confirmer la résolution."
        );
        
        // Envoyer un email au citoyen
        try {
            emailService.envoyerEmailChangementStatut(incident.getAuteur(), saved, ancienStatut, StatutIncident.RESOLU);
            emailService.envoyerEmailResolution(incident.getAuteur(), saved);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
        
        return saved;
    }
    
    public List<Incident> mesIncidentsAssignes(Utilisateur agent) {
        // Retourner uniquement les incidents assignés à cet agent
        // avec vérification supplémentaire du département pour sécurité
        List<Incident> incidents = incidentRepository.findByAgentAssigneId(agent.getId());
        
        if (agent.getDepartement() != null) {
            // Filtrer pour ne retourner que les incidents du département de l'agent
            return incidents.stream()
                    .filter(i -> i.getDepartement() != null && 
                            i.getDepartement().getId().equals(agent.getDepartement().getId()))
                    .toList();
        }
        
        return incidents;
    }

    public Page<Incident> mesIncidentsAssignes(
            Utilisateur agent, 
            StatutIncident statut, 
            PrioriteIncident priorite, 
            int page, 
            int size, 
            String sortBy, 
            String sortDir) {
        
        if (agent.getDepartement() == null) {
            throw new UnauthorizedException("L'agent n'est pas assigné à un département");
        }
        
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return incidentRepository.findAssignedToAgentWithFilters(
                agent.getId(),
                statut,
                priorite,
                agent.getDepartement().getId(),
                pageable
        );
    }
    
    public void ajouterCommentaire(Long incidentId, Utilisateur agent, String commentaire) {
        Incident incident = getIncidentById(incidentId);
        
        // Vérifier que l'agent a accès à cet incident
        if (agent.getDepartement() == null || !agent.getDepartement().getId().equals(incident.getDepartement().getId())) {
            throw new UnauthorizedException("Vous n'avez pas accès à cet incident");
        }
        
        // Créer une notification de commentaire (interne au département)
        Notification notification = new Notification();
        notification.setUtilisateur(agent); // L'agent est l'expéditeur
        notification.setIncident(incident);
        
        notification.setMessage(commentaire);
        notification.setLu(false);
        notification.setDateEnvoi(LocalDateTime.now());
        
        notificationRepository.save(notification);
        
        // Mettre à jour la date de dernière mise à jour de l'incident
        incident.setDateDerniereMiseAJour(LocalDateTime.now());
        incidentRepository.save(incident);
        
        System.out.println("Commentaire ajouté par l'agent " + agent.getNom() + " sur l'incident #" + incidentId);
    }
    
    public void envoyerMiseAJour(Long incidentId, Utilisateur agent, String message) {
        Incident incident = getIncidentById(incidentId);
        
        // Vérifier que l'agent a accès à cet incident
        if (agent.getDepartement() == null || !agent.getDepartement().getId().equals(incident.getDepartement().getId())) {
            throw new UnauthorizedException("Vous n'avez pas accès à cet incident");
        }
        
        // Créer une notification pour le citoyen
        creerNotification(
            incident.getAuteur(), 
            incident, 
            message
        );
        
        // Envoyer un email
  
    }
    
    public void envoyerMiseAJourCitoyen(Long incidentId, Utilisateur agent, String message) {
        Incident incident = getIncidentById(incidentId);
        
        // Vérifier que l'agent est assigné à cet incident
        if (incident.getAgentAssigne() == null || !incident.getAgentAssigne().getId().equals(agent.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à envoyer des mises à jour pour cet incident");
        }
        
        // Créer une notification pour le citoyen
        creerNotification(
            incident.getAuteur(), 
            incident, 
            "Mise à jour concernant votre incident '" + incident.getTitre() + "':\n\n" + message
        );
        
        
        
        // Mettre à jour la date de dernière mise à jour de l'incident
        incident.setDateDerniereMiseAJour(LocalDateTime.now());
        incidentRepository.save(incident);
        
        System.out.println("Mise à jour envoyée au citoyen par l'agent " + agent.getNom() + " pour l'incident #" + incidentId);
    }
    
    public Page<Incident> filtrerIncidentsDepartement(
            Utilisateur agent,
            boolean assignedToMe,
            String statut, 
            String priorite, 
            String dateDebut, 
            String dateFin,
            String keyword,
            int page, int size, String sortBy, String sortDir) {
        
        if (agent.getDepartement() == null) {
            throw new UnauthorizedException("L'agent n'est pas assigné à un département");
        }
        
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        StatutIncident statutEnum = null;
        if (statut != null && !statut.isEmpty()) {
            try {
                statutEnum = StatutIncident.valueOf(statut);
            } catch (IllegalArgumentException e) {
                // Ignorer
            }
        }
        
        PrioriteIncident prioriteEnum = null;
        if (priorite != null && !priorite.isEmpty()) {
            try {
                prioriteEnum = PrioriteIncident.valueOf(priorite);
            } catch (IllegalArgumentException e) {
                // Ignorer
            }
        }
        
        LocalDateTime start = null;
        if (dateDebut != null && !dateDebut.isEmpty()) {
            try {
                start = java.time.LocalDate.parse(dateDebut).atStartOfDay();
            } catch (Exception e) {
                // Ignorer
            }
        }
        
        LocalDateTime end = null;
        if (dateFin != null && !dateFin.isEmpty()) {
            try {
                end = java.time.LocalDate.parse(dateFin).atTime(23, 59, 59);
            } catch (Exception e) {
                // Ignorer
            }
        }
        
        Long agentIdFilter = assignedToMe ? agent.getId() : null;

        return incidentRepository.findWithFiltersAndKeyword(
                statutEnum,
                prioriteEnum,
                null, // quartierId
                agent.getDepartement().getId(),
                agentIdFilter,
                start,
                end,
                keyword,
                pageable
        );
    }
    
    private void creerNotification(Utilisateur utilisateur, Incident incident, String message) {
        Notification notification = new Notification();
        notification.setUtilisateur(utilisateur);
        notification.setIncident(incident);
        notification.setType(TypeNotification.EMAIL);
        notification.setMessage(message);
        notification.setLu(false);
        notification.setDateEnvoi(LocalDateTime.now());
        
        notificationRepository.save(notification);
    }
}