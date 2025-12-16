package com.smartcity.incident_management.services.email;

import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.entities.PasswordResetToken;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.StatutIncident;
import com.smartcity.incident_management.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    /**
     * Envoie un email de bienvenue lors de la création d'un compte
     */
    public void envoyerEmailBienvenue(Utilisateur utilisateur, String motDePasseTemporaire) {
        try {
            String tokenValue = UUID.randomUUID().toString();
            PasswordResetToken token = new PasswordResetToken();
            token.setToken(tokenValue);
            token.setUtilisateur(utilisateur);
            token.setCreatedAt(LocalDateTime.now());
            token.setExpiresAt(LocalDateTime.now().plusHours(24));
            token.setUsed(false);
            passwordResetTokenRepository.save(token);
            
            Context context = new Context(Locale.FRENCH);
            context.setVariable("utilisateur", utilisateur);
            context.setVariable("email", utilisateur.getEmail());
            context.setVariable("loginUrl", baseUrl + "/connexion");
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("resetUrl", baseUrl + "/mot-de-passe/reinitialiser?token=" + tokenValue);
            
            String htmlContent = templateEngine.process("emails/bienvenue", context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(utilisateur.getEmail());
            helper.setSubject("Bienvenue sur SmartCity - Votre compte a été créé");
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email de bienvenue", e);
        }
    }
    
    /**
     * Envoie un email de notification lors d'un changement de statut d'incident
     */
    public void envoyerEmailChangementStatut(Utilisateur destinataire, Incident incident, StatutIncident ancienStatut, StatutIncident nouveauStatut) {
        try {
            Context context = new Context(Locale.FRENCH);
            context.setVariable("utilisateur", destinataire);
            context.setVariable("incident", incident);
            context.setVariable("ancienStatut", ancienStatut);
            context.setVariable("nouveauStatut", nouveauStatut);
            context.setVariable("incidentUrl", baseUrl + "/citoyen/incidents/" + incident.getId());
            context.setVariable("baseUrl", baseUrl);
            
            String htmlContent = templateEngine.process("emails/changement-statut", context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(destinataire.getEmail());
            helper.setSubject("Mise à jour de votre incident : " + incident.getTitre());
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email de changement de statut", e);
        }
    }
    
    /**
     * Envoie un email de notification lors de l'assignation d'un agent à un incident
     */
    public void envoyerEmailAssignationAgent(Utilisateur citoyen, Incident incident, Utilisateur agent) {
        try {
            Context context = new Context(Locale.FRENCH);
            context.setVariable("utilisateur", citoyen);
            context.setVariable("incident", incident);
            context.setVariable("agent", agent);
            context.setVariable("incidentUrl", baseUrl + "/citoyen/incidents/" + incident.getId());
            context.setVariable("baseUrl", baseUrl);
            
            String htmlContent = templateEngine.process("emails/assignation-agent", context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(citoyen.getEmail());
            helper.setSubject("Un agent a été assigné à votre incident : " + incident.getTitre());
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email d'assignation", e);
        }
    }
    
    /**
     * Envoie un email de notification lors de la résolution d'un incident
     */
    public void envoyerEmailResolution(Utilisateur citoyen, Incident incident) {
        try {
            Context context = new Context(Locale.FRENCH);
            context.setVariable("utilisateur", citoyen);
            context.setVariable("incident", incident);
            context.setVariable("incidentUrl", baseUrl + "/citoyen/incidents/" + incident.getId());
            context.setVariable("baseUrl", baseUrl);
            
            String htmlContent = templateEngine.process("emails/resolution", context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(citoyen.getEmail());
            helper.setSubject("Votre incident a été résolu : " + incident.getTitre());
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email de résolution", e);
        }
    }
    
    /**
     * Envoie un email simple (fallback)
     */
    public void envoyerEmailSimple(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

	public void envoyerEmailMiseAJour(Utilisateur auteur, Incident incident, Utilisateur agent, String message) {
		// TODO Auto-generated method stub
		
	}

	public void sendNotificationEmail(String email, String string, String message) {
		// TODO Auto-generated method stub
		
	}
    
}

