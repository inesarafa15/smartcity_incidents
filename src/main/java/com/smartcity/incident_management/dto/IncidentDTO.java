package com.smartcity.incident_management.dto;

import com.smartcity.incident_management.enums.PrioriteIncident;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IncidentDTO {
    
    @NotBlank(message = "Le titre est obligatoire")
    private String titre;
    
    @NotBlank(message = "La description est obligatoire")
    private String description;
    
    @NotNull(message = "La priorité est obligatoire")
    private PrioriteIncident priorite;
    
    @NotBlank(message = "L'adresse est obligatoire")
    private String adresseTextuelle;
    
    private Double latitude;
    private Double longitude;
    
    @NotNull(message = "Le département est obligatoire")
    private Long departementId;
    
    @NotNull(message = "Le quartier est obligatoire")
    private Long quartierId;
    
    private List<MultipartFile> photos;
}


