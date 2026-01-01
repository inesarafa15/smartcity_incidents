package com.smartcity.incident_management;

import com.smartcity.incident_management.services.citoyen.IncidentCitoyenService;
import com.smartcity.incident_management.dto.IncidentDTO;
import com.smartcity.incident_management.entities.*;
import com.smartcity.incident_management.enums.PrioriteIncident;
import com.smartcity.incident_management.enums.StatutIncident;
import com.smartcity.incident_management.exceptions.BadRequestException;
import com.smartcity.incident_management.exceptions.ResourceNotFoundException;
import com.smartcity.incident_management.repository.DepartementRepository;
import com.smartcity.incident_management.repository.IncidentRepository;
import com.smartcity.incident_management.repository.PhotoRepository;
import com.smartcity.incident_management.repository.QuartierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IncidentCitoyenServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private DepartementRepository departementRepository;

    @Mock
    private QuartierRepository quartierRepository;

    @Mock
    private PhotoRepository photoRepository;

    @InjectMocks
    private IncidentCitoyenService incidentCitoyenService;

    private Utilisateur citoyen;
    private Departement departement;
    private Quartier quartier;
    private IncidentDTO incidentDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(incidentCitoyenService, "uploadDir", "uploads");
        ReflectionTestUtils.setField(incidentCitoyenService, "maxFiles", 5);
        ReflectionTestUtils.setField(incidentCitoyenService, "maxFileSize", 10485760L);

        citoyen = new Utilisateur();
        citoyen.setId(1L);

        departement = new Departement();
        departement.setId(1L);

        quartier = new Quartier();
        quartier.setId(1L);

        incidentDTO = new IncidentDTO();
        incidentDTO.setTitre("Test Incident");
        incidentDTO.setDescription("Description test");
        incidentDTO.setPriorite(PrioriteIncident.ELEVEE);
        incidentDTO.setLatitude(36.8065);
        incidentDTO.setLongitude(10.1815);
        incidentDTO.setDepartementId(1L);
        incidentDTO.setQuartierId(1L);
    }

    // ========== TEST UNITAIRE 1 : Signalement incident avec succès ==========
    @Test
    void testSignalerIncident_Success() throws IOException {
        when(departementRepository.findById(1L)).thenReturn(Optional.of(departement));
        when(quartierRepository.findById(1L)).thenReturn(Optional.of(quartier));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> {
            Incident incident = i.getArgument(0);
            incident.setId(1L);
            return incident;
        });

        Incident result = incidentCitoyenService.signalerIncident(citoyen, incidentDTO);

        assertNotNull(result);
        assertEquals(StatutIncident.SIGNALE, result.getStatut());
        verify(incidentRepository, times(1)).save(any(Incident.class));
    }

    // ========== TEST UNITAIRE 2 : GPS obligatoire ==========
    @Test
    void testSignalerIncident_GPSObligatoire() {
        incidentDTO.setLatitude(null);
        incidentDTO.setLongitude(null);
        when(departementRepository.findById(1L)).thenReturn(Optional.of(departement));
        when(quartierRepository.findById(1L)).thenReturn(Optional.of(quartier));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> incidentCitoyenService.signalerIncident(citoyen, incidentDTO));

        assertTrue(exception.getMessage().contains("position GPS"));
    }

    // ========== TEST UNITAIRE 3 : Département inexistant ==========
    @Test
    void testSignalerIncident_DepartementNotFound() {
        when(departementRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> incidentCitoyenService.signalerIncident(citoyen, incidentDTO));
    }
}