package com.smartcity.incident_management.services.utilisateur;

import com.smartcity.incident_management.dto.InscriptionDTO;
import com.smartcity.incident_management.entities.Departement;
import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.CategorieDepartement;
import com.smartcity.incident_management.enums.RoleType;
import com.smartcity.incident_management.enums.StatutIncident;
import com.smartcity.incident_management.exceptions.BadRequestException;
import com.smartcity.incident_management.exceptions.ResourceNotFoundException;
import com.smartcity.incident_management.repository.DepartementRepository;
import com.smartcity.incident_management.repository.IncidentRepository;
import com.smartcity.incident_management.repository.UtilisateurRepository;
import com.smartcity.incident_management.services.email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
@Transactional
public class SuperAdminService {
    
    @Autowired
    private DepartementRepository departementRepository;
    
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    
    @Autowired
    private IncidentRepository incidentRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;
    
    // ========== GESTION DES DÉPARTEMENTS ==========
    
    public Departement creerDepartement(CategorieDepartement nom, String description) {
        if (departementRepository.existsByNom(nom)) {
            throw new BadRequestException("Un département avec ce nom existe déjà");
        }
        
        Departement departement = new Departement();
        departement.setNom(nom);
        departement.setDescription(description);
        departement.setActif(true);
        departement.setLibelle(nom.name());
        
        return departementRepository.save(departement);
    }
    
    public Departement modifierDepartement(Long id, String description) {
        Departement departement = departementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé"));
        
        departement.setDescription(description);
        return departementRepository.save(departement);
    }
    
    public Departement desactiverDepartement(Long id) {
        Departement departement = departementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé"));
        
        departement.setActif(false);
        return departementRepository.save(departement);
    }
    
    public Departement activerDepartement(Long id) {
        Departement departement = departementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé"));
        
        departement.setActif(true);
        return departementRepository.save(departement);
    }
    
    public List<Departement> tousLesDepartements() {
        return departementRepository.findAll();
    }

    public Page<Departement> tousLesDepartements(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir != null && sortDir.equalsIgnoreCase("DESC")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return departementRepository.findAll(pageable);
    }
    
    public Departement trouverDepartementParId(Long id) {
        return departementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé"));
    }
    
    // ========== GESTION DES ADMINISTRATEURS ==========
    
    public Utilisateur creerAdministrateur(Long departementId, InscriptionDTO dto) {
        if (!dto.getMotDePasse().equals(dto.getConfirmationMotDePasse())) {
            throw new BadRequestException("Les mots de passe ne correspondent pas");
        }
        
        if (utilisateurRepository.existsByEmail(dto.getEmail())) {
            throw new BadRequestException("Un utilisateur avec cet email existe déjà");
        }
        
        if (departementId == null) {
            throw new BadRequestException("Le département est obligatoire");
        }
        
        Departement departement = departementRepository.findById(departementId)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé"));
        
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(dto.getNom());
        utilisateur.setPrenom(dto.getPrenom());
        utilisateur.setEmail(dto.getEmail());
        utilisateur.setMotDePasseHash(passwordEncoder.encode(dto.getMotDePasse()));
        utilisateur.setTelephone(dto.getTelephone());
        utilisateur.setRole(RoleType.ADMINISTRATEUR);
        utilisateur.setDepartement(departement);
        utilisateur.setActif(true);
        
        Utilisateur saved = utilisateurRepository.save(utilisateur);
        
        // Envoyer un email de bienvenue
        try {
            emailService.envoyerEmailBienvenue(saved, dto.getMotDePasse());
        } catch (Exception e) {
            // Log l'erreur mais ne bloque pas la création
            System.err.println("Erreur lors de l'envoi de l'email de bienvenue: " + e.getMessage());
        }
        
        return saved;
    }
    
