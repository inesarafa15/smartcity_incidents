package com.smartcity.incident_management.controllers;

import com.smartcity.incident_management.dto.IncidentDTO;
import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.security.SecurityUtils;
import com.smartcity.incident_management.services.citoyen.IncidentCitoyenService;
import com.smartcity.incident_management.services.utilisateur.DepartementService;
import com.smartcity.incident_management.services.utilisateur.QuartierService;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;


@Controller
@RequestMapping("/citoyen")
@PreAuthorize("hasRole('CITOYEN')")
public class CitoyenController {
    
    @Autowired
    private IncidentCitoyenService incidentCitoyenService;
    
    @Autowired
    private DepartementService departementService;
    
    @Autowired
    private QuartierService quartierService;
    
    @Autowired
    private com.smartcity.incident_management.services.utilisateur.NotificationService notificationService;
    
    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Long departementId,
            @RequestParam(required = false) String dateFilter,
            @RequestParam(defaultValue = "dateCreation") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Model model) {
        Utilisateur citoyen = SecurityUtils.getCurrentUser();
        
        // Récupérer les incidents récents avec filtres
        Page<Incident> incidentsRecents = incidentCitoyenService.mesIncidentsFiltres(citoyen, page, size, sortBy, sortDir, statut, departementId, dateFilter);
        
        // Récupérer les notifications non lues
        var notifications = notificationService.mesNotificationsNonLues(citoyen);
        long nbNotificationsNonLues = notificationService.nombreNotificationsNonLues(citoyen);
        
        // Statistiques
        Page<Incident> tousIncidents = incidentCitoyenService.mesIncidents(citoyen, 0, 1000, "dateCreation", "DESC");
        long totalIncidents = tousIncidents.getTotalElements();
        long incidentsEnCours = tousIncidents.getContent().stream()
                .filter(i -> i.getStatut().name().equals("SIGNALE") || 
                            i.getStatut().name().equals("PRIS_EN_CHARGE") || 
                            i.getStatut().name().equals("EN_RESOLUTION"))
                .count();
        long incidentsResolus = tousIncidents.getContent().stream()
                .filter(i -> i.getStatut().name().equals("RESOLU") || i.getStatut().name().equals("CLOTURE"))
                .count();
        
        int tauxResolution = totalIncidents > 0 ? (int) ((incidentsResolus * 100) / totalIncidents) : 0;
        
        model.addAttribute("incidents", incidentsRecents);
        model.addAttribute("notifications", notifications);
        model.addAttribute("nbNotificationsNonLues", nbNotificationsNonLues);
        model.addAttribute("totalIncidents", totalIncidents);
        model.addAttribute("incidentsEnCours", incidentsEnCours);
        model.addAttribute("incidentsResolus", incidentsResolus);
        model.addAttribute("tauxResolution", tauxResolution);
        
        // Filtres
        model.addAttribute("departements", departementService.findAll());
        model.addAttribute("statutFilter", statut);
        model.addAttribute("departementFilter", departementId);
        model.addAttribute("dateFilter", dateFilter);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        
        return "citoyen/dashboard";
    }
    
    @GetMapping("/incidents")
    public String mesIncidents(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String priorite,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateCreation") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Model model) {
        
        Utilisateur citoyen = SecurityUtils.getCurrentUser();
        Page<Incident> incidents = incidentCitoyenService.mesIncidents(citoyen, page, size, sortBy, sortDir);
        
        // Filtrer côté serveur si nécessaire (pour simplifier, on filtre après récupération)
        if (statut != null && !statut.isEmpty()) {
            incidents = (Page<Incident>) incidents.map(incident -> {
                if (incident.getStatut().name().equals(statut)) {
                    return incident;
                }
                return null;
            }).filter(incident -> incident != null);
        }
        
        if (priorite != null && !priorite.isEmpty()) {
            incidents = (Page<Incident>) incidents.map(incident -> {
                if (incident != null && incident.getPriorite().name().equals(priorite)) {
                    return incident;
                }
                return null;
            }).filter(incident -> incident != null);
        }
        
        model.addAttribute("incidents", incidents);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", incidents.getTotalPages());
        model.addAttribute("statut", statut);
        model.addAttribute("priorite", priorite);
        
        return "citoyen/incidents";
    }
    
    @GetMapping("/incidents/nouveau")
    public String nouveauIncidentForm(Model model) {
        model.addAttribute("incidentDTO", new IncidentDTO());
        model.addAttribute("departements", departementService.findAll());
        model.addAttribute("quartiers", quartierService.findAll());
        return "citoyen/nouveau-incident";
    }
    
    @PostMapping("/incidents/nouveau")
    public String signalerIncident(
            @Valid @ModelAttribute IncidentDTO dto,
            BindingResult result,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (result.hasErrors()) {
            return "citoyen/nouveau-incident";
        }

        try {
            dto.setPhotos(photos);

            Utilisateur citoyen = SecurityUtils.getCurrentUser();
            incidentCitoyenService.signalerIncident(citoyen, dto);

            redirectAttributes.addFlashAttribute("success", "Incident signalé avec succès !");
            return "redirect:/citoyen/incidents";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/citoyen/incidents/nouveau";
        }
    }

    
    @GetMapping("/incidents/{id}")
    public String consulterIncident(@PathVariable Long id, Model model) {
        Utilisateur citoyen = SecurityUtils.getCurrentUser();
        Incident incident = incidentCitoyenService.consulterIncident(id, citoyen);
        model.addAttribute("incident", incident);
        return "citoyen/incident-details";
    }

    @GetMapping("/incidents/{id}/modifier")
    public String modifierIncidentForm(@PathVariable Long id, Model model) {
        Utilisateur citoyen = SecurityUtils.getCurrentUser();
        Incident incident = incidentCitoyenService.consulterIncident(id, citoyen);
        
        IncidentDTO dto = new IncidentDTO();
        dto.setTitre(incident.getTitre());
        dto.setDescription(incident.getDescription());
        dto.setPriorite(incident.getPriorite());
        dto.setAdresseTextuelle(incident.getAdresseTextuelle());
        dto.setDepartementId(incident.getDepartement().getId());
        dto.setQuartierId(incident.getQuartier().getId());
        if (incident.getLatitude() != null) dto.setLatitude(incident.getLatitude().doubleValue());
        if (incident.getLongitude() != null) dto.setLongitude(incident.getLongitude().doubleValue());

        model.addAttribute("incident", incident);
        model.addAttribute("incidentDTO", dto);
        model.addAttribute("departements", departementService.findAll());
        model.addAttribute("quartiers", quartierService.findAll());
        return "citoyen/modifier-incident";
    }

    @PostMapping("/incidents/{id}/modifier")
    public String modifierIncident(@PathVariable Long id,
                                   @Valid @ModelAttribute IncidentDTO dto,
                                   BindingResult result,
                                   @RequestParam(required = false) List<Long> photosToDelete,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        if (result.hasErrors()) {
            Utilisateur citoyen = SecurityUtils.getCurrentUser();
            Incident incident = incidentCitoyenService.consulterIncident(id, citoyen);
            model.addAttribute("incident", incident);
            model.addAttribute("departements", departementService.findAll());
            model.addAttribute("quartiers", quartierService.findAll());
            return "citoyen/modifier-incident";
        }
        
        try {
            Utilisateur citoyen = SecurityUtils.getCurrentUser();
            incidentCitoyenService.modifierIncident(id, citoyen, dto, photosToDelete);
            redirectAttributes.addFlashAttribute("success", "Incident modifié avec succès !");
            return "redirect:/citoyen/incidents";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/citoyen/incidents/" + id + "/modifier";
        }
    }

    @PostMapping("/incidents/{id}/feedback")
    public String soumettreFeedback(@PathVariable Long id,
                                   @RequestParam(name = "satisfait", required = false) Boolean satisfait,
                                   @RequestParam(name = "note", required = false) Integer note,
                                   @RequestParam(name = "commentaire", required = false) String commentaire,
                                   RedirectAttributes redirectAttributes) {
        try {
            Utilisateur citoyen = SecurityUtils.getCurrentUser();
            incidentCitoyenService.soumettreFeedback(id, citoyen, satisfait, note, commentaire);
            redirectAttributes.addFlashAttribute("success", "Feedback enregistré, merci !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/citoyen/incidents/" + id;
    }
}
