package com.mini.credit.mapper;

import com.mini.credit.dto.employe.CreateEmployeRequest;
import com.mini.credit.dto.employe.EmployeDTO;
import com.mini.credit.dto.employe.UpdateEmployeRequest;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.enums.PosteEmploye;
import org.springframework.stereotype.Component;

/**
 * Mapper pour convertir Employe entity ↔ DTOs
 * 
 * Responsabilités:
 * - toDTO: Entity → Response DTO (inclut ID + timestamps)
 * - toEntity: CreateRequest → Entity (pour création)
 * - updateEntityFromDTO: UpdateRequest → Entity (pour mise à jour)
 * 
 * Pattern: Null-safety systématique, préservation des champs immuables
 */
@Component
public class EmployeMapper {

    /**
     * Convertit une entité Employe en DTO (Response)
     * Inclut tous les champs y compris ID et timestamps
     * 
     * @param entity Employe entity à convertir
     * @return EmployeDTO avec tous les champs, ou null si entity null
     */
    public EmployeDTO toDTO(Employe entity) {
        if (entity == null) {
            return null;
        }

        boolean agentTerrain = entity.getFonction() == PosteEmploye.AGENT_TERRAIN;

        return EmployeDTO.builder()
            .id(entity.getId())
            .matricule(entity.getMatricule())
            .nom(entity.getNom())
            .prenom(entity.getPrenom())
            .nomComplet(entity.getNomComplet())
            .telephone(entity.getTelephone())
            .photoUrl(entity.getPhotoUrl())
            .adresse(entity.getAdresse())
            .commune(entity.getCommune())
            .fonction(entity.getFonction())
            .dateEmbauche(entity.getDateEmbauche())
            .salaireBase(entity.getSalaireBase())
            .primeFixe(entity.getPrimeFixe())
            .bonusVariable(entity.getBonusVariable())
            .totalRemuneration(entity.getTotalRemuneration())
            .actif(entity.getActif())
            .agenceId(entity.getAgence() != null ? entity.getAgence().getId() : null)
            .nomAgence(entity.getAgence() != null ? entity.getAgence().getNomAgence() : null)
            .siteId(agentTerrain && entity.getSite() != null ? entity.getSite().getId() : null)
            .nomSite(agentTerrain && entity.getSite() != null ? entity.getSite().getNomSite() : null)
            .utilisateurId(entity.getUtilisateur() != null ? entity.getUtilisateur().getId() : null)
            .roleUtilisateur(entity.getUtilisateur() != null ?
                entity.getUtilisateur().getRole().getCode().toString() : null)
            .dateCreation(entity.getDateCreation())
            .dateModification(entity.getDateModification())
            .build();
    }

    /**
     * Convertit une CreateEmployeRequest en entité Employe
     * Exclut ID et timestamps (générés par la DB)
     * Normalise le matricule en UPPERCASE
     * 
     * @param request Request DTO avec champs création
     * @return Employe entity avec champs normalisés, ou null si request null
     */
    public Employe toEntity(CreateEmployeRequest request) {
        if (request == null) {
            return null;
        }

        return Employe.builder()
            // Note: matricule généré automatiquement par MatriculeGeneratorService — pas via le mapper
            .nomComplet(request.getNom() + " " + request.getPrenom())
            .nom(request.getNom())
            .prenom(request.getPrenom())
            .telephone(request.getTelephone())
            .photoUrl(normalizePhotoUrl(request.getPhotoUrl()))
            .adresse(request.getAdresse())
            .commune(request.getCommune())
            .fonction(request.getFonction())
            .dateEmbauche(request.getDateEmbauche())
            .salaireBase(request.getSalaireBase())
            .primeFixe(request.getPrimeFixe() != null ? request.getPrimeFixe() : java.math.BigDecimal.ZERO)
            .bonusVariable(request.getBonusVariable() != null ? request.getBonusVariable() : java.math.BigDecimal.ZERO)
            // Note: agence_id, site_id et utilisateur_id sont injectés par le service
            .actif(true)
            .build();
    }

    /**
     * Met à jour une entité Employe existante à partir d'un UpdateRequest
     * Préserve les champs immuables: id, matricule, dateCreation, agence_id
     * 
     * @param request UpdateRequest avec champs à mettre à jour
     * @param entity Employe entity à mettre à jour
     */
    public void updateEntityFromDTO(UpdateEmployeRequest request, Employe entity) {
        if (request == null || entity == null) {
            return;
        }

        // Champs modifiables
        if (request.getNom() != null) {
            entity.setNom(request.getNom());
            // Update nomComplet aussi
            entity.setNomComplet(request.getNom() + " " + 
                (request.getPrenom() != null ? request.getPrenom() : entity.getPrenom()));
        }

        if (request.getPrenom() != null) {
            entity.setPrenom(request.getPrenom());
            // Update nomComplet aussi
            entity.setNomComplet((request.getNom() != null ? request.getNom() : entity.getNom()) + 
                " " + request.getPrenom());
        }

        if (request.getTelephone() != null) {
            entity.setTelephone(request.getTelephone());
        }

        if (request.isPhotoUrlProvided()) {
            entity.setPhotoUrl(normalizePhotoUrl(request.getPhotoUrl()));
        }

        if (request.getAdresse() != null) {
            entity.setAdresse(request.getAdresse());
        }

        if (request.getCommune() != null) {
            entity.setCommune(request.getCommune());
        }

        if (request.getFonction() != null) {
            entity.setFonction(request.getFonction());
        }

        if (request.getDateEmbauche() != null) {
            entity.setDateEmbauche(request.getDateEmbauche());
        }

        if (request.getSalaireBase() != null) {
            entity.setSalaireBase(request.getSalaireBase());
        }

        if (request.getPrimeFixe() != null) {
            entity.setPrimeFixe(request.getPrimeFixe());
        }

        if (request.getBonusVariable() != null) {
            entity.setBonusVariable(request.getBonusVariable());
        }

        if (request.getActif() != null) {
            entity.setActif(request.getActif());
        }

        // Note: agence_id est modifiable mais validation requise (service layer)
        // Si request contient agenceId, le service gère la validation et l'assignation

        // Champs IMMUABLES (jamais modifiés):
        // - id: clé primaire
        // - matricule: identifiant unique métier
        // - dateCreation: audit trail
        // - agence_id: peut être modifié via service avec validation spéciale
    }

    private String normalizePhotoUrl(String photoUrl) {
        if (photoUrl == null || photoUrl.trim().isEmpty()) {
            return null;
        }

        String normalized = photoUrl.trim();
        if (normalized.length() > 500
                || normalized.matches(".*\\s.*")
                || !(normalized.startsWith("https://")
                    || normalized.startsWith("http://")
                    || (normalized.startsWith("/") && !normalized.startsWith("//")))
                || normalized.matches(".*[<>\\\"'].*")) {
            throw new IllegalArgumentException("photoUrl doit être une URL http(s) ou un chemin relatif valide");
        }

        return normalized;
    }
}
