package com.smartcity.incident_management.services.citoyen;

import com.smartcity.incident_management.dto.IncidentDTO;
import com.smartcity.incident_management.entities.*;
import com.smartcity.incident_management.enums.TypePhoto;
import com.smartcity.incident_management.enums.StatutIncident;
import com.smartcity.incident_management.exceptions.BadRequestException;
import com.smartcity.incident_management.exceptions.ResourceNotFoundException;
import com.smartcity.incident_management.exceptions.UnauthorizedException;
import com.smartcity.incident_management.repository.DepartementRepository;
import com.smartcity.incident_management.repository.IncidentRepository;
import com.smartcity.incident_management.repository.PhotoRepository;
import com.smartcity.incident_management.repository.QuartierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class IncidentCitoyenService {
    
    @Autowired
    private IncidentRepository incidentRepository;
    
    @Autowired
    private DepartementRepository departementRepository;
    
    @Autowired
    private QuartierRepository quartierRepository;
    
    @Autowired
    private PhotoRepository photoRepository;
    
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
    @Value("${app.upload.max-files:5}")
    private int maxFiles;
    
    @Value("${app.upload.max-file-size:10485760}") // 10MB par défaut
    private long maxFileSize;
    
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "webp"
    );
    
    public Incident signalerIncident(Utilisateur citoyen, IncidentDTO dto) throws IOException {
        // Validation du département
        Departement departement = departementRepository.findById(dto.getDepartementId())
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé"));
        
        // Validation du quartier
        Quartier quartier = quartierRepository.findById(dto.getQuartierId())
                .orElseThrow(() -> new ResourceNotFoundException("Quartier non trouvé"));
        
        // Créer l'incident
        Incident incident = new Incident();
        incident.setTitre(dto.getTitre());
        incident.setDescription(dto.getDescription());
        incident.setPriorite(dto.getPriorite());
        incident.setAdresseTextuelle(dto.getAdresseTextuelle());
        
        // Validation et assignation de la position GPS (obligatoire)
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            incident.setLatitude(java.math.BigDecimal.valueOf(dto.getLatitude()));
            incident.setLongitude(java.math.BigDecimal.valueOf(dto.getLongitude()));
        } else {
            throw new BadRequestException("La position GPS est obligatoire. Veuillez sélectionner un point sur la carte.");
        }
        
        incident.setAuteur(citoyen);
        incident.setDepartement(departement);
        incident.setQuartier(quartier);
        incident.setStatut(StatutIncident.SIGNALE);
        
        // Sauvegarder l'incident
        Incident savedIncident = incidentRepository.save(incident);
        
        // Gérer l'upload des photos avec validation
        if (dto.getPhotos() != null && !dto.getPhotos().isEmpty()) {
            // Valider les photos avant de les sauvegarder
            validerPhotos(dto.getPhotos());
            sauvegarderPhotos(savedIncident, dto.getPhotos());
        }
        
        return savedIncident;
    }
    
    /**
     * Valide les fichiers photos avant l'upload
     */
    private void validerPhotos(List<MultipartFile> fichiers) {
        // Compter le nombre de fichiers non vides
        long nbFichiersValides = fichiers.stream()
            .filter(f -> f != null && !f.isEmpty())
            .count();
        
        // Vérifier le nombre maximum de fichiers
        if (nbFichiersValides > maxFiles) {
            throw new BadRequestException(
                String.format("Vous ne pouvez télécharger que %d photos maximum. Vous avez sélectionné %d photos.", 
                    maxFiles, nbFichiersValides)
            );
        }
        
        // Valider chaque fichier individuellement
        for (MultipartFile fichier : fichiers) {
            if (fichier != null && !fichier.isEmpty()) {
                validerFichier(fichier);
            }
        }
    }
    
    /**
     * Valide un fichier individuel
     */
    private void validerFichier(MultipartFile fichier) {
        String nomFichier = fichier.getOriginalFilename();
        
        // Vérifier la taille du fichier
        if (fichier.getSize() > maxFileSize) {
            throw new BadRequestException(
                String.format("Le fichier '%s' dépasse la taille maximale de %dMB", 
                    nomFichier, maxFileSize / 1024 / 1024)
            );
        }
        
        // Vérifier le type MIME
        String contentType = fichier.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                String.format("Le fichier '%s' n'est pas une image valide. Formats acceptés: JPEG, PNG, GIF, WebP", 
                    nomFichier)
            );
        }
        
        // Vérifier l'extension du fichier
        if (nomFichier != null && nomFichier.contains(".")) {
            String extension = nomFichier.substring(nomFichier.lastIndexOf(".") + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new BadRequestException(
                    String.format("Extension de fichier non valide pour '%s'. Extensions acceptées: %s", 
                        nomFichier, String.join(", ", ALLOWED_EXTENSIONS))
                );
            }
        } else {
            throw new BadRequestException("Le fichier doit avoir une extension valide");
        }
    }
    
    /**
     * Sauvegarde les photos sur le disque et en base de données
     */
    private void sauvegarderPhotos(Incident incident, List<MultipartFile> fichiers) throws IOException {
        // Créer le dossier d'upload s'il n'existe pas
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        int compteur = 0;
        for (MultipartFile fichier : fichiers) {
            if (fichier != null && !fichier.isEmpty() && compteur < maxFiles) {
                // Générer un nom de fichier sécurisé et unique
                String nomFichierSecurise = genererNomFichierSecurise(fichier.getOriginalFilename(), compteur);
                Path cheminFichier = uploadPath.resolve(nomFichierSecurise);
                
                // Copier le fichier sur le disque
                Files.copy(fichier.getInputStream(), cheminFichier, StandardCopyOption.REPLACE_EXISTING);
                
                // Créer l'entité Photo
                Photo photo = new Photo();
                photo.setTypePhoto(TypePhoto.CREATION);
                photo.setCheminFichier("uploads/" + nomFichierSecurise);
                photo.setTypeMime(fichier.getContentType());
                photo.setTailleKo(fichier.getSize() / 1024);
                photo.setIncident(incident);
                
                // Sauvegarder en base de données
                photoRepository.save(photo);
                compteur++;
            }
        }
    }
    
    /**
     * Génère un nom de fichier sécurisé et unique
     */
    private String genererNomFichierSecurise(String nomOriginal, int compteur) {
        if (nomOriginal == null || nomOriginal.isEmpty()) {
            return System.currentTimeMillis() + "_" + compteur + ".jpg";
        }
        
        // Extraire l'extension
        String extension = "";
        int indexPoint = nomOriginal.lastIndexOf(".");
        if (indexPoint > 0) {
            extension = nomOriginal.substring(indexPoint).toLowerCase();
        }
        
        // Nettoyer le nom de fichier (supprimer les caractères dangereux)
        String nomNettoye = nomOriginal
            .substring(0, indexPoint > 0 ? indexPoint : nomOriginal.length())
            .replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Limiter la longueur du nom
        if (nomNettoye.length() > 50) {
            nomNettoye = nomNettoye.substring(0, 50);
        }
        
        // Construire le nom final: timestamp_compteur_nomOriginal.extension
        return System.currentTimeMillis() + "_" + compteur + "_" + nomNettoye + extension;
    }
    
    public Page<Incident> mesIncidents(Utilisateur citoyen, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return incidentRepository.findByAuteurId(citoyen.getId(), pageable);
    }

    public Page<Incident> mesIncidentsFiltres(Utilisateur citoyen, int page, int size, String sortBy, String sortDir, 
                                             String statutStr, Long departementId, String dateFilter) {
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        StatutIncident statut = null;
        if (statutStr != null && !statutStr.isEmpty()) {
            try {
                statut = StatutIncident.valueOf(statutStr);
            } catch (IllegalArgumentException e) {
                // Ignore invalid status
            }
        }
        
        java.time.LocalDateTime dateDebut = null;
        java.time.LocalDateTime dateFin = null;
        
        if ("today".equals(dateFilter)) {
            dateDebut = java.time.LocalDate.now().atStartOfDay();
            dateFin = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
        } else if ("week".equals(dateFilter)) {
            dateDebut = java.time.LocalDate.now().minusDays(7).atStartOfDay();
            dateFin = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
        }
        
        return incidentRepository.findByAuteurIdAndFilters(citoyen.getId(), statut, departementId, dateDebut, dateFin, pageable);
    }
    
    public Incident consulterIncident(Long incidentId, Utilisateur citoyen) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));
        
        // Vérifier que l'utilisateur est bien l'auteur
        if (!incident.getAuteur().getId().equals(citoyen.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à consulter cet incident");
        }
        
        // Forcer le chargement des relations lazy
        if (incident.getQuartier() != null) {
            incident.getQuartier().getNom();
        }
        if (incident.getDepartement() != null) {
            incident.getDepartement().getNom();
        }
        if (incident.getAgentAssigne() != null) {
            incident.getAgentAssigne().getNom();
        }
        if (incident.getPhotos() != null) {
            incident.getPhotos().size();
        }
        
        return incident;
    }
    
    public List<Incident> mesIncidentsList(Utilisateur citoyen) {
        return incidentRepository.findByAuteurId(citoyen.getId());
    }

    public Incident soumettreFeedback(Long incidentId, Utilisateur citoyen, Boolean satisfait, Integer note, String commentaire) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));
        
        // Vérifier que l'utilisateur est bien l'auteur
        if (!incident.getAuteur().getId().equals(citoyen.getId())) {
            throw new UnauthorizedException("Non autorisé");
        }
        
        // Vérifier que l'incident est résolu
        if (incident.getStatut() != StatutIncident.RESOLU) {
            throw new UnauthorizedException("L'incident doit être en statut RESOLU pour donner un feedback");
        }
        
        // Mettre à jour le feedback
        incident.setFeedbackSatisfait(satisfait);
        incident.setFeedbackNote(note);
        incident.setFeedbackCommentaire(commentaire != null ? commentaire.trim() : null);
        incident.setDateFeedback(java.time.LocalDateTime.now());
        incident.setDateDerniereMiseAJour(java.time.LocalDateTime.now());
        
        return incidentRepository.save(incident);
    }
}

    public Incident modifierIncident(Long id, Utilisateur citoyen, IncidentDTO dto) throws IOException {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));

        if (!incident.getAuteur().getId().equals(citoyen.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à modifier cet incident");
        }

        if (incident.getStatut() == StatutIncident.RESOLU || incident.getStatut() == StatutIncident.CLOTURE) {
            throw new com.smartcity.incident_management.exceptions.BadRequestException("Impossible de modifier un incident résolu ou clôturé");
        }

        Departement departement = departementRepository.findById(dto.getDepartementId())
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé"));

        Quartier quartier = quartierRepository.findById(dto.getQuartierId())
                .orElseThrow(() -> new ResourceNotFoundException("Quartier non trouvé"));

        incident.setTitre(dto.getTitre());
        incident.setDescription(dto.getDescription());
        incident.setPriorite(dto.getPriorite());
        incident.setAdresseTextuelle(dto.getAdresseTextuelle());
        incident.setDepartement(departement);
        incident.setQuartier(quartier);
        
        // Mise à jour de la position si fournie
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            incident.setLatitude(java.math.BigDecimal.valueOf(dto.getLatitude()));
            incident.setLongitude(java.math.BigDecimal.valueOf(dto.getLongitude()));
        }

        incident.setDateDerniereMiseAJour(java.time.LocalDateTime.now());

        Incident savedIncident = incidentRepository.save(incident);

        // Gérer l'ajout de nouvelles photos
        if (dto.getPhotos() != null && !dto.getPhotos().isEmpty()) {
            sauvegarderPhotos(savedIncident, dto.getPhotos());
        }

        return savedIncident;
    }
}

