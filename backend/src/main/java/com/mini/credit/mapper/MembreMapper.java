package com.mini.credit.mapper;

import com.mini.credit.dto.membre.MembreResponse;
import com.mini.credit.entity.membre.Membre;
import org.springframework.stereotype.Component;

@Component
public class MembreMapper {

    public MembreResponse toResponse(Membre membre) {
        if (membre == null) {
            return null;
        }

        return MembreResponse.builder()
                .id(membre.getId())
                .codeMembre(membre.getCodeMembre())
                .nom(membre.getNom())
                .postnom(membre.getPostnom())
                .prenom(membre.getPrenom())
                .nomComplet(membre.getNomComplet())
                .sexe(membre.getSexe())
                .dateNaissance(membre.getDateNaissance())
                .telephonePrincipal(membre.getTelephonePrincipal())
                .telephoneSecondaire(membre.getTelephoneSecondaire())
                .email(membre.getUtilisateur() != null ? membre.getUtilisateur().getEmail() : null)
                .adresse(membre.getAdresse())
                .quartier(membre.getQuartier())
                .commune(membre.getCommune())
                .ville(membre.getVille())
                .professionActivite(membre.getProfessionActivite())
                .lieuActivite(membre.getLieuActivite())
                .sourceInscription(membre.getSourceInscription())
                .siteId(membre.getSite() != null ? membre.getSite().getId() : null)
                .siteNom(membre.getSite() != null ? membre.getSite().getNomSite() : null)
                .agentId(membre.getAgent() != null ? membre.getAgent().getId() : null)
                .agentMatricule(membre.getAgent() != null ? membre.getAgent().getMatricule() : null)
                .dateAdhesion(membre.getDateAdhesion())
                .statut(membre.getStatut())
                .photoUrl(membre.getPhotoUrl())
                .observation(membre.getObservation())
                .createdAt(membre.getDateCreation())
                .updatedAt(membre.getDateModification())
                .build();
    }
}