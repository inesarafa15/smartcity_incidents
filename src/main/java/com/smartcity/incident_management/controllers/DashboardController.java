package com.smartcity.incident_management.controllers;

import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.RoleType;
import com.smartcity.incident_management.security.SecurityUtils;
import com.smartcity.incident_management.services.citoyen.IncidentCitoyenService;
import com.smartcity.incident_management.services.municipalite.IncidentMunicipaliteService;
import com.smartcity.incident_management.services.utilisateur.NotificationService;
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
    
    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(Model model) {
        Utilisateur utilisateur = SecurityUtils.getCurrentUser();
        
        if (utilisateur.getRole() == RoleType.CITOYEN) {
            model.addAttribute("incidents", incidentCitoyenService.mesIncidentsList(utilisateur));
            model.addAttribute("notifications", notificationService.mesNotificationsNonLues(utilisateur));
            return "dashboard/citoyen";
        } else if (utilisateur.getRole() == RoleType.AGENT_MUNICIPAL) {
            model.addAttribute("incidents", incidentMunicipaliteService.mesIncidentsAssignes(utilisateur));
            model.addAttribute("notifications", notificationService.mesNotificationsNonLues(utilisateur));
            return "dashboard/agent";
        } else {
            model.addAttribute("notifications", notificationService.mesNotificationsNonLues(utilisateur));
            return "dashboard/admin";
        }
    }
}


