package com.smartcity.incident_management.services.utilisateur;

import com.smartcity.incident_management.entities.Departement;
import com.smartcity.incident_management.enums.CategorieDepartement;
import com.smartcity.incident_management.exceptions.BadRequestException;
import com.smartcity.incident_management.exceptions.ResourceNotFoundException;
import com.smartcity.incident_management.repository.DepartementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DepartementService {
    
    @Autowired
    private DepartementRepository departementRepository;
    
    public Departement creer(CategorieDepartement nom, String description) {
        if (departementRepository.existsByNom(nom)) {
            throw new BadRequestException("Un département avec ce nom existe déjà");
        }
        
        Departement departement = new Departement();
        departement.setNom(nom);
        departement.setDescription(description);
        
        return departementRepository.save(departement);
    }
    
    public Departement findById(Long id) {
        return departementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé"));
    }
    
    public List<Departement> findAll() {
        return departementRepository.findAll();
    }
    
    public Departement findByNom(CategorieDepartement nom) {
        return departementRepository.findByNom(nom)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé"));
    }
}


