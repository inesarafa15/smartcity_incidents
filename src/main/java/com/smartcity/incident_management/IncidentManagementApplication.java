package com.smartcity.incident_management;

import com.smartcity.incident_management.services.utilisateur.UtilisateurService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class IncidentManagementApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(IncidentManagementApplication.class, args);

        // Récupération du service et seed du SUPER_ADMIN
        UtilisateurService utilisateurService = context.getBean(UtilisateurService.class);
        utilisateurService.seedSuperAdmin();
    }
}
