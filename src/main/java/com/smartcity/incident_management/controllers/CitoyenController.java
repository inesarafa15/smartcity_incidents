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
            Model model) {
        Utilisateur citoyen = SecurityUtils.getCurrentUser();
        
        // Récupérer les incidents récents
        Page<Incident> incidentsRecents = incidentCitoyenService.mesIncidents(citoyen, page, size, "dateCreation", "DESC");
        
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
                .filter(i -> i.getStatut().name().equals("RESOLU"))
                .count();
        
        model.addAttribute("incidents", incidentsRecents);
        model.addAttribute("notifications", notifications);
        model.addAttribute("nbNotificationsNonLues", nbNotificationsNonLues);
        model.addAttribute("totalIncidents", totalIncidents);
        model.addAttribute("incidentsEnCours", incidentsEnCours);
        model.addAttribute("incidentsResolus", incidentsResolus);
        
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
