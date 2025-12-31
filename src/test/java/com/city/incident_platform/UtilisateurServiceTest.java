package com.city.incident_platform;

import com.smartcity.incident_management.dto.InscriptionDTO;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.RoleType;
import com.smartcity.incident_management.exceptions.BadRequestException;
import com.smartcity.incident_management.exceptions.ResourceNotFoundException;
import com.smartcity.incident_management.repository.DepartementRepository;
import com.smartcity.incident_management.repository.UtilisateurRepository;
import com.smartcity.incident_management.services.utilisateur.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UtilisateurServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private DepartementRepository departementRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UtilisateurService utilisateurService;

    private InscriptionDTO inscriptionDTO;

    @BeforeEach
    void setUp() {
        inscriptionDTO = new InscriptionDTO();
        inscriptionDTO.setNom("Doe");
        inscriptionDTO.setPrenom("John");
        inscriptionDTO.setEmail("john.doe@test.com");
        inscriptionDTO.setTelephone("12345678");
        inscriptionDTO.setMotDePasse("Password123!");
        inscriptionDTO.setConfirmationMotDePasse("Password123!");
    }

    // ========== TEST UNITAIRE 1 : Inscription citoyen avec succès ==========
    @Test
    void testInscrireCitoyen_Success() {
        when(utilisateurRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(i -> {
            Utilisateur user = i.getArgument(0);
            user.setId(1L);
            return user;
        });

        Utilisateur result = utilisateurService.inscrireCitoyen(inscriptionDTO);

        assertNotNull(result);
        assertEquals(RoleType.CITOYEN, result.getRole());
        assertTrue(result.isActif());
        verify(utilisateurRepository, times(1)).save(any(Utilisateur.class));
    }

    // ========== TEST UNITAIRE 2 : Email déjà existant ==========
    @Test
    void testInscrireCitoyen_EmailExistant() {
        when(utilisateurRepository.existsByEmail("john.doe@test.com")).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> utilisateurService.inscrireCitoyen(inscriptionDTO));

        assertTrue(exception.getMessage().contains("email existe déjà"));
        verify(utilisateurRepository, never()).save(any());
    }

    // ========== TEST UNITAIRE 3 : Mots de passe ne correspondent pas ==========
    @Test
    void testInscrireCitoyen_MotsDePasseNonCorrespondants() {
        inscriptionDTO.setConfirmationMotDePasse("DifferentPassword");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> utilisateurService.inscrireCitoyen(inscriptionDTO));

        assertTrue(exception.getMessage().contains("mots de passe ne correspondent pas"));
        verify(utilisateurRepository, never()).save(any());
    }
}