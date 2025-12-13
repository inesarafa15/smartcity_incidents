package com.smartcity.incident_management.dto;

import com.smartcity.incident_management.enums.TypeRapport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RapportDTO {
    
    @NotBlank(message = "Le titre est obligatoire")
    private String titre;
    
    @NotNull(message = "Le type de rapport est obligatoire")
    private TypeRapport type;
    
    @NotNull(message = "La période de début est obligatoire")
    private LocalDate periodeDebut;
    
    @NotNull(message = "La période de fin est obligatoire")
    private LocalDate periodeFin;
    
    private Long quartierId;
    private Long departementId;
}


