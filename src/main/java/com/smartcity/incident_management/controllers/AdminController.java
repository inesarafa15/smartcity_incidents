package com.smartcity.incident_management.controllers;

import com.smartcity.incident_management.dto.RapportDTO;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.security.SecurityUtils;
import com.smartcity.incident_management.services.utilisateur.AdminService;
import com.smartcity.incident_management.services.utilisateur.QuartierService;
import com.smartcity.incident_management.services.utilisateur.RapportService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMINISTRATEUR')")
public class AdminController {
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private QuartierService quartierService;
    
    @Autowired
    private RapportService rapportService;
    
    // ========== DASHBOARD ==========
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Utilisateur admin = SecurityUtils.getCurrentUser();
        Map<String, Object> stats = adminService.getStatistiquesDepartement(admin);
        model.addAttribute("stats", stats);
        model.addAttribute("incidentsEnAttente", adminService.incidentsEnAttente(admin));
        model.addAttribute("agentsDisponibles", adminService.agentsDisponibles(admin));
        return "admin/dashboard";
    }
    
    // ========== GESTION DES INCIDENTS ==========
    
    @GetMapping("/incidents")
    public String incidents(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(defaultValue = "dateCreation") String sortBy,
                           @RequestParam(defaultValue = "DESC") String sortDir,
                           Model model) {
        Utilisateur admin = SecurityUtils.getCurrentUser();
        Page incidents = adminService.incidentsDuDepartement(admin, page, size, sortBy, sortDir);
        model.addAttribute("incidents", incidents);
        model.addAttribute("agentsDisponibles", adminService.agentsDisponibles(admin));
        return "admin/incidents";
    }
    
    @PostMapping("/incidents/{incidentId}/affecter")
    public String affecterIncident(@PathVariable Long incidentId,
                                  @RequestParam Long agentId,
                                  RedirectAttributes redirectAttributes) {
        try {
            Utilisateur admin = SecurityUtils.getCurrentUser();
            adminService.affecterIncidentAAgent(incidentId, agentId, admin);
            redirectAttributes.addFlashAttribute("success", "Incident affecté avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/incidents";
    }
    
    // ========== GESTION DES RAPPORTS ==========
    
    @GetMapping("/rapports")
    public String rapports(Model model) {
        Utilisateur admin = SecurityUtils.getCurrentUser();
        model.addAttribute("rapports", rapportService.findByAdmin(admin.getId()));
        return "admin/rapports";
    }
    
    @GetMapping("/rapports/nouveau")
    public String nouveauRapportForm(Model model) {
        model.addAttribute("rapportDTO", new RapportDTO());
        model.addAttribute("quartiers", quartierService.findAll());
        return "admin/nouveau-rapport";
    }
    
    @PostMapping("/rapports/nouveau")
    public String genererRapport(@Valid @ModelAttribute RapportDTO dto,
                                  BindingResult result,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("quartiers", quartierService.findAll());
            return "admin/nouveau-rapport";
        }
        
        try {
            Utilisateur admin = SecurityUtils.getCurrentUser();
            adminService.genererRapportDepartement(admin, dto);
            redirectAttributes.addFlashAttribute("success", "Rapport généré avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/rapports";
    }
}


