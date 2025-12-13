package com.smartcity.incident_management.services.citoyen;

import com.smartcity.incident_management.dto.IncidentDTO;
import com.smartcity.incident_management.entities.*;
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
        incident.setLatitude(dto.getLatitude());
        incident.setLongitude(dto.getLongitude());
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
    
    public Incident consulterIncident(Long incidentId, Utilisateur citoyen) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));
        
        if (!incident.getAuteur().getId().equals(citoyen.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à consulter cet incident");
        }
        
        return incident;
    }
    
    public List<Incident> mesIncidentsList(Utilisateur citoyen) {
        return incidentRepository.findByAuteurId(citoyen.getId());
    }
}

