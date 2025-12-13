package com.smartcity.incident_management.entities;

import com.smartcity.incident_management.enums.TypeRapport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rapports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rapport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Le titre est obligatoire")
    @Column(nullable = false, length = 200)
    private String titre;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeRapport type;
    
    @Column(name = "date_generation", nullable = false, updatable = false)
    private LocalDateTime dateGeneration = LocalDateTime.now();
    
    @NotNull
    @Column(name = "periode_debut", nullable = false)
    private LocalDate periodeDebut;
    
    @NotNull
    @Column(name = "periode_fin", nullable = false)
    private LocalDate periodeFin;
    
    @Column(name = "contenu_ou_chemin_fichier", columnDefinition = "TEXT")
    private String contenuOuCheminFichier;
    
    // Relation avec l'administrateur qui a généré le rapport
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genere_par_id", nullable = false)
    private Utilisateur generePar;
    
    // Relation avec les incidents analysés
    @ManyToMany
    @JoinTable(
        name = "rapport_incidents",
        joinColumns = @JoinColumn(name = "rapport_id"),
        inverseJoinColumns = @JoinColumn(name = "incident_id")
    )
    private List<Incident> incidentsAnalyses = new ArrayList<>();
}


