package com.smartcity.incident_management.controllers;

import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.security.SecurityUtils;
import com.smartcity.incident_management.services.utilisateur.AdminService;
import com.smartcity.incident_management.services.rapport.RapportExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    private RapportExportService rapportExportService;
    
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
    
 // In AdminController.java, update the incidents method:

    @GetMapping("/incidents")
    public String incidents(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(required = false) String statut,
                           @RequestParam(required = false) String priorite,
                           @RequestParam(defaultValue = "dateCreation") String sortBy,
                           @RequestParam(defaultValue = "DESC") String sortDir,
                            
                           Model model) {
        
        Utilisateur admin = SecurityUtils.getCurrentUser();
        Page incidents = adminService.incidentsDuDepartement(admin, statut, priorite, page, size, sortBy, sortDir);
        model.addAttribute("incidents", incidents);
        model.addAttribute("agentsDisponibles", adminService.agentsDisponibles(admin));
        model.addAttribute("statut", statut);
        model.addAttribute("priorite", priorite);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        return "admin/incidents";
    }

    @GetMapping("/incidents/{id}")
    public String incidentDetails(@PathVariable Long id, Model model) {
        try {
            Utilisateur admin = SecurityUtils.getCurrentUser();
            Incident incident = adminService.consulterDetailsIncident(id, admin);
            model.addAttribute("incident", incident);
            return "admin/incident-details";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/incidents";
        }
    }

    @PostMapping("/incidents/{id}/valider")
    public String validerResolution(
            @PathVariable Long id, 
            @RequestParam(required = false) String commentaire,
            RedirectAttributes redirectAttributes) {
        try {
            Utilisateur admin = SecurityUtils.getCurrentUser();
            adminService.validerResolution(id, admin, commentaire);
            redirectAttributes.addFlashAttribute("success", 
                "✅ Résolution validée avec succès. L'incident est maintenant clôturé.");
        } catch (Exception e) {
            System.err.println("Erreur validation: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/incidents/" + id;
    }
    
    @PostMapping("/incidents/{id}/refuser")
    public String refuserResolution(
            @PathVariable Long id, 
            @RequestParam String motif,
            RedirectAttributes redirectAttributes) {
        try {
            Utilisateur admin = SecurityUtils.getCurrentUser();
            adminService.refuserResolution(id, admin, motif);
            redirectAttributes.addFlashAttribute("success", 
                "🔄 Résolution refusée. L'incident a été réassigné à l'agent avec votre motif.");
        } catch (Exception e) {
            System.err.println("Erreur refus: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/incidents/" + id;
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
    
    // ========== EXPORT RAPPORTS ==========
    
    @GetMapping("/rapports/export/csv")
    public ResponseEntity<byte[]> exporterCSV() {
        try {
            Utilisateur admin = SecurityUtils.getCurrentUser();
            return rapportExportService.exporterCSV(admin);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/rapports/export/excel")
    public ResponseEntity<byte[]> exporterExcel() {
        try {
            Utilisateur admin = SecurityUtils.getCurrentUser();
            return rapportExportService.exporterExcel(admin);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/rapports/export/pdf")
    public ResponseEntity<byte[]> exporterPDF() {
        try {
            Utilisateur admin = SecurityUtils.getCurrentUser();
            return rapportExportService.exporterPDF(admin);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/rapports/export/statistiques")
    public ResponseEntity<byte[]> exporterStatistiques() {
        // Similaire à exporterExcel mais seulement les stats
        try {
            Utilisateur admin = SecurityUtils.getCurrentUser();
            return rapportExportService.exporterExcel(admin); // Réutilise Excel pour stats détaillées
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}