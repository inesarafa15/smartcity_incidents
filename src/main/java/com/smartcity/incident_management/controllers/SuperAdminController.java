package com.smartcity.incident_management.controllers;

import com.smartcity.incident_management.dto.InscriptionDTO;
import com.smartcity.incident_management.entities.Departement;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.CategorieDepartement;
import com.smartcity.incident_management.services.utilisateur.SuperAdminService;
import com.smartcity.incident_management.services.recherche.RechercheService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {
    
    @Autowired
    private SuperAdminService superAdminService;
    
    @Autowired
    private RechercheService rechercheService;
    
    // ========== DASHBOARD ==========
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Object> stats = superAdminService.getStatistiquesGlobales();
        model.addAttribute("stats", stats);
        return "super-admin/dashboard";
    }
    
    // ========== GESTION DES DÉPARTEMENTS ==========
    
    @GetMapping("/departements")
    public String gestionDepartements(Model model) {
        model.addAttribute("departements", superAdminService.tousLesDepartements());
        return "super-admin/departements";
    }
    
    @GetMapping("/departements/nouveau")
    public String nouveauDepartementForm(Model model) {
        model.addAttribute("categories", CategorieDepartement.values());
        return "super-admin/nouveau-departement";
    }
    
    @PostMapping("/departements/nouveau")
    public String creerDepartement(@RequestParam String nom,
                                   @RequestParam String description,
                                   @RequestParam(required = false) String nouveauDepartementNom,
                                   RedirectAttributes redirectAttributes) {
        try {
            if ("AUTRE".equals(nom)) {
                if (nouveauDepartementNom == null || nouveauDepartementNom.trim().isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Veuillez saisir le nom du nouveau département");
                    return "redirect:/super-admin/departements/nouveau";
                }
                superAdminService.creerDepartementAvecNom(nouveauDepartementNom, description);
            } else {
                CategorieDepartement categorie = CategorieDepartement.valueOf(nom);
                superAdminService.creerDepartement(categorie, description);
            }
            redirectAttributes.addFlashAttribute("success", "Département créé avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/departements";
    }
    
    @GetMapping("/departements/{id}/modifier")
    public String modifierDepartementForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Departement departement = superAdminService.trouverDepartementParId(id);
            model.addAttribute("departement", departement);
            return "super-admin/modifier-departement";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/super-admin/departements";
        }
    }
    
    @PostMapping("/departements/{id}/modifier")
    public String modifierDepartement(@PathVariable Long id,
                                     @RequestParam String description,
                                     RedirectAttributes redirectAttributes) {
        try {
            superAdminService.modifierDepartement(id, description);
            redirectAttributes.addFlashAttribute("success", "Département modifié avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/departements";
    }
    
    @PostMapping("/departements/{id}/desactiver")
    public String desactiverDepartement(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            superAdminService.desactiverDepartement(id);
            redirectAttributes.addFlashAttribute("success", "Département désactivé");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/departements";
    }
    
    @PostMapping("/departements/{id}/activer")
    public String activerDepartement(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            superAdminService.activerDepartement(id);
            redirectAttributes.addFlashAttribute("success", "Département activé");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/departements";
    }
    
    // ========== GESTION DES ADMINISTRATEURS ==========
    
    @GetMapping("/administrateurs")
    public String gestionAdministrateurs(Model model) {
        model.addAttribute("administrateurs", superAdminService.tousLesAdministrateurs());
        model.addAttribute("departements", superAdminService.tousLesDepartements());
        return "super-admin/administrateurs";
    }
    
    @GetMapping("/administrateurs/nouveau")
    public String nouveauAdministrateurForm(Model model) {
        model.addAttribute("inscriptionDTO", new InscriptionDTO());
        model.addAttribute("departements", superAdminService.tousLesDepartements());
        model.addAttribute("categories", com.smartcity.incident_management.enums.CategorieDepartement.values());
        return "super-admin/nouveau-administrateur";
    }
    
    @PostMapping("/administrateurs/nouveau")
    public String creerAdministrateur(@Valid @ModelAttribute InscriptionDTO dto,
                                     @RequestParam String departementId,
                                     BindingResult result,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("departements", superAdminService.tousLesDepartements());
            model.addAttribute("categories", com.smartcity.incident_management.enums.CategorieDepartement.values());
            return "super-admin/nouveau-administrateur";
        }
        
        try {
            Long finalDepartementId;
            try {
                finalDepartementId = Long.parseLong(departementId);
            } catch (NumberFormatException e) {
                redirectAttributes.addFlashAttribute("error", "Département invalide");
                return "redirect:/super-admin/administrateurs/nouveau";
            }

            superAdminService.creerAdministrateur(finalDepartementId, dto);
            redirectAttributes.addFlashAttribute("success", "Administrateur créé avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/administrateurs";
    }
    
    @GetMapping("/administrateurs/{id}/modifier")
    public String modifierAdministrateurForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Utilisateur admin = superAdminService.trouverUtilisateurParId(id);
            if (admin.getRole() != com.smartcity.incident_management.enums.RoleType.ADMINISTRATEUR) {
                redirectAttributes.addFlashAttribute("error", "L'utilisateur n'est pas un administrateur");
                return "redirect:/super-admin/administrateurs";
            }
            model.addAttribute("administrateur", admin);
            model.addAttribute("departements", superAdminService.tousLesDepartements());
            return "super-admin/modifier-administrateur";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/super-admin/administrateurs";
        }
    }
    
    @PostMapping("/administrateurs/{id}/modifier")
    public String modifierAdministrateur(@PathVariable Long id,
                                        @RequestParam String nom,
                                        @RequestParam String prenom,
                                        @RequestParam(required = false) String telephone,
                                        RedirectAttributes redirectAttributes) {
        try {
            superAdminService.modifierAdministrateur(id, nom, prenom, telephone);
            redirectAttributes.addFlashAttribute("success", "Administrateur modifié avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/administrateurs";
    }
    
    @PostMapping("/administrateurs/{id}/affecter-departement")
    public String affecterAdministrateurADepartement(@PathVariable Long id,
                                                     @RequestParam Long departementId,
                                                     RedirectAttributes redirectAttributes) {
        try {
            superAdminService.affecterAdministrateurADepartement(id, departementId);
            redirectAttributes.addFlashAttribute("success", "Administrateur affecté au département");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/administrateurs";
    }
    
    @PostMapping("/administrateurs/{id}/activer")
    public String activerAdministrateur(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            superAdminService.activerAdministrateur(id);
            redirectAttributes.addFlashAttribute("success", "Administrateur activé");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/administrateurs";
    }
    
    @PostMapping("/administrateurs/{id}/desactiver")
    public String desactiverAdministrateur(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            superAdminService.desactiverAdministrateur(id);
            redirectAttributes.addFlashAttribute("success", "Administrateur désactivé");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/administrateurs";
    }
    
    // ========== GESTION DES AGENTS MUNICIPAUX ==========
    
    @GetMapping("/agents")
    public String gestionAgents(Model model) {
        model.addAttribute("agents", superAdminService.tousLesAgentsMunicipaux());
        model.addAttribute("departements", superAdminService.tousLesDepartements());
        return "super-admin/agents";
    }
    
    @GetMapping("/agents/nouveau")
    public String nouveauAgentForm(Model model) {
        model.addAttribute("inscriptionDTO", new InscriptionDTO());
        model.addAttribute("departements", superAdminService.tousLesDepartements());
        return "super-admin/nouveau-agent";
    }
    
    @PostMapping("/agents/nouveau")
    public String creerAgent(@Valid @ModelAttribute InscriptionDTO dto,
                            @RequestParam(required = false) String departementId,
                            @RequestParam(required = false) String nouveauDepartementNom,
                            @RequestParam(required = false) String nouveauDepartementDescription,
                            BindingResult result,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("departements", superAdminService.tousLesDepartements());
            return "super-admin/nouveau-agent";
        }
        
        try {
            Long finalDepartementId = null;
            
            // Si "AUTRE" est sélectionné et un nouveau nom est fourni
            if ("AUTRE".equals(departementId) && nouveauDepartementNom != null && !nouveauDepartementNom.trim().isEmpty()) {
                Departement nouveauDepartement = superAdminService.creerDepartementAvecNom(
                    nouveauDepartementNom, 
                    nouveauDepartementDescription != null ? nouveauDepartementDescription : ""
                );
                finalDepartementId = nouveauDepartement.getId();
            } else if (departementId != null && !departementId.isEmpty() && !"AUTRE".equals(departementId)) {
                // Département existant sélectionné
                try {
                    finalDepartementId = Long.parseLong(departementId);
                } catch (NumberFormatException e) {
                    redirectAttributes.addFlashAttribute("error", "Département invalide");
                    return "redirect:/super-admin/agents/nouveau";
                }
            }
            
            if (finalDepartementId == null) {
                redirectAttributes.addFlashAttribute("error", "Le département est obligatoire");
                return "redirect:/super-admin/agents/nouveau";
            }
            
            superAdminService.creerAgentMunicipal(finalDepartementId, dto);
            redirectAttributes.addFlashAttribute("success", "Agent créé avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/agents";
    }
    
    @GetMapping("/agents/{id}/modifier")
    public String modifierAgentForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Utilisateur agent = superAdminService.trouverUtilisateurParId(id);
            if (agent.getRole() != com.smartcity.incident_management.enums.RoleType.AGENT_MUNICIPAL) {
                redirectAttributes.addFlashAttribute("error", "L'utilisateur n'est pas un agent municipal");
                return "redirect:/super-admin/agents";
            }
            model.addAttribute("agent", agent);
            model.addAttribute("departements", superAdminService.tousLesDepartements());
            return "super-admin/modifier-agent";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/super-admin/agents";
        }
    }
    
    @PostMapping("/agents/{id}/modifier")
    public String modifierAgent(@PathVariable Long id,
                               @RequestParam String nom,
                               @RequestParam String prenom,
                               @RequestParam(required = false) String telephone,
                               RedirectAttributes redirectAttributes) {
        try {
            // Ne pas permettre de modifier le département via cette méthode
            superAdminService.modifierAgentMunicipal(id, nom, prenom, telephone, null);
            redirectAttributes.addFlashAttribute("success", "Agent modifié avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/agents";
    }
    
    @PostMapping("/agents/{id}/reaffecter-departement")
    public String reaffecterAgentADepartement(@PathVariable Long id,
                                              @RequestParam Long departementId,
                                              RedirectAttributes redirectAttributes) {
        try {
            Utilisateur agent = superAdminService.trouverUtilisateurParId(id);
            if (agent.getRole() != com.smartcity.incident_management.enums.RoleType.AGENT_MUNICIPAL) {
                redirectAttributes.addFlashAttribute("error", "L'utilisateur n'est pas un agent municipal");
                return "redirect:/super-admin/agents";
            }
            superAdminService.modifierAgentMunicipal(id, agent.getNom(), agent.getPrenom(), agent.getTelephone(), departementId);
            redirectAttributes.addFlashAttribute("success", "Agent réaffecté au département");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/agents";
    }
    
    @PostMapping("/agents/{id}/activer")
    public String activerAgent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            superAdminService.activerAgentMunicipal(id);
            redirectAttributes.addFlashAttribute("success", "Agent activé");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/agents";
    }
    
    @PostMapping("/agents/{id}/desactiver")
    public String desactiverAgent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            superAdminService.desactiverAgentMunicipal(id);
            redirectAttributes.addFlashAttribute("success", "Agent désactivé");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/agents";
    }
    
    // ========== TOUS LES UTILISATEURS ==========
    
    @GetMapping("/utilisateurs")
    public String tousLesUtilisateurs(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long departementId,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String recherche,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nom") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir,
            Model model) {
        
        com.smartcity.incident_management.dto.UtilisateurFiltreDTO filtres = new com.smartcity.incident_management.dto.UtilisateurFiltreDTO();
        if (role != null && !role.isEmpty()) {
            try {
                filtres.setRole(com.smartcity.incident_management.enums.RoleType.valueOf(role));
            } catch (IllegalArgumentException e) {
                // Ignorer si le rôle n'est pas valide
            }
        }
        filtres.setDepartementId(departementId);
        filtres.setActif(actif);
        filtres.setRecherche(recherche);
        filtres.setPage(page);
        filtres.setSize(size);
        filtres.setSortBy(sortBy);
        filtres.setSortDir(sortDir);
        
        org.springframework.data.domain.Page<com.smartcity.incident_management.entities.Utilisateur> utilisateurs = 
                rechercheService.rechercherUtilisateurs(filtres);
        
        model.addAttribute("utilisateurs", utilisateurs);
        model.addAttribute("filtres", filtres);
        model.addAttribute("statistiques", rechercheService.getStatistiquesUtilisateurs(filtres));
        model.addAttribute("departements", superAdminService.tousLesDepartements());
        
        return "super-admin/utilisateurs";
    }
    
    // ========== TOUS LES INCIDENTS ==========
    
    @GetMapping("/incidents")
    public String tousLesIncidents(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String priorite,
            @RequestParam(required = false) Long quartierId,
            @RequestParam(required = false) Long departementId,
            @RequestParam(required = false) String dateDebut,
            @RequestParam(required = false) String dateFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateCreation") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Model model) {
        
        com.smartcity.incident_management.dto.IncidentFiltreDTO filtres = new com.smartcity.incident_management.dto.IncidentFiltreDTO();
        if (statut != null && !statut.isEmpty()) {
            try {
                filtres.setStatut(com.smartcity.incident_management.enums.StatutIncident.valueOf(statut));
            } catch (IllegalArgumentException e) {
                // Ignorer si le statut n'est pas valide
            }
        }
        if (priorite != null && !priorite.isEmpty()) {
            try {
                filtres.setPriorite(com.smartcity.incident_management.enums.PrioriteIncident.valueOf(priorite));
            } catch (IllegalArgumentException e) {
                // Ignorer si la priorité n'est pas valide
            }
        }
        filtres.setQuartierId(quartierId);
        filtres.setDepartementId(departementId);
        if (dateDebut != null && !dateDebut.isEmpty()) {
            try {
                filtres.setDateDebut(java.time.LocalDateTime.parse(dateDebut + "T00:00:00"));
            } catch (Exception e) {
                // Ignorer si la date n'est pas valide
            }
        }
        if (dateFin != null && !dateFin.isEmpty()) {
            try {
                filtres.setDateFin(java.time.LocalDateTime.parse(dateFin + "T23:59:59"));
            } catch (Exception e) {
                // Ignorer si la date n'est pas valide
            }
        }
        filtres.setPage(page);
        filtres.setSize(size);
        filtres.setSortBy(sortBy);
        filtres.setSortDir(sortDir);
        
        org.springframework.data.domain.Page<com.smartcity.incident_management.entities.Incident> incidents = 
                rechercheService.rechercherIncidents(filtres);
        
        model.addAttribute("incidents", incidents);
        model.addAttribute("filtres", filtres);
        model.addAttribute("statistiques", rechercheService.getStatistiquesIncidents(filtres));
        model.addAttribute("departements", superAdminService.tousLesDepartements());
        
        return "super-admin/incidents";
    }
}

