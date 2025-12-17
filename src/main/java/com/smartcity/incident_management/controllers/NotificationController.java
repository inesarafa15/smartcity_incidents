package com.smartcity.incident_management.controllers;

import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.security.SecurityUtils;
import com.smartcity.incident_management.services.utilisateur.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;
    
    @GetMapping
    public String mesNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Utilisateur utilisateur = SecurityUtils.getCurrentUser();
        Page<com.smartcity.incident_management.entities.Notification> notifications = notificationService.mesNotifications(utilisateur, page, size);
        
        model.addAttribute("notifications", notifications);
        model.addAttribute("notificationsNonLues", notificationService.mesNotificationsNonLues(utilisateur));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notifications.getTotalPages());
        model.addAttribute("totalItems", notifications.getTotalElements());
        
        return "notifications/liste";
    }
    
    @PostMapping("/{id}/marquer-lue")
    public String marquerCommeLue(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Utilisateur utilisateur = SecurityUtils.getCurrentUser();
            notificationService.marquerCommeLue(id, utilisateur);
            redirectAttributes.addFlashAttribute("success", "Notification marquée comme lue");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/notifications";
    }
    
    @PostMapping("/{id}/marquer-lue-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, String>> marquerCommeLueAjax(@PathVariable Long id) {
        try {
            Utilisateur utilisateur = SecurityUtils.getCurrentUser();
            notificationService.marquerCommeLue(id, utilisateur);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/marquer-toutes-lues")
    public String marquerToutesCommeLues(RedirectAttributes redirectAttributes) {
        try {
            Utilisateur utilisateur = SecurityUtils.getCurrentUser();
            notificationService.marquerToutesCommeLues(utilisateur);
            redirectAttributes.addFlashAttribute("success", "Toutes les notifications ont été marquées comme lues");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/notifications";
    }
    
    @PostMapping("/marquer-toutes-lues-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, String>> marquerToutesCommeLuesAjax() {
        try {
            Utilisateur utilisateur = SecurityUtils.getCurrentUser();
            notificationService.marquerToutesCommeLues(utilisateur);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    

    
    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> nombreNotificationsNonLues() {
        Utilisateur utilisateur = SecurityUtils.getCurrentUser();
        long count = notificationService.nombreNotificationsNonLues(utilisateur);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<?> listeNotificationsNonLues() {
        Utilisateur utilisateur = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(notificationService.mesNotificationsNonLues(utilisateur));
    }
}

