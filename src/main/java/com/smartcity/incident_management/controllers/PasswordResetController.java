package com.smartcity.incident_management.controllers;

import com.smartcity.incident_management.entities.PasswordResetToken;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.repository.PasswordResetTokenRepository;
import com.smartcity.incident_management.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class PasswordResetController {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/mot-de-passe/reinitialiser")
    public String afficherFormulaire(@RequestParam("token") String token, Model model, RedirectAttributes redirectAttributes) {
        Optional<PasswordResetToken> opt = tokenRepository.findByToken(token);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Lien de réinitialisation invalide.");
            return "redirect:/connexion";
        }
        PasswordResetToken prt = opt.get();
        if (prt.isUsed() || prt.getExpiresAt().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("error", "Lien de réinitialisation expiré ou déjà utilisé.");
            return "redirect:/connexion";
        }
        model.addAttribute("token", token);
        return "auth/reinitialiser-mot-de-passe";
    }

    @PostMapping("/mot-de-passe/reinitialiser")
    public String reinitialiser(@RequestParam("token") String token,
                                @RequestParam("nouveauMotDePasse") String nouveauMotDePasse,
                                @RequestParam("confirmationMotDePasse") String confirmation,
                                RedirectAttributes redirectAttributes) {
        Optional<PasswordResetToken> opt = tokenRepository.findByToken(token);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Lien de réinitialisation invalide.");
            return "redirect:/connexion";
        }
        PasswordResetToken prt = opt.get();
        if (prt.isUsed() || prt.getExpiresAt().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("error", "Lien de réinitialisation expiré ou déjà utilisé.");
            return "redirect:/connexion";
        }
        if (nouveauMotDePasse == null || nouveauMotDePasse.isBlank() || !nouveauMotDePasse.equals(confirmation)) {
            redirectAttributes.addFlashAttribute("error", "Les mots de passe ne correspondent pas.");
            return "redirect:/mot-de-passe/reinitialiser?token=" + token;
        }
        // Validation basique de sécurité (8+ caractères avec majuscule, minuscule, chiffre, spécial)
        String pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        if (!nouveauMotDePasse.matches(pattern)) {
            redirectAttributes.addFlashAttribute("error", "Le mot de passe doit contenir 8+ caractères, majuscule, minuscule, chiffre et caractère spécial.");
            return "redirect:/mot-de-passe/reinitialiser?token=" + token;
        }
        Utilisateur utilisateur = prt.getUtilisateur();
        utilisateur.setMotDePasseHash(passwordEncoder.encode(nouveauMotDePasse));
        utilisateurRepository.save(utilisateur);
        prt.setUsed(true);
        tokenRepository.save(prt);
        redirectAttributes.addFlashAttribute("success", "Votre mot de passe a été réinitialisé. Vous pouvez vous connecter.");
        return "redirect:/connexion";
    }
}

