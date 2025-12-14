package com.smartcity.incident_management.dto;

import com.smartcity.incident_management.enums.RoleType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurFiltreDTO {
    private RoleType role;
    private Long departementId;
    private Boolean actif;
    private String recherche; // Recherche textuelle (nom, prénom, email)
    private int page = 0;
    private int size = 10;
    private String sortBy = "nom";
    private String sortDir = "ASC";
}

