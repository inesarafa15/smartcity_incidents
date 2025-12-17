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
           "(:agentId IS NULL OR i.agentAssigne.id = :agentId) AND " +
           "(:dateDebut IS NULL OR i.dateCreation >= :dateDebut) AND " +
           "(:dateFin IS NULL OR i.dateCreation <= :dateFin) AND " +
           "(:keyword IS NULL OR LOWER(i.titre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(i.adresseTextuelle) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Incident> findWithFiltersAndKeyword(
        @Param("statut") StatutIncident statut,
        @Param("priorite") PrioriteIncident priorite,
        @Param("quartierId") Long quartierId,
        @Param("departementId") Long departementId,
        @Param("agentId") Long agentId,
        @Param("dateDebut") LocalDateTime dateDebut,
        @Param("dateFin") LocalDateTime dateFin,
        @Param("keyword") String keyword,
        Pageable pageable
    );

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

    @Query("SELECT i FROM Incident i WHERE " +
           "i.agentAssigne.id = :agentId AND " +
           "(:statut IS NULL OR i.statut = :statut) AND " +
           "(:priorite IS NULL OR i.priorite = :priorite) AND " +
           "(:departementId IS NULL OR i.departement.id = :departementId)")
    Page<Incident> findAssignedToAgentWithFilters(
        @Param("agentId") Long agentId,
        @Param("statut") StatutIncident statut,
        @Param("priorite") PrioriteIncident priorite,
        @Param("departementId") Long departementId,
        Pageable pageable
    );
    @Query("SELECT i FROM Incident i WHERE " +
            "i.auteur.id = :auteurId AND " +
            "(:statut IS NULL OR i.statut = :statut) AND " +
            "(:priorite IS NULL OR i.priorite = :priorite)")
     Page<Incident> findByAuteurIdAndFilters(
          @Param("auteurId") Long auteurId,
          @Param("statut") StatutIncident statut,
          @Param("priorite") PrioriteIncident priorite,
          Pageable pageable
     );
     
     // Méthode pour le dashboard (avec plus de filtres)
     @Query("SELECT i FROM Incident i WHERE " +
            "i.auteur.id = :auteurId AND " +
            "(:statut IS NULL OR i.statut = :statut) AND " +
            "(:departementId IS NULL OR i.departement.id = :departementId) AND " +
            "(:dateDebut IS NULL OR i.dateCreation >= :dateDebut) AND " +
            "(:dateFin IS NULL OR i.dateCreation <= :dateFin)")
     Page<Incident> findByAuteurIdAndFiltersDashboard(
         @Param("auteurId") Long auteurId,
         @Param("statut") StatutIncident statut,
         @Param("departementId") Long departementId,
         @Param("dateDebut") LocalDateTime dateDebut,
         @Param("dateFin") LocalDateTime dateFin,
         Pageable pageable
     );
    @Query("SELECT DISTINCT i FROM Incident i " +
    	       "LEFT JOIN FETCH i.departement d " +
    	       "LEFT JOIN FETCH i.quartier q " +
    	       "LEFT JOIN FETCH i.agentAssigne a " +
    	       "LEFT JOIN FETCH i.auteur u " +
    	       "WHERE d.id = :departementId " +
    	       "ORDER BY i.dateCreation DESC")
    	List<Incident> findIncidentsCompletsByDepartementId(@Param("departementId") Long departementId);
    
}


