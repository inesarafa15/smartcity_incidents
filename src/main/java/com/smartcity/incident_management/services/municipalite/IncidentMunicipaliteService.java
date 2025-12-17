package com.smartcity.incident_management.services.municipalite;

import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.entities.Notification;
import com.smartcity.incident_management.entities.Photo;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.TypePhoto;
import com.smartcity.incident_management.enums.PrioriteIncident;
import com.smartcity.incident_management.enums.StatutIncident;
import com.smartcity.incident_management.enums.TypeNotification;
import com.smartcity.incident_management.exceptions.ResourceNotFoundException;
import com.smartcity.incident_management.exceptions.UnauthorizedException;
import com.smartcity.incident_management.repository.IncidentRepository;
import com.smartcity.incident_management.repository.NotificationRepository;
import com.smartcity.incident_management.repository.PhotoRepository;
import com.smartcity.incident_management.services.email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class IncidentMunicipaliteService {
    
    @Autowired
    private IncidentRepository incidentRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private PhotoRepository photoRepository;
    
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
    public Incident getIncidentById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé avec l'ID: " + id));
    }
    
    /**
     * Récupère tous les incidents assignés à un agent spécifique
     * Filtre strictement par agent ET par département pour sécurité
     */
    public List<Incident> mesIncidentsAssignes(Utilisateur agent) {
        if (agent.getDepartement() == null) {
            throw new UnauthorizedException("L'agent n'est pas assigné à un département");
        }
        
        // Récupérer les incidents assignés à cet agent
        List<Incident> incidents = incidentRepository.findByAgentAssigneId(agent.getId());
        
        // Double vérification : filtrer pour ne garder que ceux du département de l'agent
        return incidents.stream()
                .filter(i -> i.getDepartement() != null && 
                        i.getDepartement().getId().equals(agent.getDepartement().getId()))
                .collect(Collectors.toList());
    }

    /**
     * Filtrage avancé des incidents du département avec option assigné à moi
     */
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
                System.err.println("Statut invalide: " + statut);
            }
        }
        
        PrioriteIncident prioriteEnum = null;
        if (priorite != null && !priorite.isEmpty()) {
            try {
                prioriteEnum = PrioriteIncident.valueOf(priorite);
            } catch (IllegalArgumentException e) {
                System.err.println("Priorité invalide: " + priorite);
            }
        }
        
        LocalDateTime start = null;
        if (dateDebut != null && !dateDebut.isEmpty()) {
            try {
                start = java.time.LocalDate.parse(dateDebut).atStartOfDay();
            } catch (Exception e) {
                System.err.println("Date début invalide: " + dateDebut);
            }
        }
        
        LocalDateTime end = null;
        if (dateFin != null && !dateFin.isEmpty()) {
            try {
                end = java.time.LocalDate.parse(dateFin).atTime(23, 59, 59);
            } catch (Exception e) {
                System.err.println("Date fin invalide: " + dateFin);
            }
        }
        
        // Si assignedToMe = true, on filtre sur l'agent ID, sinon null
        Long agentIdFilter = assignedToMe ? agent.getId() : null;

        return incidentRepository.findWithFiltersAndKeyword(
                statutEnum,
                prioriteEnum,
                null, // quartierId
                agent.getDepartement().getId(), // département obligatoire
                agentIdFilter, // filtre agent si assignedToMe
                start,
                end,
                keyword,
                pageable
        );
    }
    
    public Incident prendreEnCharge(Long incidentId, Utilisateur agent) {
        Incident incident = getIncidentById(incidentId);
        
        // Vérifications de sécurité
        if (agent.getDepartement() == null) {
            throw new UnauthorizedException("Vous n'êtes pas assigné à un département");
        }
        
        if (!incident.getDepartement().getId().equals(agent.getDepartement().getId())) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à gérer cet incident (département différent)");
        }
        
        if (incident.getStatut() != StatutIncident.SIGNALE) {
            throw new UnauthorizedException("L'incident doit être en statut SIGNALE pour être pris en charge");
        }
        
        StatutIncident ancienStatut = incident.getStatut();
        incident.setAgentAssigne(agent);
        incident.setStatut(StatutIncident.PRIS_EN_CHARGE);
        incident.setDateDerniereMiseAJour(LocalDateTime.now());
        
        Incident saved = incidentRepository.save(incident);
        
        // Notifications
        creerNotification(
            incident.getAuteur(), 
            saved, 
            "Votre incident '" + saved.getTitre() + "' a été pris en charge par " + 
            agent.getPrenom() + " " + agent.getNom() + "."
        );
        
        // Email
        try {
            emailService.envoyerEmailChangementStatut(incident.getAuteur(), saved, ancienStatut, StatutIncident.PRIS_EN_CHARGE);
            emailService.envoyerEmailAssignationAgent(incident.getAuteur(), saved, agent);
        } catch (Exception e) {
            System.err.println("Erreur envoi email: " + e.getMessage());
        }
        
        return saved;
    }
    
    public Incident mettreEnResolution(Long incidentId, Utilisateur agent) {
        Incident incident = getIncidentById(incidentId);
        
        // Vérifications
        if (incident.getAgentAssigne() == null || !incident.getAgentAssigne().getId().equals(agent.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à modifier cet incident");
        }
        
        if (incident.getStatut() != StatutIncident.PRIS_EN_CHARGE) {
            throw new UnauthorizedException("L'incident doit être en statut PRIS_EN_CHARGE");
        }
        
        if (agent.getDepartement() == null || !agent.getDepartement().getId().equals(incident.getDepartement().getId())) {
            throw new UnauthorizedException("Vous n'appartenez pas au département de cet incident");
        }
        
        StatutIncident ancienStatut = incident.getStatut();
        incident.setStatut(StatutIncident.EN_RESOLUTION);
        incident.setDateDerniereMiseAJour(LocalDateTime.now());
        
        Incident saved = incidentRepository.save(incident);
        
        creerNotification(
            incident.getAuteur(), 
            saved, 
            "L'intervention pour votre incident '" + saved.getTitre() + "' est en cours de résolution."
        );
        
        try {
            emailService.envoyerEmailChangementStatut(incident.getAuteur(), saved, ancienStatut, StatutIncident.EN_RESOLUTION);
        } catch (Exception e) {
            System.err.println("Erreur envoi email: " + e.getMessage());
        }
        
        return saved;
    }
    
    public Incident marquerResolu(Long incidentId, Utilisateur agent) {
        Incident incident = getIncidentById(incidentId);
        
        // Vérifications
        if (incident.getAgentAssigne() == null || !incident.getAgentAssigne().getId().equals(agent.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à modifier cet incident");
        }
        
        if (incident.getStatut() != StatutIncident.EN_RESOLUTION) {
            throw new UnauthorizedException("L'incident doit être en statut EN_RESOLUTION");
        }
        
        if (agent.getDepartement() == null || !agent.getDepartement().getId().equals(incident.getDepartement().getId())) {
            throw new UnauthorizedException("Vous n'appartenez pas au département de cet incident");
        }
        
        StatutIncident ancienStatut = incident.getStatut();
        incident.setStatut(StatutIncident.RESOLU);
        incident.setDateDerniereMiseAJour(LocalDateTime.now());
        
        Incident saved = incidentRepository.save(incident);
        
        creerNotification(
            incident.getAuteur(), 
            saved, 
            "Votre incident '" + saved.getTitre() + "' a été marqué comme résolu. Merci de confirmer la résolution."
        );
        
        try {
            emailService.envoyerEmailChangementStatut(incident.getAuteur(), saved, ancienStatut, StatutIncident.RESOLU);
            emailService.envoyerEmailResolution(incident.getAuteur(), saved);
        } catch (Exception e) {
            System.err.println("Erreur envoi email: " + e.getMessage());
        }
        
        return saved;
    }
    
    public Incident marquerResoluAvecUpload(Long incidentId, Utilisateur agent, List<MultipartFile> images, String commentaire) throws IOException {
        Incident incident = getIncidentById(incidentId);
        
        if (incident.getAgentAssigne() == null || !incident.getAgentAssigne().getId().equals(agent.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à modifier cet incident");
        }
        if (incident.getStatut() != StatutIncident.EN_RESOLUTION) {
            throw new UnauthorizedException("L'incident doit être en statut EN_RESOLUTION");
        }
        if (agent.getDepartement() == null || !agent.getDepartement().getId().equals(incident.getDepartement().getId())) {
            throw new UnauthorizedException("Vous n'appartenez pas au département de cet incident");
        }
        
        if (images == null || images.isEmpty() || images.stream().allMatch(MultipartFile::isEmpty)) {
            throw new UnauthorizedException("Au moins une image est requise pour marquer l'incident résolu");
        }
        
        sauvegarderPhotos(incident, images);
        
        if (commentaire != null && !commentaire.isBlank()) {
            Notification notification = new Notification();
            notification.setUtilisateur(agent);
            notification.setIncident(incident);
            notification.setType(TypeNotification.PUSH);
            notification.setMessage("Commentaire d'intervention: " + commentaire.trim());
            notification.setLu(false);
            notification.setDateEnvoi(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        
        StatutIncident ancienStatut = incident.getStatut();
        incident.setStatut(StatutIncident.RESOLU);
        incident.setDateDerniereMiseAJour(LocalDateTime.now());
        Incident saved = incidentRepository.save(incident);
        
        creerNotification(
            incident.getAuteur(), 
            saved, 
            "Votre incident '" + saved.getTitre() + "' a été marqué comme résolu. Merci de confirmer la résolution."
        );
        
        try {
            emailService.envoyerEmailChangementStatut(incident.getAuteur(), saved, ancienStatut, StatutIncident.RESOLU);
            emailService.envoyerEmailResolution(incident.getAuteur(), saved);
        } catch (Exception e) {
            System.err.println("Erreur envoi email: " + e.getMessage());
        }
        
        return saved;
    }
    
    private void sauvegarderPhotos(Incident incident, List<MultipartFile> fichiers) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        for (MultipartFile fichier : fichiers) {
            if (!fichier.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + fichier.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(fichier.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                Photo photo = new Photo();
                photo.setTypePhoto(TypePhoto.RESOLUTION);
                photo.setCheminFichier("uploads/" + fileName);
                photo.setTypeMime(fichier.getContentType());
                photo.setTailleKo(fichier.getSize() / 1024);
                photo.setIncident(incident);
                photoRepository.save(photo);
            }
        }
    }
    
    public void ajouterCommentaire(Long incidentId, Utilisateur agent, String commentaire) {
        Incident incident = getIncidentById(incidentId);
        
        // Vérifier accès
        if (agent.getDepartement() == null || !agent.getDepartement().getId().equals(incident.getDepartement().getId())) {
            throw new UnauthorizedException("Vous n'avez pas accès à cet incident");
        }
        
        // Créer notification interne
        Notification notification = new Notification();
        notification.setUtilisateur(agent);
        notification.setIncident(incident);
        notification.setType(TypeNotification.PUSH);
        notification.setMessage("Commentaire: " + commentaire);
        notification.setLu(false);
        notification.setDateEnvoi(LocalDateTime.now());
        
        notificationRepository.save(notification);
        
        incident.setDateDerniereMiseAJour(LocalDateTime.now());
        incidentRepository.save(incident);
        
        System.out.println("Commentaire ajouté par " + agent.getNom() + " sur incident #" + incidentId);
    }
    
    public void envoyerMiseAJour(Long incidentId, Utilisateur agent, String message) {
        Incident incident = getIncidentById(incidentId);
        
        // Vérifier accès
        if (agent.getDepartement() == null || !agent.getDepartement().getId().equals(incident.getDepartement().getId())) {
            throw new UnauthorizedException("Vous n'avez pas accès à cet incident");
        }
        
        // Notification au citoyen
        creerNotification(
            incident.getAuteur(), 
            incident, 
            "Mise à jour de " + agent.getPrenom() + " " + agent.getNom() + ":\n\n" + message
        );
        
        // Email optionnel
        try {
            // emailService.envoyerEmailMiseAJour(incident.getAuteur(), incident, message);
        } catch (Exception e) {
            System.err.println("Erreur envoi email: " + e.getMessage());
        }
        
        incident.setDateDerniereMiseAJour(LocalDateTime.now());
        incidentRepository.save(incident);
        
        System.out.println("Mise à jour envoyée au citoyen par " + agent.getNom());
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
