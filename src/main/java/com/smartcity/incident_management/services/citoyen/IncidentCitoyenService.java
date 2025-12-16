package com.smartcity.incident_management.services.citoyen;

import com.smartcity.incident_management.dto.IncidentDTO;
import com.smartcity.incident_management.entities.*;
import com.smartcity.incident_management.enums.TypePhoto;
import com.smartcity.incident_management.enums.StatutIncident;
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
    
    public Incident signalerIncident(Utilisateur citoyen, IncidentDTO dto) throws IOException {
        Departement departement = departementRepository.findById(dto.getDepartementId())
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé"));
        
        Quartier quartier = quartierRepository.findById(dto.getQuartierId())
                .orElseThrow(() -> new ResourceNotFoundException("Quartier non trouvé"));
        
        Incident incident = new Incident();
        incident.setTitre(dto.getTitre());
        incident.setDescription(dto.getDescription());
        incident.setPriorite(dto.getPriorite());
        incident.setAdresseTextuelle(dto.getAdresseTextuelle());
        
        // Position GPS obtenue depuis la carte (obligatoire)
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            incident.setLatitude(java.math.BigDecimal.valueOf(dto.getLatitude()));
            incident.setLongitude(java.math.BigDecimal.valueOf(dto.getLongitude()));
        } else {
            throw new com.smartcity.incident_management.exceptions.BadRequestException("La position GPS est obligatoire. Veuillez sélectionner un point sur la carte.");
        }
        
        incident.setAuteur(citoyen);
        incident.setDepartement(departement);
        incident.setQuartier(quartier);
        incident.setStatut(StatutIncident.SIGNALE);
        
        Incident savedIncident = incidentRepository.save(incident);
        
        // Gérer l'upload des photos
        if (dto.getPhotos() != null && !dto.getPhotos().isEmpty()) {
            sauvegarderPhotos(savedIncident, dto.getPhotos());
        }
        
        return savedIncident;
    }
    
    private void sauvegarderPhotos(Incident incident, List<MultipartFile> fichiers) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        for (MultipartFile fichier : fichiers) {
            if (!fichier.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + fichier.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(fichier.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                Photo photo = new Photo();
                photo.setTypePhoto(TypePhoto.CREATION);
                photo.setCheminFichier("uploads/" + fileName);
                photo.setTypeMime(fichier.getContentType());
                photo.setTailleKo(fichier.getSize() / 1024);
                photo.setIncident(incident);
                
                photoRepository.save(photo);
            }
        }
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
        
        if (!incident.getAuteur().getId().equals(citoyen.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à consulter cet incident");
        }
        
        // Charger les relations nécessaires
        if (incident.getQuartier() != null) {
            incident.getQuartier().getNom(); // Force le chargement
        }
        if (incident.getDepartement() != null) {
            incident.getDepartement().getNom(); // Force le chargement
        }
        if (incident.getAgentAssigne() != null) {
            incident.getAgentAssigne().getNom(); // Force le chargement
        }
        if (incident.getPhotos() != null) {
            incident.getPhotos().size(); // Force le chargement
        }
        
        return incident;
    }
    
    public List<Incident> mesIncidentsList(Utilisateur citoyen) {
        return incidentRepository.findByAuteurId(citoyen.getId());
    }

    public Incident soumettreFeedback(Long incidentId, Utilisateur citoyen, Boolean satisfait, Integer note, String commentaire) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));
        if (!incident.getAuteur().getId().equals(citoyen.getId())) {
            throw new UnauthorizedException("Non autorisé");
        }
        if (incident.getStatut() != StatutIncident.RESOLU) {
            throw new UnauthorizedException("L'incident doit être en statut RESOLU pour donner un feedback");
        }
        incident.setFeedbackSatisfait(satisfait);
        incident.setFeedbackNote(note);
        incident.setFeedbackCommentaire(commentaire != null ? commentaire.trim() : null);
        incident.setDateFeedback(java.time.LocalDateTime.now());
        incident.setDateDerniereMiseAJour(java.time.LocalDateTime.now());
        return incidentRepository.save(incident);
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

