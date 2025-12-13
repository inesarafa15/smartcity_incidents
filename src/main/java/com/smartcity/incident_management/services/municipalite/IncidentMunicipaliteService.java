package com.smartcity.incident_management.services.municipalite;

import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.entities.Notification;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.StatutIncident;
import com.smartcity.incident_management.enums.TypeNotification;
import com.smartcity.incident_management.exceptions.ResourceNotFoundException;
import com.smartcity.incident_management.exceptions.UnauthorizedException;
import com.smartcity.incident_management.repository.IncidentRepository;
import com.smartcity.incident_management.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class IncidentMunicipaliteService {
    
    @Autowired
    private IncidentRepository incidentRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
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
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));
        
        if (!incident.getDepartement().getId().equals(agent.getDepartement().getId())) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à gérer cet incident");
        }
        
        incident.setAgentAssigne(agent);
        incident.setStatut(StatutIncident.PRIS_EN_CHARGE);
        
        // Créer une notification pour le citoyen
        creerNotification(incident.getAuteur(), incident, 
                "Votre incident '" + incident.getTitre() + "' a été pris en charge par un agent.");
        
        return incidentRepository.save(incident);
    }
    
    public Incident mettreEnResolution(Long incidentId, Utilisateur agent) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));
        
        if (!incident.getAgentAssigne().getId().equals(agent.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à modifier cet incident");
        }
        
        incident.setStatut(StatutIncident.EN_RESOLUTION);
        
        // Créer une notification
        creerNotification(incident.getAuteur(), incident, 
                "L'intervention pour votre incident '" + incident.getTitre() + "' est en cours.");
        
        return incidentRepository.save(incident);
    }
    
    public Incident marquerResolu(Long incidentId, Utilisateur agent) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));
        
        if (!incident.getAgentAssigne().getId().equals(agent.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à modifier cet incident");
        }
        
        incident.setStatut(StatutIncident.RESOLU);
        
        // Créer une notification
        creerNotification(incident.getAuteur(), incident, 
                "Votre incident '" + incident.getTitre() + "' a été résolu. Merci de confirmer.");
        
        return incidentRepository.save(incident);
    }
    
    public List<Incident> mesIncidentsAssignes(Utilisateur agent) {
        return incidentRepository.findByAgentAssigneId(agent.getId());
    }
    
    private void creerNotification(Utilisateur utilisateur, Incident incident, String message) {
        Notification notification = new Notification();
        notification.setUtilisateur(utilisateur);
        notification.setIncident(incident);
        notification.setType(TypeNotification.EMAIL);
        notification.setMessage(message);
        notification.setLu(false);
        
        notificationRepository.save(notification);
    }
}


