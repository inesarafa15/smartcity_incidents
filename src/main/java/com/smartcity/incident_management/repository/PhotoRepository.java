package com.smartcity.incident_management.repository;

import com.smartcity.incident_management.entities.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByIncidentId(Long incidentId);
    void deleteByIncidentId(Long incidentId);
}


