package com.smartcity.incident_management.controllers;

import com.smartcity.incident_management.dto.ConnexionDTO;
import com.smartcity.incident_management.dto.InscriptionDTO;
import com.smartcity.incident_management.services.utilisateur.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    
    @Autowired
    private UtilisateurService utilisateurService;
    
    @GetMapping("/")
    public String home() {
        return "index";
    }
    
    @GetMapping("/inscription")
    public String inscriptionForm(Model model) {
        model.addAttribute("inscriptionDTO", new InscriptionDTO());
        return "auth/inscription";
    }
    
    @PostMapping("/inscription")
    public String inscription(@Valid @ModelAttribute InscriptionDTO dto, 
                             BindingResult result, 
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/inscription";
        }
        
        try {
            utilisateurService.inscrireCitoyen(dto);
            redirectAttributes.addFlashAttribute("success", 
                    "Inscription réussie ! Vous pouvez maintenant vous connecter.");
            return "redirect:/connexion";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/inscription";
        }
    }
    
    @GetMapping("/connexion")
    public String connexionForm(Model model) {
        model.addAttribute("connexionDTO", new ConnexionDTO());
        return "auth/connexion";
    }
}


