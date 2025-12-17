package com.smartcity.incident_management.services.utilisateur;

import com.smartcity.incident_management.dto.RapportDTO;
import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.entities.Notification;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.PrioriteIncident;
import com.smartcity.incident_management.enums.RoleType;
import com.smartcity.incident_management.enums.StatutIncident;
import com.smartcity.incident_management.enums.TypeNotification;
import com.smartcity.incident_management.exceptions.ResourceNotFoundException;
import com.smartcity.incident_management.exceptions.UnauthorizedException;
import com.smartcity.incident_management.repository.IncidentRepository;
import com.smartcity.incident_management.repository.NotificationRepository;
import com.smartcity.incident_management.repository.UtilisateurRepository;
import com.smartcity.incident_management.services.email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private NotificationRepository notificationRepository;
    
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
        
        List<Incident> incidents = incidentRepository.findByDepartementId(departementId);
        
        Map<String, Long> parStatut = new HashMap<>();
        for (StatutIncident statut : StatutIncident.values()) {
            long count = incidents.stream()
                    .filter(i -> i.getStatut() == statut)
                    .count();
            parStatut.put(statut.name(), count);
        }
        stats.put("incidentsParStatut", parStatut);
        
        Map<String, Long> parPriorite = new HashMap<>();
        for (PrioriteIncident priorite : PrioriteIncident.values()) {
            long count = incidents.stream()
                    .filter(i -> i.getPriorite() == priorite)
                    .count();
            parPriorite.put(priorite.name(), count);
        }
        stats.put("incidentsParPriorite", parPriorite);
        
        long totalIncidents = incidents.size();
        long incidentsResolus = incidents.stream()
                .filter(i -> i.getStatut() == StatutIncident.RESOLU || i.getStatut() == StatutIncident.CLOTURE)
                .count();
        long incidentsEnCours = incidents.stream()
                .filter(i -> i.getStatut() == StatutIncident.PRIS_EN_CHARGE || 
                             i.getStatut() == StatutIncident.EN_RESOLUTION)
                .count();
        long incidentsEnAttente = incidents.stream()
                .filter(i -> i.getStatut() == StatutIncident.SIGNALE)
                .count();
        
        List<Utilisateur> agents = agentsDisponibles(admin);
        
        double tempsMoyen = incidents.stream()
                .filter(i -> i.getStatut() == StatutIncident.RESOLU || i.getStatut() == StatutIncident.CLOTURE)
                .filter(i -> i.getDateCreation() != null && i.getDateDerniereMiseAJour() != null)
                .mapToLong(i -> java.time.Duration.between(i.getDateCreation(), i.getDateDerniereMiseAJour()).toHours())
                .average()
                .orElse(0.0);
        
        stats.put("totalIncidents", totalIncidents);
        stats.put("incidentsResolus", incidentsResolus);
        stats.put("incidentsEnCours", incidentsEnCours);
        stats.put("incidentsEnAttente", incidentsEnAttente);
        stats.put("agentsDisponibles", (long) agents.size());
        stats.put("totalAgents", utilisateurRepository.findByDepartementId(departementId)
                .stream()
                .filter(u -> u.getRole() == RoleType.AGENT_MUNICIPAL)
                .count());
        stats.put("tempsResolutionMoyen", String.format("%.1f", tempsMoyen));
        
        return stats;
    }
    
    // ========== RAPPORTS DU DÉPARTEMENT ==========
    
    public void genererRapportDepartement(Utilisateur admin, RapportDTO dto) {
        if (admin.getDepartement() == null) {
            throw new UnauthorizedException("Vous n'êtes pas affecté à un département");
        }
        
        if (dto.getDepartementId() != null && !dto.getDepartementId().equals(admin.getDepartement().getId())) {
            throw new UnauthorizedException("Vous ne pouvez générer des rapports que pour votre département");
        }
        
        if (dto.getDepartementId() == null) {
            dto.setDepartementId(admin.getDepartement().getId());
        }
        
        rapportService.genererRapport(admin, dto);
    }
    
    // ========== VALIDATION FINALE DES INCIDENTS ==========
    
    public Incident consulterDetailsIncident(Long incidentId, Utilisateur admin) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));
        verifierDepartement(admin, incident.getDepartement().getId());
        if (incident.getPhotos() != null) {
            incident.getPhotos().size();
        }
        if (incident.getNotificationsIncident() != null) {
            incident.getNotificationsIncident().size();
        }
        return incident;
    }
    
    public Incident validerResolution(Long incidentId, Utilisateur admin, String commentaireValidation) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));
        verifierDepartement(admin, incident.getDepartement().getId());
        
        if (incident.getStatut() != StatutIncident.RESOLU) {
            throw new UnauthorizedException("L'incident doit être en statut RESOLU pour être validé");
        }
        
        if (incident.getDateFeedback() == null) {
            throw new UnauthorizedException("Le citoyen doit d'abord fournir son feedback avant la validation");
        }
        
        StatutIncident ancien = incident.getStatut();
        incident.setStatut(StatutIncident.CLOTURE);
        incident.setDateDerniereMiseAJour(LocalDateTime.now());
        Incident saved = incidentRepository.save(incident);
        
        // Message de validation structuré
        String messageValidation = "✅ RÉSOLUTION VALIDÉE ET INCIDENT CLÔTURÉ\n\n" +
                                   "Par : " + admin.getPrenom() + " " + admin.getNom() + " (Administrateur)\n" +
                                   "Date : " + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
        
        if (commentaireValidation != null && !commentaireValidation.trim().isEmpty()) {
            messageValidation += "\n\n📝 Commentaire de validation :\n" + commentaireValidation.trim();
        }
        
        // Ajouter le feedback dans la notification de validation
        messageValidation += "\n\n⭐ Feedback du citoyen :\n" +
                            "• Satisfaction : " + (saved.getFeedbackSatisfait() ? "Satisfait ✓" : "Non satisfait ✗") + "\n";
        
        if (saved.getFeedbackNote() != null) {
            messageValidation += "• Note : " + saved.getFeedbackNote() + "/5\n";
        }
        
        if (saved.getFeedbackCommentaire() != null && !saved.getFeedbackCommentaire().isEmpty()) {
            messageValidation += "• Commentaire : " + saved.getFeedbackCommentaire();
        }
        
        // Notification pour le citoyen
        Notification notifCitoyen = new Notification();
        notifCitoyen.setIncident(saved);
        notifCitoyen.setUtilisateur(saved.getAuteur());
        notifCitoyen.setType(TypeNotification.PUSH);
        notifCitoyen.setMessage(messageValidation);
        notifCitoyen.setLu(false);
        notifCitoyen.setDateEnvoi(LocalDateTime.now());
        notificationRepository.save(notifCitoyen);
        
        // Notification pour l'agent
        if (saved.getAgentAssigne() != null) {
            Notification notifAgent = new Notification();
            notifAgent.setIncident(saved);
            notifAgent.setUtilisateur(saved.getAgentAssigne());
            notifAgent.setType(TypeNotification.PUSH);
            notifAgent.setMessage(messageValidation);
            notifAgent.setLu(false);
            notifAgent.setDateEnvoi(LocalDateTime.now());
            notificationRepository.save(notifAgent);
        }
        
        System.out.println("[ADMIN] Validation résolution incident #" + incidentId + " par " + admin.getEmail());
        
        // Envoyer emails
        try {
            emailService.envoyerEmailChangementStatut(incident.getAuteur(), saved, ancien, StatutIncident.CLOTURE);
            
            if (incident.getAgentAssigne() != null) {
                String subjectAgent = "✅ Résolution validée pour l'incident #" + saved.getId();
                String textAgent = "Bonjour " + incident.getAgentAssigne().getPrenom() + ",\n\n" +
                        "Bonne nouvelle ! La résolution de l'incident '" + saved.getTitre() + "' a été validée par l'administrateur.\n\n" +
                        "L'incident est maintenant clôturé définitivement.\n\n" +
                        (commentaireValidation != null && !commentaireValidation.trim().isEmpty() ? 
                         "📝 Commentaire de l'administrateur :\n" + commentaireValidation.trim() + "\n\n" : "") +
                        "⭐ Feedback du citoyen :\n" +
                        "• Satisfaction : " + (saved.getFeedbackSatisfait() ? "Satisfait ✓" : "Non satisfait ✗") + "\n" +
                        (saved.getFeedbackNote() != null ? "• Note : " + saved.getFeedbackNote() + "/5\n" : "") +
                        (saved.getFeedbackCommentaire() != null && !saved.getFeedbackCommentaire().isEmpty() ? 
                         "• Commentaire : " + saved.getFeedbackCommentaire() + "\n" : "") +
                        "\nFélicitations pour votre travail !\n\n" +
                        "Cordialement,\n" +
                        "L'équipe de gestion des incidents";
                emailService.envoyerEmailSimple(incident.getAgentAssigne().getEmail(), subjectAgent, textAgent);
            }
        } catch (Exception e) {
            System.err.println("Erreur email validation: " + e.getMessage());
        }
        
        return saved;
    }
    
    public Incident refuserResolution(Long incidentId, Utilisateur admin, String motif) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));
        verifierDepartement(admin, incident.getDepartement().getId());
        
        if (incident.getStatut() != StatutIncident.RESOLU) {
            throw new UnauthorizedException("L'incident doit être en statut RESOLU pour être refusé");
        }
        
        Utilisateur agent = incident.getAgentAssigne();
        if (agent == null) {
            throw new UnauthorizedException("Aucun agent assigné pour réassignation");
        }
        
        if (motif == null || motif.trim().isEmpty()) {
            throw new IllegalArgumentException("Le motif du refus est obligatoire");
        }
        
        // Sauvegarder le feedback actuel avant de le réinitialiser
        Boolean feedbackSatisfaitActuel = incident.getFeedbackSatisfait();
        Integer feedbackNoteActuelle = incident.getFeedbackNote();
        String feedbackCommentaireActuel = incident.getFeedbackCommentaire();
        LocalDateTime dateFeedbackActuelle = incident.getDateFeedback();
        
        StatutIncident ancien = incident.getStatut();
        incident.setStatut(StatutIncident.PRIS_EN_CHARGE);
        incident.setAgentAssigne(agent);
        incident.setDateDerniereMiseAJour(LocalDateTime.now());
        
        // Marquer les photos de résolution actuelles comme refusées
        if (incident.getPhotos() != null) {
            for (com.smartcity.incident_management.entities.Photo photo : incident.getPhotos()) {
                if (photo.getTypePhoto() == com.smartcity.incident_management.enums.TypePhoto.RESOLUTION && 
                    (photo.getEstRefuse() == null || !photo.getEstRefuse())) {
                    photo.setEstRefuse(true);
                }
            }
        }
        
        // IMPORTANT: Réinitialiser le feedback pour permettre un nouveau feedback
        incident.setFeedbackSatisfait(null);
        incident.setFeedbackNote(null);
        incident.setFeedbackCommentaire(null);
        incident.setDateFeedback(null);
        
        Incident saved = incidentRepository.save(incident);
        
        // Message de refus structuré pour l'agent (avec toutes les infos)
        String messageRefusAgent = "❌ RÉSOLUTION REFUSÉE - INCIDENT RÉASSIGNÉ\n\n" +
                                  "Par : " + admin.getPrenom() + " " + admin.getNom() + " (Administrateur)\n" +
                                  "Date : " + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) + "\n" +
                                  "Réassigné à : " + agent.getPrenom() + " " + agent.getNom() + "\n\n" +
                                  "📋 Motif du refus :\n" + motif.trim();
        
        // Ajouter le feedback précédent dans la notification pour l'agent
        if (dateFeedbackActuelle != null) {
            messageRefusAgent += "\n\n⭐ Feedback du citoyen (précédent) :\n" +
                                "• Satisfaction : " + (feedbackSatisfaitActuel ? "Satisfait ✓" : "Non satisfait ✗") + "\n";
            
            if (feedbackNoteActuelle != null) {
                messageRefusAgent += "• Note : " + feedbackNoteActuelle + "/5\n";
            }
            
            if (feedbackCommentaireActuel != null && !feedbackCommentaireActuel.isEmpty()) {
                messageRefusAgent += "• Commentaire : " + feedbackCommentaireActuel;
            }
        }
        
        // Message de refus simplifié pour le citoyen (AVEC feedback pour historique)
        String messageRefusCitoyen = "🔄 INTERVENTION COMPLÉMENTAIRE NÉCESSAIRE\n\n" +
                                    "Date : " + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) + "\n\n" +
                                    "Votre incident nécessite une intervention complémentaire pour s'assurer d'une résolution optimale.\n\n" +
                                    "Notre équipe reprend le dossier. Vous serez informé(e) dès que les travaux seront finalisés.\n\n" +
                                    "ℹ️ Vous pourrez donner un nouveau feedback une fois l'incident résolu à nouveau.";
        
        // Ajouter le feedback précédent dans la notification pour le citoyen (Historique)
        if (dateFeedbackActuelle != null) {
            messageRefusCitoyen += "\n\n📝 Votre feedback précédent (archivé) :\n" +
                                "• Satisfaction : " + (feedbackSatisfaitActuel ? "Satisfait" : "Non satisfait") + "\n";
            
            if (feedbackNoteActuelle != null) {
                messageRefusCitoyen += "• Note : " + feedbackNoteActuelle + "/5\n";
            }
            
            if (feedbackCommentaireActuel != null && !feedbackCommentaireActuel.isEmpty()) {
                messageRefusCitoyen += "• Commentaire : " + feedbackCommentaireActuel;
            }
        }
        
        // Notification pour le citoyen
        Notification notifCitoyen = new Notification();
        notifCitoyen.setIncident(saved);
        notifCitoyen.setUtilisateur(saved.getAuteur());
        notifCitoyen.setType(TypeNotification.PUSH);
        notifCitoyen.setMessage(messageRefusCitoyen);
        notifCitoyen.setLu(false);
        notifCitoyen.setDateEnvoi(LocalDateTime.now());
        notificationRepository.save(notifCitoyen);
        
        // Notification pour l'agent (message complet AVEC motif ET feedback)
        Notification notifAgent = new Notification();
        notifAgent.setIncident(saved);
        notifAgent.setUtilisateur(agent);
        notifAgent.setType(TypeNotification.PUSH);
        notifAgent.setMessage(messageRefusAgent);
        notifAgent.setLu(false);
        notifAgent.setDateEnvoi(LocalDateTime.now());
        notificationRepository.save(notifAgent);
        
        System.out.println("[ADMIN] Refus résolution incident #" + incidentId + " par " + admin.getEmail());
        
        // Envoyer emails
        try {
            String subjectCitoyen = "🔄 Mise à jour de votre incident #" + saved.getId();
            String textCitoyen = "Bonjour " + incident.getAuteur().getPrenom() + ",\n\n" +
                    "Nous vous informons que votre incident '" + saved.getTitre() + "' nécessite une intervention complémentaire.\n\n" +
                    "Notre équipe reprend le dossier pour s'assurer d'une résolution optimale.\n" +
                    "Vous serez informé(e) dès que les travaux seront finalisés.\n\n" +
                    "ℹ️ Vous pourrez donner un nouveau feedback une fois l'incident résolu à nouveau.\n\n" +
                    "Merci de votre patience et de votre compréhension.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe de gestion des incidents";
            emailService.envoyerEmailSimple(incident.getAuteur().getEmail(), subjectCitoyen, textCitoyen);
            
            String subjectAgent = "🔄 Résolution non validée pour l'incident #" + saved.getId();
            String textAgent = "Bonjour " + agent.getPrenom() + ",\n\n" +
                    "La résolution de l'incident '" + saved.getTitre() + "' n'a pas été validée par l'administrateur.\n\n" +
                    "📋 MOTIF DU REFUS :\n" + motif.trim() + "\n\n";
            
            if (dateFeedbackActuelle != null) {
                textAgent += "⭐ FEEDBACK DU CITOYEN (précédent) :\n" +
                            "• Satisfaction : " + (feedbackSatisfaitActuel ? "Satisfait ✓" : "Non satisfait ✗") + "\n";
                
                if (feedbackNoteActuelle != null) {
                    textAgent += "• Note : " + feedbackNoteActuelle + "/5\n";
                }
                
                if (feedbackCommentaireActuel != null && !feedbackCommentaireActuel.isEmpty()) {
                    textAgent += "• Commentaire : " + feedbackCommentaireActuel + "\n";
                }
                
                textAgent += "\n";
            }
            
            textAgent += "📌 PROCHAINE ÉTAPE :\n" +
                        "L'incident vous est réassigné et repasse en statut PRIS EN CHARGE.\n" +
                        "Veuillez prendre en compte les remarques de l'administrateur.\n\n" +
                        "ℹ️ Le citoyen pourra donner un nouveau feedback après la prochaine résolution.\n\n" +
                        "Cordialement,\n" +
                        "L'équipe de gestion des incidents";
            emailService.envoyerEmailSimple(agent.getEmail(), subjectAgent, textAgent);
        } catch (Exception e) {
            System.err.println("Erreur email refus: " + e.getMessage());
        }
        
        return saved;
    }
}