package com.smartcity.incident_management.services.utilisateur;

import com.smartcity.incident_management.entities.Quartier;
import com.smartcity.incident_management.exceptions.ResourceNotFoundException;
import com.smartcity.incident_management.repository.QuartierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class QuartierService {
    
    @Autowired
    private QuartierRepository quartierRepository;
    
    public Quartier creer(String nom, String description, String codePostal) {
        Quartier quartier = new Quartier();
        quartier.setNom(nom);
        //quartier.setDescription(description);
        quartier.setCodePostal(codePostal);
        
        return quartierRepository.save(quartier);
    }
    
    public Quartier findById(Long id) {
        return quartierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quartier non trouvé"));
    }
    
    public List<Quartier> findAll() {
        return quartierRepository.findAll();
    }
    
    public List<Quartier> findByCodePostal(String codePostal) {
        return quartierRepository.findByCodePostal(codePostal);
    }
}


