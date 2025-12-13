package com.smartcity.incident_management.repository;

import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Utilisateur> findByRole(RoleType role);
    List<Utilisateur> findByDepartementId(Long departementId);
    List<Utilisateur> findByActifTrue();
}


