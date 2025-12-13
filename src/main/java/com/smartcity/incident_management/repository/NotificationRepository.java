package com.smartcity.incident_management.repository;

import com.smartcity.incident_management.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUtilisateurId(Long utilisateurId);
    List<Notification> findByUtilisateurIdAndLuFalse(Long utilisateurId);
    List<Notification> findByIncidentId(Long incidentId);
    long countByUtilisateurIdAndLuFalse(Long utilisateurId);
}


