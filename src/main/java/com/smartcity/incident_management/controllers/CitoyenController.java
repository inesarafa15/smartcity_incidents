package com.smartcity.incident_management.controllers;

import com.smartcity.incident_management.dto.IncidentDTO;
import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.PrioriteIncident;
import com.smartcity.incident_management.enums.StatutIncident;
import com.smartcity.incident_management.security.SecurityUtils;
import com.smartcity.incident_management.services.citoyen.IncidentCitoyenService;
import com.smartcity.incident_management.services.utilisateur.DepartementService;
import com.smartcity.incident_management.services.utilisateur.NotificationService;
import com.smartcity.incident_management.services.utilisateur.QuartierService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/citoyen")
@PreAuthorize("hasRole('CITOYEN')")
public class CitoyenController {

    @Autowired
    private IncidentCitoyenService incidentCitoyenService;

    @Autowired
    private DepartementService departementService;

    @Autowired
    private QuartierService quartierService;

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model
    ) {
        Utilisateur citoyen = SecurityUtils.getCurrentUser();

        Page<Incident> incidentsRecents =
                incidentCitoyenService.mesIncidents(citoyen, page, size, "dateCreation", "DESC");

        var notifications = notificationService.mesNotificationsNonLues(citoyen);
        long nbNotificationsNonLues = notificationService.nombreNotificationsNonLues(citoyen);

        Page<Incident> tousIncidents =
                incidentCitoyenService.mesIncidents(citoyen, 0, 1000, "dateCreation", "DESC");

        long totalIncidents = tousIncidents.getTotalElements();

        long incidentsEnCours = tousIncidents.getContent().stream()
                .filter(i -> i.getStatut() == StatutIncident.SIGNALE
                        || i.getStatut() == StatutIncident.PRIS_EN_CHARGE
                        || i.getStatut() == StatutIncident.EN_RESOLUTION)
                .count();

        long incidentsResolus = tousIncidents.getContent().stream()
                .filter(i -> i.getStatut() == StatutIncident.RESOLU)
                .count();

        model.addAttribute("incidents", incidentsRecents);
        model.addAttribute("notifications", notifications);
        model.addAttribute("nbNotificationsNonLues", nbNotificationsNonLues);
        model.addAttribute("totalIncidents", totalIncidents);
        model.addAttribute("incidentsEnCours", incidentsEnCours);
        model.addAttribute("incidentsResolus", incidentsResolus);

        return "citoyen/dashboard";
    }

    @GetMapping("/incidents")
    public String mesIncidents(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String priorite,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateCreation") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Model model
    ) {
        Utilisateur citoyen = SecurityUtils.getCurrentUser();

        Page<Incident> pageIncidents =
                incidentCitoyenService.mesIncidents(citoyen, page, size, sortBy, sortDir);

        final StatutIncident statutEnum = (statut != null && !statut.isBlank()) ? safeStatut(statut) : null;
        final PrioriteIncident prioriteEnum = (priorite != null && !priorite.isBlank()) ? safePriorite(priorite) : null;

        List<Incident> filtered = pageIncidents.getContent().stream()
                .filter(i -> statutEnum == null || (statutEnum != null && i.getStatut() == statutEnum))
                .filter(i -> prioriteEnum == null || (prioriteEnum != null && i.getPriorite() == prioriteEnum))
                .toList();

        Pageable pageable = PageRequest.of(page, size);
        Page<Incident> incidents = new PageImpl<>(filtered, pageable, filtered.size());

        model.addAttribute("incidents", incidents);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", incidents.getTotalPages());
        model.addAttribute("statut", statut);
        model.addAttribute("priorite", priorite);

        return "citoyen/incidents";
    }

    @GetMapping("/incidents/nouveau")
    public String nouveauIncidentForm(Model model) {
        model.addAttribute("incidentDTO", new IncidentDTO());
        model.addAttribute("departements", departementService.findAll());
        model.addAttribute("quartiers", quartierService.findAll());
        return "citoyen/nouveau-incident";
    }

    @PostMapping("/incidents/nouveau")
    public String signalerIncident(
            @Valid @ModelAttribute("incidentDTO") IncidentDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            model.addAttribute("departements", departementService.findAll());
            model.addAttribute("quartiers", quartierService.findAll());
            return "citoyen/nouveau-incident";
        }

        try {
            Utilisateur citoyen = SecurityUtils.getCurrentUser();
            incidentCitoyenService.signalerIncident(citoyen, dto);
            redirectAttributes.addFlashAttribute("success", "Incident signalé avec succès !");
            return "redirect:/citoyen/incidents";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/citoyen/incidents/nouveau";
        }
    }

    @GetMapping("/incidents/{id}")
    public String consulterIncident(@PathVariable Long id, Model model) {
        Utilisateur citoyen = SecurityUtils.getCurrentUser();
        Incident incident = incidentCitoyenService.consulterIncident(id, citoyen);
        model.addAttribute("incident", incident);
        return "citoyen/incident-details";
    }

    private StatutIncident safeStatut(String value) {
        try {
            return StatutIncident.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private PrioriteIncident safePriorite(String value) {
        try {
            return PrioriteIncident.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
