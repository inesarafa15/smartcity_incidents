package com.smartcity.incident_management.controllers;

import com.smartcity.incident_management.dto.InscriptionDTO;
import com.smartcity.incident_management.dto.RapportDTO;
import com.smartcity.incident_management.entities.*;
import com.smartcity.incident_management.security.SecurityUtils;
import com.smartcity.incident_management.services.utilisateur.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMINISTRATEUR', 'SUPER_ADMIN')")
public class AdminController {
    
    @Autowired
    private UtilisateurService utilisateurService;
    
    @Autowired
    private DepartementService departementService;
    
    @Autowired
    private QuartierService quartierService;
    
    @Autowired
    private RapportService rapportService;
    
    @GetMapping("/utilisateurs")
    public String gestionUtilisateurs(Model model) {
        model.addAttribute("utilisateurs", utilisateurService.findAll());
        return "admin/utilisateurs";
    }
    
    @GetMapping("/utilisateurs/nouveau-agent")
    public String nouveauAgentForm(Model model) {
        model.addAttribute("inscriptionDTO", new InscriptionDTO());
        model.addAttribute("departements", departementService.findAll());
        return "admin/nouveau-agent";
    }
    
    @PostMapping("/utilisateurs/nouveau-agent")
    public String creerAgent(@Valid @ModelAttribute InscriptionDTO dto,
                            @RequestParam Long departementId,
                            BindingResult result,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/nouveau-agent";
        }
        
        try {
            utilisateurService.creerAgent(departementId, dto);
            redirectAttributes.addFlashAttribute("success", "Agent créé avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/utilisateurs";
    }
    
    @GetMapping("/departements")
    public String gestionDepartements(Model model) {
        model.addAttribute("departements", departementService.findAll());
        return "admin/departements";
    }
    
    @GetMapping("/quartiers")
    public String gestionQuartiers(Model model) {
        model.addAttribute("quartiers", quartierService.findAll());
        return "admin/quartiers";
    }
    
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
        model.addAttribute("departements", departementService.findAll());
        return "admin/nouveau-rapport";
    }
    
    @PostMapping("/rapports/nouveau")
    public String genererRapport(@Valid @ModelAttribute RapportDTO dto,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/nouveau-rapport";
        }
        
        try {
            Utilisateur admin = SecurityUtils.getCurrentUser();
            rapportService.genererRapport(admin, dto);
            redirectAttributes.addFlashAttribute("success", "Rapport généré avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/rapports";
    }
    
    @PostMapping("/utilisateurs/{id}/desactiver")
    public String desactiverUtilisateur(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            utilisateurService.desactiverUtilisateur(id);
            redirectAttributes.addFlashAttribute("success", "Utilisateur désactivé");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/utilisateurs";
    }
    
    @PostMapping("/utilisateurs/{id}/activer")
    public String activerUtilisateur(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            utilisateurService.activerUtilisateur(id);
            redirectAttributes.addFlashAttribute("success", "Utilisateur activé");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/utilisateurs";
    }
}


