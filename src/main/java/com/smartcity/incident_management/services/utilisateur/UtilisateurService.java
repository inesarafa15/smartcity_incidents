package com.smartcity.incident_management.services.utilisateur;

import com.smartcity.incident_management.dto.InscriptionDTO;
import com.smartcity.incident_management.entities.Departement;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.RoleType;
import com.smartcity.incident_management.exceptions.BadRequestException;
import com.smartcity.incident_management.exceptions.ResourceNotFoundException;
import com.smartcity.incident_management.repository.DepartementRepository;
import com.smartcity.incident_management.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UtilisateurService {
    
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    
    @Autowired
    private DepartementRepository departementRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public Utilisateur inscrireCitoyen(InscriptionDTO dto) {
        if (!dto.getMotDePasse().equals(dto.getConfirmationMotDePasse())) {
            throw new BadRequestException("Les mots de passe ne correspondent pas");
        }
        
        if (utilisateurRepository.existsByEmail(dto.getEmail())) {
            throw new BadRequestException("Un utilisateur avec cet email existe déjà");
        }
        
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(dto.getNom());
        utilisateur.setPrenom(dto.getPrenom());
        utilisateur.setEmail(dto.getEmail());
        utilisateur.setMotDePasseHash(passwordEncoder.encode(dto.getMotDePasse()));
        utilisateur.setTelephone(dto.getTelephone());
        utilisateur.setRole(RoleType.CITOYEN);
        utilisateur.setActif(true);
        
        return utilisateurRepository.save(utilisateur);
    }
    
    public Utilisateur creerAgent(Long departementId, InscriptionDTO dto) {
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
        
        return utilisateurRepository.save(utilisateur);
    }
    
    public Utilisateur creerAdministrateur(InscriptionDTO dto) {
        if (!dto.getMotDePasse().equals(dto.getConfirmationMotDePasse())) {
            throw new BadRequestException("Les mots de passe ne correspondent pas");
        }
        
        if (utilisateurRepository.existsByEmail(dto.getEmail())) {
            throw new BadRequestException("Un utilisateur avec cet email existe déjà");
        }
        
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(dto.getNom());
        utilisateur.setPrenom(dto.getPrenom());
        utilisateur.setEmail(dto.getEmail());
        utilisateur.setMotDePasseHash(passwordEncoder.encode(dto.getMotDePasse()));
        utilisateur.setTelephone(dto.getTelephone());
        utilisateur.setRole(RoleType.ADMINISTRATEUR);
        utilisateur.setActif(true);
        
        return utilisateurRepository.save(utilisateur);
    }
    
    public Utilisateur findById(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }
    
    public List<Utilisateur> findAll() {
        return utilisateurRepository.findAll();
    }
    
    public List<Utilisateur> findByRole(RoleType role) {
        return utilisateurRepository.findByRole(role);
    }
    
    public List<Utilisateur> findByDepartement(Long departementId) {
        return utilisateurRepository.findByDepartementId(departementId);
    }
    
    public Utilisateur desactiverUtilisateur(Long id) {
        Utilisateur utilisateur = findById(id);
        utilisateur.setActif(false);
        return utilisateurRepository.save(utilisateur);
    }
    
    public Utilisateur activerUtilisateur(Long id) {
        Utilisateur utilisateur = findById(id);
        utilisateur.setActif(true);
        return utilisateurRepository.save(utilisateur);
    }

    public Utilisateur mettreAJourProfil(Long userId, String nom, String prenom, String email, String telephone) {
        Utilisateur utilisateur = findById(userId);
        
        // Vérifier si l'email est modifié et s'il n'est pas déjà pris
        if (!utilisateur.getEmail().equals(email) && utilisateurRepository.existsByEmail(email)) {
            throw new BadRequestException("Cet email est déjà utilisé par un autre utilisateur");
        }
        
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setEmail(email);
        utilisateur.setTelephone(telephone);
        
        return utilisateurRepository.save(utilisateur);
    }
    
    public void seedSuperAdmin() {
        // Vérifie si un SUPER_ADMIN existe déjà
        if (utilisateurRepository.findByRole(RoleType.SUPER_ADMIN).isEmpty()) {
            Utilisateur superAdmin = new Utilisateur();
            superAdmin.setNom("Super");
            superAdmin.setPrenom("Admin");
            superAdmin.setEmail("superadmin@yopmail.com");
            superAdmin.setMotDePasseHash(passwordEncoder.encode("StrongPassword123!"));
            superAdmin.setRole(RoleType.SUPER_ADMIN);
            superAdmin.setActif(true);

            utilisateurRepository.save(superAdmin);
            System.out.println("SUPER_ADMIN créé: superadmin@yopmail.com / StrongPassword123!");
        } else {
            System.out.println("SUPER_ADMIN déjà présent");
        }
    }

}


