package com.smartcity.incident_management.controllers;

import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.RoleType;
import com.smartcity.incident_management.security.SecurityUtils;
import com.smartcity.incident_management.services.citoyen.IncidentCitoyenService;
import com.smartcity.incident_management.services.municipalite.IncidentMunicipaliteService;
import com.smartcity.incident_management.services.utilisateur.AdminService;
import com.smartcity.incident_management.services.utilisateur.NotificationService;
import com.smartcity.incident_management.services.utilisateur.SuperAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    
    @Autowired
    private IncidentCitoyenService incidentCitoyenService;
    
    @Autowired
    private IncidentMunicipaliteService incidentMunicipaliteService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private SuperAdminService superAdminService;
    
    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard() {
        Utilisateur utilisateur = SecurityUtils.getCurrentUser();
        
        // Redirection automatique selon le rôle (sans exposer le rôle dans l'URL principale)
        if (utilisateur.getRole() == RoleType.SUPER_ADMIN) {
            return "redirect:/super-admin/dashboard";
        } else if (utilisateur.getRole() == RoleType.ADMINISTRATEUR) {
            return "redirect:/admin/dashboard";
        } else if (utilisateur.getRole() == RoleType.AGENT_MUNICIPAL) {
            return "redirect:/agent/dashboard";
        } else if (utilisateur.getRole() == RoleType.CITOYEN) {
            return "redirect:/citoyen/dashboard";
        }
        
        return "redirect:/";
    }
}


