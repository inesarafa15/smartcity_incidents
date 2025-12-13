package com.smartcity.incident_management.entities;

import com.smartcity.incident_management.enums.RoleType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "utilisateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50)
    @Column(nullable = false)
    private String nom;
    
    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 50)
    @Column(nullable = false)
    private String prenom;
    
    @Email(message = "Email invalide")
    @NotBlank(message = "L'email est obligatoire")
    @Column(unique = true, nullable = false)
    private String email;
    
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Column(name = "mot_de_passe_hash", nullable = false)
    private String motDePasseHash;
    
    @Size(max = 20)
    private String telephone;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleType role;
    
    @Column(nullable = false)
    private boolean actif = true;
    
    // Relation avec Département (pour les agents municipaux)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departement_id")
    private Departement departement;
    
    // Relation avec les incidents signalés (en tant que citoyen)
    @OneToMany(mappedBy = "auteur", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Incident> incidentsSignales = new ArrayList<>();
    
    // Relation avec les incidents assignés (en tant qu'agent municipal)
    @OneToMany(mappedBy = "agentAssigne", cascade = CascadeType.ALL)
    private List<Incident> incidentsAssignes = new ArrayList<>();
    
    // Relation avec les notifications
    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();
    
    // Relation avec les rapports générés (en tant qu'administrateur)
    @OneToMany(mappedBy = "generePar", cascade = CascadeType.ALL)
    private List<Rapport> rapportsGeneres = new ArrayList<>();
}


