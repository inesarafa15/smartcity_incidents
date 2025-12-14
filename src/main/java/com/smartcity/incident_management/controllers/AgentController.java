package com.smartcity.incident_management.controllers;

import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.security.SecurityUtils;
import com.smartcity.incident_management.services.municipalite.IncidentMunicipaliteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/agent")
@PreAuthorize("hasRole('AGENT_MUNICIPAL')")
public class AgentController {
    
    @Autowired
    private IncidentMunicipaliteService incidentMunicipaliteService;
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Utilisateur agent = SecurityUtils.getCurrentUser();
        List<Incident> incidents = incidentMunicipaliteService.mesIncidentsAssignes(agent);
        model.addAttribute("incidentsAssignes", incidents);
        
        // Calculer les statistiques pour les graphiques
        Map<String, Long> parStatut = new HashMap<>();
        Map<String, Long> parPriorite = new HashMap<>();
        
        for (Incident incident : incidents) {
            // Par statut
            String statut = incident.getStatut().name();
            parStatut.put(statut, parStatut.getOrDefault(statut, 0L) + 1);
            
            // Par priorité
            String priorite = incident.getPriorite().name();
            parPriorite.put(priorite, parPriorite.getOrDefault(priorite, 0L) + 1);
        }
        
        model.addAttribute("incidentsParStatut", parStatut);
        model.addAttribute("incidentsParPriorite", parPriorite);
        
        return "agent/dashboard";
    }
    
    @GetMapping("/incidents")
    public String incidentsDepartement(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateCreation") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Model model) {
        
        Utilisateur agent = SecurityUtils.getCurrentUser();
        Page<Incident> incidents = incidentMunicipaliteService.incidentsDuDepartement(agent, page, size, sortBy, sortDir);
        
        model.addAttribute("incidents", incidents);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", incidents.getTotalPages());
        
        return "agent/incidents";
    }
    
    @GetMapping("/incidents/mes-assignations")
    public String mesAssignations(Model model) {
        Utilisateur agent = SecurityUtils.getCurrentUser();
        model.addAttribute("incidents", incidentMunicipaliteService.mesIncidentsAssignes(agent));
        return "agent/mes-assignations";
    }
    
    @PostMapping("/incidents/{id}/prendre-en-charge")
    public String prendreEnCharge(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Utilisateur agent = SecurityUtils.getCurrentUser();
            incidentMunicipaliteService.prendreEnCharge(id, agent);
            redirectAttributes.addFlashAttribute("success", "Incident pris en charge avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/incidents";
    }
    
    @PostMapping("/incidents/{id}/en-resolution")
    public String mettreEnResolution(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Utilisateur agent = SecurityUtils.getCurrentUser();
            incidentMunicipaliteService.mettreEnResolution(id, agent);
            redirectAttributes.addFlashAttribute("success", "Statut mis à jour : En résolution");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/incidents/mes-assignations";
    }
    
    @PostMapping("/incidents/{id}/resolu")
    public String marquerResolu(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Utilisateur agent = SecurityUtils.getCurrentUser();
            incidentMunicipaliteService.marquerResolu(id, agent);
            redirectAttributes.addFlashAttribute("success", "Incident marqué comme résolu");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/incidents/mes-assignations";
    }
}


