package com.smartcity.incident_management.services.utilisateur;

import com.smartcity.incident_management.entities.Notification;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.exceptions.ResourceNotFoundException;
import com.smartcity.incident_management.exceptions.UnauthorizedException;
import com.smartcity.incident_management.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    public List<Notification> mesNotifications(Utilisateur utilisateur) {
        return notificationRepository.findByUtilisateurId(utilisateur.getId());
    }

    public Page<Notification> mesNotifications(Utilisateur utilisateur, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUtilisateurId(utilisateur.getId(), pageable);
    }
    
    public List<Notification> mesNotificationsNonLues(Utilisateur utilisateur) {
        return notificationRepository.findByUtilisateurIdAndLuFalse(utilisateur.getId());
    }
    
    public long nombreNotificationsNonLues(Utilisateur utilisateur) {
        return notificationRepository.countByUtilisateurIdAndLuFalse(utilisateur.getId());
    }
    
    public Notification marquerCommeLue(Long notificationId, Utilisateur utilisateur) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification non trouvée"));
        
        if (!notification.getUtilisateur().getId().equals(utilisateur.getId())) {
            throw new UnauthorizedException("Non autorisé");
        }
        
        notification.setLu(true);
        return notificationRepository.save(notification);
    }
    
    public void marquerToutesCommeLues(Utilisateur utilisateur) {
        List<Notification> notifications = notificationRepository.findByUtilisateurIdAndLuFalse(utilisateur.getId());
        notifications.forEach(n -> n.setLu(true));
        notificationRepository.saveAll(notifications);
    }
}

