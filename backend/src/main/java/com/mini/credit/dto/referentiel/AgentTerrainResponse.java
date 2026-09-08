package com.mini.credit.dto.referentiel;

import com.mini.credit.entity.referentiel.AgentTerrain;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Builder
public class AgentTerrainResponse {
    private Long id;
    private String matricule;
    private Long utilisateurId;
    private String nomCompletUtilisateur;
    private String username;
    /** Nom complet depuis la fiche Employé liée à l'utilisateur */
    private String employeNomComplet;
    /** ID de l'employé lié à l'utilisateur (utile pour ré-affectation agence) */
    private Long employeId;
    /** Téléphone depuis la fiche Employé liée à l'utilisateur */
    private String employeTelephone;
    /** ID de l'agence de l'employé agent terrain (pour détection d'incohérence) */
    private Long agentAgenceId;
    /** Nom de l'agence de l'employé agent terrain */
    private String agentAgenceNom;
    private Long siteId;
    private String sitePrincipalNom;
    /** ID de l'agence du site principal */
    private Long siteAgenceId;
    /** Nom de l'agence du site principal */
    private String siteAgenceNom;
    private Long gestionnaireId;
    private String gestionnaireNomComplet;
    /** Téléphone du gestionnaire (employé) */
    private String gestionnaireTelephone;
    /** ID de l'agence du gestionnaire (pour détection d'incohérence) */
    private Long gestionnaireAgenceId;
    /** Nom de l'agence du gestionnaire */
    private String gestionnaireAgenceNom;
    private LocalDate dateAffectation;
    private Boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    
    // Phase 4: Additional assigned sites
    private Set<Long> siteIds;

    public static AgentTerrainResponse fromEntity(AgentTerrain agent) {
        return AgentTerrainResponse.builder()
                .id(agent.getId())
                .matricule(agent.getMatricule())
                .utilisateurId(agent.getUtilisateur() != null ? agent.getUtilisateur().getId() : null)
                .nomCompletUtilisateur(
                        agent.getUtilisateur() != null ? agent.getUtilisateur().getNomComplet() : null
                )
                .username(
                        agent.getUtilisateur() != null ? agent.getUtilisateur().getUsername() : null
                )
                .siteId(agent.getSite() != null ? agent.getSite().getId() : null)
                .sitePrincipalNom(agent.getSite() != null ? agent.getSite().getNomSite() : null)
                .gestionnaireId(agent.getGestionnaire() != null ? agent.getGestionnaire().getId() : null)
                .gestionnaireNomComplet(agent.getGestionnaire() != null ? agent.getGestionnaire().getNomComplet() : null)
                .dateAffectation(agent.getDateAffectation())
                .actif(agent.getActif())
                .dateCreation(agent.getDateCreation())
                .dateModification(agent.getDateModification())
                .siteIds(agent.getSitesAffectes() != null 
                    ? agent.getSitesAffectes().stream()
                        .map(site -> site.getId())
                        .collect(Collectors.toSet())
                    : Set.of())
                .build();
    }
}