package com.smartcity.incident_management.repository;

import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Utilisateur> findByRole(RoleType role);
    Page<Utilisateur> findByRole(RoleType role, Pageable pageable);
    List<Utilisateur> findByDepartementId(Long departementId);
    Page<Utilisateur> findByDepartementId(Long departementId, Pageable pageable);
    Page<Utilisateur> findByActif(Boolean actif, Pageable pageable);
    List<Utilisateur> findByActifTrue();
    Page<Utilisateur> findByRoleAndDepartementId(RoleType role, Long departementId, Pageable pageable);
    Page<Utilisateur> findByRoleAndActif(RoleType role, Boolean actif, Pageable pageable);
    Page<Utilisateur> findByDepartementIdAndActif(Long departementId, Boolean actif, Pageable pageable);
    Page<Utilisateur> findByRoleAndDepartementIdAndActif(RoleType role, Long departementId, Boolean actif, Pageable pageable);
    
    @Query("SELECT u FROM Utilisateur u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:departementId IS NULL OR u.departement.id = :departementId) AND " +
           "(:actif IS NULL OR u.actif = :actif) AND " +
           "(:recherche IS NULL OR LOWER(u.nom) LIKE LOWER(CONCAT('%', :recherche, '%')) OR " +
           "LOWER(u.prenom) LIKE LOWER(CONCAT('%', :recherche, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :recherche, '%')))")
    Page<Utilisateur> findWithFilters(
            @Param("role") RoleType role,
            @Param("departementId") Long departementId,
            @Param("actif") Boolean actif,
            @Param("recherche") String recherche,
            Pageable pageable
    );
}


