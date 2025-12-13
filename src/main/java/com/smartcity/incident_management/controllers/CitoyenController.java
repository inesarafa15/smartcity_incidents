package com.smartcity.incident_management.controllers;

import com.smartcity.incident_management.dto.IncidentDTO;
import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.security.SecurityUtils;
import com.smartcity.incident_management.services.citoyen.IncidentCitoyenService;
import com.smartcity.incident_management.services.utilisateur.DepartementService;
import com.smartcity.incident_management.services.utilisateur.QuartierService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    
    @GetMapping("/incidents")
    public String mesIncidents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateCreation") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Model model) {
        
        Utilisateur citoyen = SecurityUtils.getCurrentUser();
        Page<Incident> incidents = incidentCitoyenService.mesIncidents(citoyen, page, size, sortBy, sortDir);
        
        model.addAttribute("incidents", incidents);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", incidents.getTotalPages());
        
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
    public String signalerIncident(@Valid @ModelAttribute IncidentDTO dto,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "citoyen/nouveau-incident";
        }
        
        try {
            Utilisateur citoyen = SecurityUtils.getCurrentUser();
            incidentCitoyenService.signalerIncident(citoyen, dto);
            redirectAttributes.addFlashAttribute("success", 
                    "Incident signalé avec succès !");
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
}


