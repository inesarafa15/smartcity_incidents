package com.smartcity.incident_management.entities;

import com.smartcity.incident_management.enums.PrioriteIncident;
import com.smartcity.incident_management.enums.StatutIncident;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le titre est obligatoire")
    @Column(nullable = false, length = 200)
    private String titre;

    @NotBlank(message = "La description est obligatoire")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutIncident statut = StatutIncident.SIGNALE;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrioriteIncident priorite = PrioriteIncident.MOYENNE;

    @NotBlank(message = "L'adresse est obligatoire")
    @Column(name = "adresse_textuelle", nullable = false)
    private String adresseTextuelle;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_derniere_mise_a_jour")
    private LocalDateTime dateDerniereMiseAJour = LocalDateTime.now();

    // Relation avec le citoyen auteur
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id", nullable = false)
    private Utilisateur auteur;

    // Relation avec l'agent assigné
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_assigne_id")
    private Utilisateur agentAssigne;

    // Relation avec le département
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departement_id", nullable = false)
    private Departement departement;

    // Relation avec le quartier
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quartier_id", nullable = false)
    private Quartier quartier;

    // Relation avec les photos
    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Photo> photos = new ArrayList<>();

    // Relation avec les notifications liées à l'incident
    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notificationsIncident = new ArrayList<>();

    // Relation avec les rapports
    @ManyToMany(mappedBy = "incidentsAnalyses")
    private List<Rapport> rapports = new ArrayList<>();

    @PreUpdate
    public void preUpdate() {
        this.dateDerniereMiseAJour = LocalDateTime.now();
    }
}