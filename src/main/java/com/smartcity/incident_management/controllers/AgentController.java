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

@Controller
@RequestMapping("/agent")
@PreAuthorize("hasRole('AGENT_MUNICIPAL')")
public class AgentController {
    
    @Autowired
    private IncidentMunicipaliteService incidentMunicipaliteService;
    
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


