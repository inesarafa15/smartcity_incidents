package com.smartcity.incident_management.repository;

import com.smartcity.incident_management.entities.Rapport;
import com.smartcity.incident_management.enums.TypeRapport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RapportRepository extends JpaRepository<Rapport, Long> {
    List<Rapport> findByGenereParId(Long adminId);
    List<Rapport> findByType(TypeRapport type);
}


