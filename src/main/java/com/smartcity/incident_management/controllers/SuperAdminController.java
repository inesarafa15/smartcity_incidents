package com.smartcity.incident_management.controllers;

import com.smartcity.incident_management.dto.InscriptionDTO;
import com.smartcity.incident_management.entities.Departement;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.CategorieDepartement;
import com.smartcity.incident_management.services.utilisateur.SuperAdminService;
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
    public String creerDepartement(@RequestParam CategorieDepartement nom,
                                   @RequestParam String description,
                                   RedirectAttributes redirectAttributes) {
        try {
            superAdminService.creerDepartement(nom, description);
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
        return "super-admin/nouveau-administrateur";
    }
    
    @PostMapping("/administrateurs/nouveau")
    public String creerAdministrateur(@Valid @ModelAttribute InscriptionDTO dto,
                                     BindingResult result,
                                     RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "super-admin/nouveau-administrateur";
        }
        
        try {
            superAdminService.creerAdministrateur(dto);
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
                            @RequestParam Long departementId,
                            BindingResult result,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("departements", superAdminService.tousLesDepartements());
            return "super-admin/nouveau-agent";
        }
        
        try {
            superAdminService.creerAgentMunicipal(departementId, dto);
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
                               @RequestParam(required = false) Long departementId,
                               RedirectAttributes redirectAttributes) {
        try {
            superAdminService.modifierAgentMunicipal(id, nom, prenom, telephone, departementId);
            redirectAttributes.addFlashAttribute("success", "Agent modifié avec succès");
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
    public String tousLesUtilisateurs(Model model) {
        model.addAttribute("utilisateurs", superAdminService.tousLesUtilisateurs());
        return "super-admin/utilisateurs";
    }
    
    // ========== TOUS LES INCIDENTS ==========
    
    @GetMapping("/incidents")
    public String tousLesIncidents(Model model) {
        model.addAttribute("incidents", superAdminService.tousLesIncidents());
        return "super-admin/incidents";
    }
}

