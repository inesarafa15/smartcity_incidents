package com.city.incident_platform;

import com.smartcity.incident_management.entities.Departement;
import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.CategorieDepartement;
import com.smartcity.incident_management.enums.StatutIncident;
import com.smartcity.incident_management.exceptions.ResourceNotFoundException;
import com.smartcity.incident_management.exceptions.UnauthorizedException;
import com.smartcity.incident_management.repository.IncidentRepository;
import com.smartcity.incident_management.repository.NotificationRepository;
import com.smartcity.incident_management.repository.PhotoRepository;
import com.smartcity.incident_management.services.email.EmailService;
import com.smartcity.incident_management.services.municipalite.IncidentMunicipaliteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IncidentMunicipaliteServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PhotoRepository photoRepository;

    @InjectMocks
    private IncidentMunicipaliteService incidentMunicipaliteService;

    private Utilisateur agent;
    private Departement departement;
    private Incident incident;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(incidentMunicipaliteService, "uploadDir", "uploads");

        // Département
        departement = new Departement();
        departement.setId(1L);
        departement.setNom(CategorieDepartement.SECURITE);
         

        // Agent
        agent = new Utilisateur();
        agent.setId(1L);
        agent.setNom("Agent");
        agent.setPrenom("Test");
        agent.setDepartement(departement);

        // Incident
        incident = new Incident();
        incident.setId(1L);
        incident.setTitre("Incident Test");
        incident.setStatut(StatutIncident.SIGNALE);
        incident.setDepartement(departement);
    }

    // ========== TEST UNITAIRE 1 : Récupérer incident par ID avec succès ==========
    @Test
    void testGetIncidentById_Success() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        Incident result = incidentMunicipaliteService.getIncidentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Incident Test", result.getTitre());
        verify(incidentRepository, times(1)).findById(1L);
    }

    // ========== TEST UNITAIRE 2 : Incident non trouvé ==========
    @Test
    void testGetIncidentById_NotFound() {
        when(incidentRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> incidentMunicipaliteService.getIncidentById(999L));

        assertTrue(exception.getMessage().contains("Incident non trouvé"));
    }

    // ========== TEST UNITAIRE 3 : Prise en charge avec succès ==========
    @Test
    void testPrendreEnCharge_Success() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).envoyerEmailChangementStatut(any(), any(), any(), any());
        doNothing().when(emailService).envoyerEmailAssignationAgent(any(), any(), any());

        Incident result = incidentMunicipaliteService.prendreEnCharge(1L, agent);

        assertNotNull(result);
        assertEquals(StatutIncident.PRIS_EN_CHARGE, result.getStatut());
        assertEquals(agent, result.getAgentAssigne());
        verify(incidentRepository, times(1)).save(any(Incident.class));
        verify(notificationRepository, times(1)).save(any());
    }
}