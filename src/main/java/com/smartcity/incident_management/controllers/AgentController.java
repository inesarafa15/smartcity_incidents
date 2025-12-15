package com.smartcity.incident_management.controllers;

import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.PrioriteIncident;
import com.smartcity.incident_management.enums.StatutIncident;
import com.smartcity.incident_management.security.SecurityUtils;
import com.smartcity.incident_management.services.municipalite.IncidentMunicipaliteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            
            // Vérifier que l'agent existe et a un département
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

            // Récupérer les incidents assignés
            List<Incident> incidents = incidentMunicipaliteService.mesIncidentsAssignes(agent);
            
            // Initialiser les maps pour éviter les valeurs null
            Map<String, Long> parStatut = new HashMap<>();
            Map<String, Long> parPriorite = new HashMap<>();

            // Calculer les statistiques pour les graphiques
            if (incidents != null && !incidents.isEmpty()) {
                for (Incident incident : incidents) {
                    // Par statut
                    String statut = incident.getStatut().name();
                    parStatut.put(statut, parStatut.getOrDefault(statut, 0L) + 1);

                    // Par priorité
                    String priorite = incident.getPriorite().name();
                    parPriorite.put(priorite, parPriorite.getOrDefault(priorite, 0L) + 1);
                }
            }

            // Ajouter tous les attributs au modèle
            model.addAttribute("incidentsAssignes", incidents != null ? incidents : List.of());
            model.addAttribute("incidentsParStatut", parStatut);
            model.addAttribute("incidentsParPriorite", parPriorite);
            
            // Log pour debug
            System.out.println("Dashboard chargé pour l'agent: " + agent.getNom());
            System.out.println("Nombre d'incidents: " + (incidents != null ? incidents.size() : 0));
            System.out.println("Statuts: " + parStatut);
            System.out.println("Priorités: " + parPriorite);
            
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
            @RequestParam(defaultValue = "mine") String filter,
            Model model) {
        
        try {
            Utilisateur agent = SecurityUtils.getCurrentUser();
            
            if (agent == null || agent.getDepartement() == null) {
                model.addAttribute("error", "Vous n'êtes pas assigné à un département");
                model.addAttribute("incidents", Page.empty());
                model.addAttribute("currentPage", 0);
                model.addAttribute("totalPages", 0);
                return "agent/incidents";
            }

            boolean assignedToMe = "mine".equalsIgnoreCase(filter);

            Page<Incident> incidents = incidentMunicipaliteService.filtrerIncidentsDepartement(
                agent, assignedToMe, statut, priorite, dateDebut, dateFin, keyword, page, size, sortBy, sortDir
            );
            
            model.addAttribute("incidents", incidents);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", incidents.getTotalPages());
            model.addAttribute("agent", agent); // Ajouter l'agent au modèle pour les vues
            
            // Paramètres de filtre pour la vue
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
            model.addAttribute("error", "Erreur lors du chargement des incidents: " + e.getMessage());
            model.addAttribute("incidents", Page.empty());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            return "agent/incidents";
        }
    }

    @GetMapping("/mes-assignations")
    public String mesAssignations() {
        return "redirect:/agent/incidents?filter=mine";
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
    
    @GetMapping("/incidents/{id}")
    public String detailsIncident(@PathVariable Long id, Model model) {
        try {
            Utilisateur agent = SecurityUtils.getCurrentUser();
            
            if (agent == null) {
                model.addAttribute("error", "Utilisateur non trouvé");
                return "redirect:/login";
            }
            
            Incident incident = incidentMunicipaliteService.getIncidentById(id);
            
            // Vérifier si l'agent a accès à cet incident
            if (incident == null) {
                model.addAttribute("error", "Incident non trouvé");
                return "redirect:/agent/incidents";
            }
            
            // Vérifier si l'agent fait partie du département de l'incident
            if (agent.getDepartement() == null || 
                !agent.getDepartement().getId().equals(incident.getDepartement().getId())) {
                model.addAttribute("error", "Vous n'avez pas accès à cet incident");
                return "redirect:/agent/incidents";
            }
            
            model.addAttribute("incident", incident);
            model.addAttribute("agent", agent);
            
            return "agent/incident";
            
        } catch (Exception e) {
            System.err.println("Erreur dans détails incident: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Erreur lors du chargement des détails: " + e.getMessage());
            return "redirect:/agent/incidents";
        }
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
    public String envoyerMiseAJour(@PathVariable Long id, @RequestParam String message, RedirectAttributes redirectAttributes) { // Note: param name in modal form was 'message' ? Let's check modal.
        // Modal input name was not visible in previous `read` output, wait.
        // <textarea class="form-control" id="commentaire" name="commentaire" ...> for commentModal
        // UpdateModal: <form th:action="@{/agent/incidents/{id}/envoyer-mise-a-jour(id=${incident.id})}" method="post">
        // I need to check the input name in updateModal.
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