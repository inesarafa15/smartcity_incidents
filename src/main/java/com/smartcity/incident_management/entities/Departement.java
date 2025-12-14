package com.smartcity.incident_management.entities;

import com.smartcity.incident_management.enums.CategorieDepartement;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Departement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private CategorieDepartement nom;
    
    @NotBlank(message = "La description est obligatoire")
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private boolean actif = true;
    
    // Relation avec les agents municipaux
    @OneToMany(mappedBy = "departement", cascade = CascadeType.ALL)
    private List<Utilisateur> agents = new ArrayList<>();
    
    // Relation avec les incidents
    @OneToMany(mappedBy = "departement", cascade = CascadeType.ALL)
    private List<Incident> incidents = new ArrayList<>();
}


