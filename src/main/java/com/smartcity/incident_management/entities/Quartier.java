package com.smartcity.incident_management.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quartiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Quartier {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Le nom du quartier est obligatoire")
    @Size(min = 2, max = 100)
    @Column(nullable = false)
    private String nom;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @NotBlank(message = "Le code postal est obligatoire")
    @Size(min = 5, max = 10)
    @Column(name = "code_postal", nullable = false)
    private String codePostal;
    
    // Relation avec les incidents
    @OneToMany(mappedBy = "quartier", cascade = CascadeType.ALL)
    private List<Incident> incidents = new ArrayList<>();
}


