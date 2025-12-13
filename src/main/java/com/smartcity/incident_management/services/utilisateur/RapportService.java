package com.smartcity.incident_management.services.utilisateur;

import com.smartcity.incident_management.dto.RapportDTO;
import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.entities.Rapport;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.StatutIncident;
import com.smartcity.incident_management.enums.TypeRapport;
import com.smartcity.incident_management.exceptions.ResourceNotFoundException;
import com.smartcity.incident_management.repository.IncidentRepository;
import com.smartcity.incident_management.repository.RapportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RapportService {
    
    @Autowired
    private RapportRepository rapportRepository;
    
    @Autowired
    private IncidentRepository incidentRepository;
    
    public Rapport genererRapport(Utilisateur admin, RapportDTO dto) {
        Rapport rapport = new Rapport();
        rapport.setTitre(dto.getTitre());
        rapport.setType(dto.getType());
        rapport.setPeriodeDebut(dto.getPeriodeDebut());
        rapport.setPeriodeFin(dto.getPeriodeFin());
        rapport.setGenerePar(admin);
        
        // Filtrer les incidents selon le type de rapport
        List<Incident> incidents = filtrerIncidents(dto);
        rapport.setIncidentsAnalyses(incidents);
        
        // Générer le contenu du rapport
        String contenu = genererContenuRapport(rapport, incidents);
        rapport.setContenuOuCheminFichier(contenu);
        
        return rapportRepository.save(rapport);
    }
    
    private List<Incident> filtrerIncidents(RapportDTO dto) {
        List<Incident> incidents = incidentRepository.findAll();
        
        return incidents.stream()
                .filter(i -> !i.getDateCreation().toLocalDate().isBefore(dto.getPeriodeDebut()))
                .filter(i -> !i.getDateCreation().toLocalDate().isAfter(dto.getPeriodeFin()))
                .filter(i -> dto.getQuartierId() == null || i.getQuartier().getId().equals(dto.getQuartierId()))
                .filter(i -> dto.getDepartementId() == null || i.getDepartement().getId().equals(dto.getDepartementId()))
                .collect(Collectors.toList());
    }
    
    private String genererContenuRapport(Rapport rapport, List<Incident> incidents) {
        StringBuilder contenu = new StringBuilder();
        contenu.append("Rapport: ").append(rapport.getTitre()).append("\n");
        contenu.append("Type: ").append(rapport.getType()).append("\n");
        contenu.append("Période: ").append(rapport.getPeriodeDebut()).append(" - ").append(rapport.getPeriodeFin()).append("\n\n");
        contenu.append("Nombre total d'incidents: ").append(incidents.size()).append("\n\n");
        
        // Statistiques par statut
        contenu.append("Répartition par statut:\n");
        for (StatutIncident statut : StatutIncident.values()) {
            long count = incidents.stream().filter(i -> i.getStatut() == statut).count();
            contenu.append("- ").append(statut).append(": ").append(count).append("\n");
        }
        
        return contenu.toString();
    }
    
    public Rapport findById(Long id) {
        return rapportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé"));
    }
    
    public List<Rapport> findByAdmin(Long adminId) {
        return rapportRepository.findByGenereParId(adminId);
    }
    
    public List<Rapport> findAll() {
        return rapportRepository.findAll();
    }
}