    public Departement creerDepartementAvecNom(String nomDepartement, String description) {
        // Vérifier si un département avec ce nom existe déjà
        CategorieDepartement categorie = null;
        try {
            categorie = CategorieDepartement.valueOf(nomDepartement.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Si ce n'est pas une catégorie existante, utiliser AUTRE
            categorie = CategorieDepartement.AUTRE;
        }
        
        // Vérifier si un département avec cette catégorie existe déjà
        if (departementRepository.existsByNom(categorie) && categorie != CategorieDepartement.AUTRE) {
            throw new BadRequestException("Un département avec ce nom existe déjà");
        }
        
        Departement departement = new Departement();
        departement.setNom(categorie);
        departement.setDescription(description != null ? description : "Département créé automatiquement");
        departement.setActif(true);
        departement.setLibelle(nomDepartement.toUpperCase());
        
        return departementRepository.save(departement);
    }
    
    public Utilisateur modifierAdministrateur(Long id, String nom, String prenom, String telephone) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        if (utilisateur.getRole() != RoleType.ADMINISTRATEUR) {
            throw new BadRequestException("L'utilisateur n'est pas un administrateur");
        }
        
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setTelephone(telephone);
        
        return utilisateurRepository.save(utilisateur);
    }
    
    public Utilisateur affecterAdministrateurADepartement(Long adminId, Long departementId) {
        Utilisateur admin = utilisateurRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Administrateur non trouvé"));
        
        if (admin.getRole() != RoleType.ADMINISTRATEUR) {
            throw new BadRequestException("L'utilisateur n'est pas un administrateur");
        }
        
        Departement departement = departementRepository.findById(departementId)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé"));
        
        admin.setDepartement(departement);
        return utilisateurRepository.save(admin);
    }
    
    public Utilisateur activerAdministrateur(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        if (utilisateur.getRole() != RoleType.ADMINISTRATEUR) {
            throw new BadRequestException("L'utilisateur n'est pas un administrateur");
        }
        
        utilisateur.setActif(true);
        return utilisateurRepository.save(utilisateur);
    }
    
    public Utilisateur desactiverAdministrateur(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        if (utilisateur.getRole() != RoleType.ADMINISTRATEUR) {
            throw new BadRequestException("L'utilisateur n'est pas un administrateur");
        }
        
        utilisateur.setActif(false);
        return utilisateurRepository.save(utilisateur);
    }
    
    // ========== GESTION DES AGENTS MUNICIPAUX ==========
    
    public Utilisateur creerAgentMunicipal(Long departementId, InscriptionDTO dto) {
        if (!dto.getMotDePasse().equals(dto.getConfirmationMotDePasse())) {
            throw new BadRequestException("Les mots de passe ne correspondent pas");
        }
        
        if (utilisateurRepository.existsByEmail(dto.getEmail())) {
            throw new BadRequestException("Un utilisateur avec cet email existe déjà");
        }
        
        Departement departement = departementRepository.findById(departementId)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé"));
        
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(dto.getNom());
        utilisateur.setPrenom(dto.getPrenom());
        utilisateur.setEmail(dto.getEmail());
        utilisateur.setMotDePasseHash(passwordEncoder.encode(dto.getMotDePasse()));
        utilisateur.setTelephone(dto.getTelephone());
        utilisateur.setRole(RoleType.AGENT_MUNICIPAL);
        utilisateur.setDepartement(departement);
        utilisateur.setActif(true);
        
        Utilisateur saved = utilisateurRepository.save(utilisateur);
        
        // Envoyer un email de bienvenue
        try {
            emailService.envoyerEmailBienvenue(saved, dto.getMotDePasse());
        } catch (Exception e) {
            // Log l'erreur mais ne bloque pas la création
            System.err.println("Erreur lors de l'envoi de l'email de bienvenue: " + e.getMessage());
        }
        
        return saved;
    }
    
    public Utilisateur modifierAgentMunicipal(Long id, String nom, String prenom, String telephone, Long departementId) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        if (utilisateur.getRole() != RoleType.AGENT_MUNICIPAL) {
            throw new BadRequestException("L'utilisateur n'est pas un agent municipal");
        }
        
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setTelephone(telephone);
        
        if (departementId != null) {
            Departement departement = departementRepository.findById(departementId)
                    .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé"));
            utilisateur.setDepartement(departement);
        }
        
