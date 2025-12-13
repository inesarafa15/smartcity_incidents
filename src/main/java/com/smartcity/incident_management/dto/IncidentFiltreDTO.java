package com.smartcity.incident_management.dto;

import com.smartcity.incident_management.enums.PrioriteIncident;
import com.smartcity.incident_management.enums.StatutIncident;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IncidentFiltreDTO {
    private StatutIncident statut;
    private PrioriteIncident priorite;
    private Long quartierId;
    private Long departementId;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private int page = 0;
    private int size = 10;
    private String sortBy = "dateCreation";
    private String sortDir = "DESC";
}


