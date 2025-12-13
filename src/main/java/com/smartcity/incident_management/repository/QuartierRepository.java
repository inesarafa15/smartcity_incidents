package com.smartcity.incident_management.repository;

import com.smartcity.incident_management.entities.Quartier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuartierRepository extends JpaRepository<Quartier, Long> {
    Optional<Quartier> findByNom(String nom);
    List<Quartier> findByCodePostal(String codePostal);
}


