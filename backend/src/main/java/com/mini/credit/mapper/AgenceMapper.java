package com.mini.credit.mapper;

import com.mini.credit.dto.AgenceDTO;
import com.mini.credit.dto.CreateAgenceRequest;
import com.mini.credit.dto.UpdateAgenceRequest;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.repository.EmployeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper pour convertir entre entité Agence et DTOs
 */
@Component
@RequiredArgsConstructor
public class AgenceMapper {
    
    private final EmployeRepository employeRepository;

    /**
     * Convertir entité Agence en DTO
     * @param entity Entité Agence
     * @return DTO AgenceDTO
     */
    public AgenceDTO toDTO(Agence entity) {
        if (entity == null) {
            return null;
        }
        return AgenceDTO.builder()
            .id(entity.getId())
            .codeAgence(entity.getCodeAgence())
            .nomAgence(entity.getNomAgence())
            .adresse(entity.getAdresse())
            .commune(entity.getCommune())
            .quartier(entity.getQuartier())
            .reference(entity.getReference())
            .telephone(entity.getTelephone())
            .email(entity.getEmail())
            .ville(entity.getVille())
            .actif(entity.getActif())
            .chefBureauId(entity.getChefBureau() != null ? entity.getChefBureau().getId() : null)
            .description(entity.getDescription())
            .dateCreation(entity.getDateCreation())
            .dateModification(entity.getDateModification())
            .build();
    }

    /**
     * Convertir CreateAgenceRequest en entité Agence
     * @param request DTO CreateAgenceRequest
     * @return Entité Agence
     */
    public Agence toEntity(CreateAgenceRequest request) {
        if (request == null) {
            return null;
        }
        return Agence.builder()
            .codeAgence(request.getCodeAgence() != null ? 
                request.getCodeAgence().toUpperCase() : null)
            .nomAgence(request.getNomAgence())
            .adresse(request.getAdresse())
            .commune(request.getCommune())
            .quartier(request.getQuartier())
            .reference(request.getReference())
            .telephone(request.getTelephone())
            .email(null)
            .ville(request.getVille())
            .actif(request.getActif())
            .description(request.getDescription())
            .build();
    }

    /**
     * Mettre à jour une entité Agence à partir d'UpdateAgenceRequest
     * @param request DTO UpdateAgenceRequest
     * @param entity Entité Agence à mettre à jour
     */
    public void updateEntityFromDTO(UpdateAgenceRequest request, Agence entity) {
        if (request == null || entity == null) {
            return;
        }
        entity.setNomAgence(request.getNomAgence());
        entity.setAdresse(request.getAdresse());
        entity.setCommune(request.getCommune());
        entity.setQuartier(request.getQuartier());
        entity.setReference(request.getReference());
        entity.setTelephone(request.getTelephone());
        entity.setVille(request.getVille());
        entity.setActif(request.getActif());
        entity.setDescription(request.getDescription());
        
        // 🆕 Chef de Bureau avec validation
        if (request.getChefBureauId() != null) {
            Employe chef = employeRepository.findById(request.getChefBureauId())
                .orElseThrow(() -> new RuntimeException("Employé (Chef de Bureau) non trouvé"));
            // setChefBureau valide que chef appartient à cette agence et est actif
            entity.setChefBureau(chef);
        } else {
            // Si null explicitement, retirer le chef
            entity.setChefBureau(null);
        }
    }
}
