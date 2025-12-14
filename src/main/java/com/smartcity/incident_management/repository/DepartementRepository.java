package com.smartcity.incident_management.repository;

import com.smartcity.incident_management.entities.Departement;
import com.smartcity.incident_management.enums.CategorieDepartement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartementRepository extends JpaRepository<Departement, Long> {
    Optional<Departement> findByNom(CategorieDepartement nom);
    boolean existsByNom(CategorieDepartement nom);
    List<Departement> findByActifTrue();
    List<Departement> findByActif(boolean actif);
}


