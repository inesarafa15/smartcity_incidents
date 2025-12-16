package com.smartcity.incident_management.entities;

import com.smartcity.incident_management.enums.TypePhoto;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Photo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type_photo")
    private TypePhoto typePhoto = TypePhoto.CREATION;
    
    @NotBlank(message = "Le chemin du fichier est obligatoire")
    @Column(name = "chemin_fichier", nullable = false)
    private String cheminFichier;
    
    @NotBlank(message = "Le type MIME est obligatoire")
    @Column(name = "type_mime", nullable = false)
    private String typeMime;
    
    @NotNull
    @Column(name = "taille_ko", nullable = false)
    private Long tailleKo;
    
    @Column(name = "date_upload", nullable = false, updatable = false)
    private LocalDateTime dateUpload = LocalDateTime.now();
    
    // Relation avec l'incident
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;
}


