package com.smartcity.incident_management.repository;

import com.smartcity.incident_management.entities.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.incident WHERE n.utilisateur.id = :utilisateurId ORDER BY n.dateEnvoi DESC")
    List<Notification> findByUtilisateurId(@Param("utilisateurId") Long utilisateurId);

    @Query(value = "SELECT n FROM Notification n LEFT JOIN FETCH n.incident WHERE n.utilisateur.id = :utilisateurId ORDER BY n.dateEnvoi DESC",
           countQuery = "SELECT count(n) FROM Notification n WHERE n.utilisateur.id = :utilisateurId")
    Page<Notification> findByUtilisateurId(@Param("utilisateurId") Long utilisateurId, Pageable pageable);
    
    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.incident WHERE n.utilisateur.id = :utilisateurId AND n.lu = false ORDER BY n.dateEnvoi DESC")
    List<Notification> findByUtilisateurIdAndLuFalse(@Param("utilisateurId") Long utilisateurId);
    
    List<Notification> findByIncidentId(Long incidentId);
    
    long countByUtilisateurIdAndLuFalse(Long utilisateurId);
}


