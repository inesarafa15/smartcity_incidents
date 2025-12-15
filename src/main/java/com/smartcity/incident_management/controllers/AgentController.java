package com.smartcity.incident_management.controllers;

import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.PrioriteIncident;
import com.smartcity.incident_management.enums.StatutIncident;
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
        try {
            Utilisateur agent = SecurityUtils.getCurrentUser();
            
            if (agent == null) {
                model.addAttribute("error", "Utilisateur non trouvé");
                return "redirect:/login";
            }
            
            if (agent.getDepartement() == null) {
                model.addAttribute("error", "Vous n'êtes pas assigné à un département");
                model.addAttribute("incidentsAssignes", List.of());
                model.addAttribute("incidentsParStatut", Map.of());
                model.addAttribute("incidentsParPriorite", Map.of());
                return "agent/dashboard";
            }

            // Récupérer les incidents assignés à l'agent
            List<Incident> incidents = incidentMunicipaliteService.mesIncidentsAssignes(agent);
            
            Map<String, Long> parStatut = new HashMap<>();
            Map<String, Long> parPriorite = new HashMap<>();

            if (incidents != null && !incidents.isEmpty()) {
                for (Incident incident : incidents) {
                    String statut = incident.getStatut().name();
                    parStatut.put(statut, parStatut.getOrDefault(statut, 0L) + 1);

                    String priorite = incident.getPriorite().name();
                    parPriorite.put(priorite, parPriorite.getOrDefault(priorite, 0L) + 1);
                }
            }

            model.addAttribute("incidentsAssignes", incidents != null ? incidents : List.of());
            model.addAttribute("incidentsParStatut", parStatut);
            model.addAttribute("incidentsParPriorite", parPriorite);
            model.addAttribute("agent", agent);
            
            return "agent/dashboard";
            
        } catch (Exception e) {
            System.err.println("Erreur dans le dashboard agent: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Erreur lors du chargement du tableau de bord: " + e.getMessage());
            model.addAttribute("incidentsAssignes", List.of());
            model.addAttribute("incidentsParStatut", Map.of());
            model.addAttribute("incidentsParPriorite", Map.of());
            return "agent/dashboard";
        }
    }

    @GetMapping("/mes-assignations")
    public String mesAssignations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String priorite,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "dateCreation") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Model model) {
        
        try {
            Utilisateur agent = SecurityUtils.getCurrentUser();
            
            if (agent == null || agent.getDepartement() == null) {
                model.addAttribute("error", "Vous n'êtes pas assigné à un département");
                model.addAttribute("incidents", Page.empty());
                return "agent/mes-assignations";
            }

            // Calculer les statistiques globales pour l'agent
            List<Incident> allAssigned = incidentMunicipaliteService.mesIncidentsAssignes(agent);
            long statTotal = allAssigned.size();
            long statPrisEnCharge = allAssigned.stream().filter(i -> i.getStatut() == StatutIncident.PRIS_EN_CHARGE).count();
            long statEnResolution = allAssigned.stream().filter(i -> i.getStatut() == StatutIncident.EN_RESOLUTION).count();
            long statResolu = allAssigned.stream().filter(i -> i.getStatut() == StatutIncident.RESOLU).count();
            
            model.addAttribute("statTotal", statTotal);
            model.addAttribute("statPrisEnCharge", statPrisEnCharge);
            model.addAttribute("statEnResolution", statEnResolution);
            model.addAttribute("statResolu", statResolu);

            // Filtrer uniquement les incidents assignés à cet agent
            Page<Incident> incidents = incidentMunicipaliteService.filtrerIncidentsDepartement(
                agent, true, statut, priorite, null, null, keyword, page, size, sortBy, sortDir
            );
            
            model.addAttribute("incidents", incidents);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", incidents.getTotalPages());
            model.addAttribute("agent", agent);
            
            // Paramètres de filtre
            model.addAttribute("statut", statut);
            model.addAttribute("priorite", priorite);
            model.addAttribute("keyword", keyword);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("size", size);
            
            // Statistiques
            long enCours = incidents.getContent().stream()
                .filter(i -> i.getStatut() == StatutIncident.PRIS_EN_CHARGE || 
                            i.getStatut() == StatutIncident.EN_RESOLUTION)
                .count();
            model.addAttribute("incidentsEnCours", enCours);
            
            return "agent/mes-assignations";
            
        } catch (Exception e) {
            System.err.println("Erreur dans mes assignations: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Erreur lors du chargement: " + e.getMessage());
            model.addAttribute("incidents", Page.empty());
            return "agent/mes-assignations";
        }
    }

    @GetMapping("/incidents")
    public String incidentsDepartement(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String priorite,
            @RequestParam(required = false) String dateDebut,
            @RequestParam(required = false) String dateFin,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "dateCreation") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(defaultValue = "all") String filter,
            Model model) {
        
        try {
            Utilisateur agent = SecurityUtils.getCurrentUser();
            
            if (agent == null || agent.getDepartement() == null) {
                model.addAttribute("error", "Vous n'êtes pas assigné à un département");
                model.addAttribute("incidents", Page.empty());
                return "agent/incidents";
            }

            boolean assignedToMe = "mine".equalsIgnoreCase(filter);

            Page<Incident> incidents = incidentMunicipaliteService.filtrerIncidentsDepartement(
                agent, assignedToMe, statut, priorite, dateDebut, dateFin, keyword, page, size, sortBy, sortDir
            );
            
            model.addAttribute("incidents", incidents);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", incidents.getTotalPages());
            model.addAttribute("agent", agent);
            model.addAttribute("statut", statut);
            model.addAttribute("priorite", priorite);
            model.addAttribute("dateDebut", dateDebut);
            model.addAttribute("dateFin", dateFin);
            model.addAttribute("keyword", keyword);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("size", size);
            model.addAttribute("filter", filter);
            
            return "agent/incidents";
            
        } catch (Exception e) {
            System.err.println("Erreur dans incidents département: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Erreur: " + e.getMessage());
            model.addAttribute("incidents", Page.empty());
            return "agent/incidents";
        }
    }

    @GetMapping("/incidents/{id}")
    public String detailsIncident(@PathVariable Long id, Model model) {
        try {
            Utilisateur agent = SecurityUtils.getCurrentUser();
            
            if (agent == null) {
                model.addAttribute("error", "Utilisateur non trouvé");
                return "redirect:/login";
            }
            
            Incident incident = incidentMunicipaliteService.getIncidentById(id);
            
            if (incident == null) {
                model.addAttribute("error", "Incident non trouvé");
                return "redirect:/agent/incidents";
            }
            
            if (agent.getDepartement() == null || 
                !agent.getDepartement().getId().equals(incident.getDepartement().getId())) {
                model.addAttribute("error", "Vous n'avez pas accès à cet incident");
                return "redirect:/agent/incidents";
            }
            
            model.addAttribute("incident", incident);
            model.addAttribute("agent", agent);
            
            // Vérifier si l'agent est assigné à cet incident
            boolean isAssigned = incident.getAgentAssigne() != null && 
                                incident.getAgentAssigne().getId().equals(agent.getId());
            model.addAttribute("isAssigned", isAssigned);
            
            return "agent/incident-details";
            
        } catch (Exception e) {
            System.err.println("Erreur détails incident: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Erreur: " + e.getMessage());
            return "redirect:/agent/incidents";
        }
    }

    @PostMapping("/incidents/{id}/prendre-en-charge")
    public String prendreEnCharge(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Utilisateur agent = SecurityUtils.getCurrentUser();
            
            if (agent == null) {
                redirectAttributes.addFlashAttribute("error", "Utilisateur non trouvé");
                return "redirect:/login";
            }
            
            incidentMunicipaliteService.prendreEnCharge(id, agent);
            redirectAttributes.addFlashAttribute("success", "Incident pris en charge avec succès");
            
        } catch (Exception e) {
            System.err.println("Erreur prise en charge: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/incidents/" + id;
    }

    @PostMapping("/incidents/{id}/en-resolution")
    public String mettreEnResolution(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Utilisateur agent = SecurityUtils.getCurrentUser();
            
            if (agent == null) {
                redirectAttributes.addFlashAttribute("error", "Utilisateur non trouvé");
                return "redirect:/login";
            }
            
            incidentMunicipaliteService.mettreEnResolution(id, agent);
            redirectAttributes.addFlashAttribute("success", "Statut mis à jour : En résolution");
            
        } catch (Exception e) {
            System.err.println("Erreur mise en résolution: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/incidents/" + id;
    }

    @PostMapping("/incidents/{id}/resolu")
    public String marquerResolu(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Utilisateur agent = SecurityUtils.getCurrentUser();
            
            if (agent == null) {
                redirectAttributes.addFlashAttribute("error", "Utilisateur non trouvé");
                return "redirect:/login";
            }
            
            incidentMunicipaliteService.marquerResolu(id, agent);
            redirectAttributes.addFlashAttribute("success", "Incident marqué comme résolu");
            
        } catch (Exception e) {
            System.err.println("Erreur marquage résolu: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/incidents/" + id;
    }

    @PostMapping("/incidents/{id}/commenter")
    public String ajouterCommentaire(@PathVariable Long id, @RequestParam String commentaire, RedirectAttributes redirectAttributes) {
        try {
            Utilisateur agent = SecurityUtils.getCurrentUser();
            if (agent == null) {
                return "redirect:/login";
            }
            
            incidentMunicipaliteService.ajouterCommentaire(id, agent, commentaire);
            redirectAttributes.addFlashAttribute("success", "Commentaire ajouté avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/incidents/" + id;
    }

    @PostMapping("/incidents/{id}/envoyer-mise-a-jour")
    public String envoyerMiseAJour(@PathVariable Long id, @RequestParam String message, RedirectAttributes redirectAttributes) {
        try {
            Utilisateur agent = SecurityUtils.getCurrentUser();
            if (agent == null) {
                return "redirect:/login";
            }
            
            incidentMunicipaliteService.envoyerMiseAJour(id, agent, message);
            redirectAttributes.addFlashAttribute("success", "Mise à jour envoyée au citoyen");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/incidents/" + id;
    }
}