        return utilisateurRepository.save(utilisateur);
    }
    
    public Utilisateur activerAgentMunicipal(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        if (utilisateur.getRole() != RoleType.AGENT_MUNICIPAL) {
            throw new BadRequestException("L'utilisateur n'est pas un agent municipal");
        }
        
        utilisateur.setActif(true);
        return utilisateurRepository.save(utilisateur);
    }
    
    public Utilisateur desactiverAgentMunicipal(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        if (utilisateur.getRole() != RoleType.AGENT_MUNICIPAL) {
            throw new BadRequestException("L'utilisateur n'est pas un agent municipal");
        }
        
        utilisateur.setActif(false);
        return utilisateurRepository.save(utilisateur);
    }
    
    // ========== STATISTIQUES GLOBALES ==========
    
    public Map<String, Object> getStatistiquesGlobales() {
        Map<String, Object> stats = new HashMap<>();
        
        // Tous les utilisateurs
        stats.put("totalUtilisateurs", utilisateurRepository.count());
        stats.put("utilisateursParRole", getUtilisateursParRole());
        
        // Tous les incidents
        stats.put("totalIncidents", incidentRepository.count());
        stats.put("incidentsParStatut", getIncidentsParStatut());
        stats.put("incidentsParDepartement", getIncidentsParDepartement());
        
        // Tous les départements
        stats.put("totalDepartements", departementRepository.count());
        stats.put("departementsActifs", departementRepository.findByActifTrue().size());
        stats.put("adminsParDepartement", getAdminsParDepartement());
        stats.put("agentsParDepartement", getAgentsParDepartement());
        
        return stats;
    }
    
    private Map<String, Long> getUtilisateursParRole() {
        Map<String, Long> parRole = new HashMap<>();
        for (RoleType role : RoleType.values()) {
            parRole.put(role.name(), (long) utilisateurRepository.findByRole(role).size());
        }
        return parRole;
    }
    
    private Map<String, Long> getIncidentsParStatut() {
        Map<String, Long> parStatut = new HashMap<>();
        for (StatutIncident statut : StatutIncident.values()) {
            parStatut.put(statut.name(), (long) incidentRepository.findByStatut(statut).size());
        }
        return parStatut;
    }
    
    private Map<String, Long> getIncidentsParDepartement() {
        Map<String, Long> parDepartement = new HashMap<>();
        List<Departement> departements = departementRepository.findAll();
        for (Departement dep : departements) {
            long count = incidentRepository.findByDepartementId(dep.getId()).size();
            parDepartement.put(dep.getLibelle() != null ? dep.getLibelle() : dep.getNom().name(), count);
        }
        return parDepartement;
    }
    
    private Map<String, Long> getAdminsParDepartement() {
        Map<String, Long> parDepartement = new HashMap<>();
        List<Departement> departements = departementRepository.findAll();
        for (Departement dep : departements) {
            long count = utilisateurRepository.findByDepartementId(dep.getId())
                    .stream()
                    .filter(u -> u.getRole() == RoleType.ADMINISTRATEUR)
                    .count();
            parDepartement.put(dep.getLibelle() != null ? dep.getLibelle() : dep.getNom().name(), count);
        }
        return parDepartement;
    }
    
    private Map<String, Long> getAgentsParDepartement() {
        Map<String, Long> parDepartement = new HashMap<>();
        List<Departement> departements = departementRepository.findAll();
        for (Departement dep : departements) {
            long count = utilisateurRepository.findByDepartementId(dep.getId())
                    .stream()
                    .filter(u -> u.getRole() == RoleType.AGENT_MUNICIPAL)
                    .count();
            parDepartement.put(dep.getLibelle() != null ? dep.getLibelle() : dep.getNom().name(), count);
        }
        return parDepartement;
    }
    
    // ========== LISTES ==========
    
    public List<Utilisateur> tousLesUtilisateurs() {
        return utilisateurRepository.findAll();
    }
    
    public List<Utilisateur> tousLesAdministrateurs() {
        return utilisateurRepository.findByRole(RoleType.ADMINISTRATEUR);
    }

    public Page<Utilisateur> tousLesAdministrateurs(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir != null && sortDir.equalsIgnoreCase("DESC")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return utilisateurRepository.findByRole(RoleType.ADMINISTRATEUR, pageable);
    }
    
    public List<Utilisateur> tousLesAgentsMunicipaux() {
        return utilisateurRepository.findByRole(RoleType.AGENT_MUNICIPAL);
    }

    public Page<Utilisateur> tousLesAgentsMunicipaux(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir != null && sortDir.equalsIgnoreCase("DESC")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return utilisateurRepository.findByRole(RoleType.AGENT_MUNICIPAL, pageable);
    }
    
    public Utilisateur trouverUtilisateurParId(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }
    
    public Incident trouverIncidentParId(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));
    }
    
    public List<Incident> tousLesIncidents() {
        return incidentRepository.findAll();
    }
}

