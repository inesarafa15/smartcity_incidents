package com.smartcity.incident_management.repository;

import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.enums.PrioriteIncident;
import com.smartcity.incident_management.enums.StatutIncident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByAuteurId(Long auteurId);
    Page<Incident> findByAuteurId(Long auteurId, Pageable pageable);
    List<Incident> findByAgentAssigneId(Long agentId);
    Page<Incident> findByAgentAssigneId(Long agentId, Pageable pageable);
    List<Incident> findByDepartementId(Long departementId);
    Page<Incident> findByDepartementId(Long departementId, Pageable pageable);
    List<Incident> findByQuartierId(Long quartierId);
    List<Incident> findByStatut(StatutIncident statut);
    List<Incident> findByPriorite(PrioriteIncident priorite);
    List<Incident> findByStatutAndDepartementId(StatutIncident statut, Long departementId);
    
    @Query("SELECT i FROM Incident i WHERE " +
           "(:statut IS NULL OR i.statut = :statut) AND " +
           "(:priorite IS NULL OR i.priorite = :priorite) AND " +
           "(:quartierId IS NULL OR i.quartier.id = :quartierId) AND " +
           "(:departementId IS NULL OR i.departement.id = :departementId) AND " +
           "(:dateDebut IS NULL OR i.dateCreation >= :dateDebut) AND " +
           "(:dateFin IS NULL OR i.dateCreation <= :dateFin)")
    Page<Incident> findWithFilters(
        @Param("statut") StatutIncident statut,
        @Param("priorite") PrioriteIncident priorite,
        @Param("quartierId") Long quartierId,
        @Param("departementId") Long departementId,
        @Param("dateDebut") LocalDateTime dateDebut,
        @Param("dateFin") LocalDateTime dateFin,
        Pageable pageable
    );
}


