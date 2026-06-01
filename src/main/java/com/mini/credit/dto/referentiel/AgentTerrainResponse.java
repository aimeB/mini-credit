package com.mini.credit.dto.referentiel;

import com.mini.credit.entity.referentiel.AgentTerrain;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgentTerrainResponse {
    private Long id;
    private String matricule;
    private Long utilisateurId;
    private Long siteId;
    private String nomCompletUtilisateur;
    private String username;

    public static AgentTerrainResponse fromEntity(AgentTerrain agent) {
        return AgentTerrainResponse.builder()
                .id(agent.getId())
                .matricule(agent.getMatricule())
                .utilisateurId(agent.getUtilisateur() != null ? agent.getUtilisateur().getId() : null)
                .siteId(agent.getSite() != null ? agent.getSite().getId() : null)
                .nomCompletUtilisateur(
                        agent.getUtilisateur() != null ? agent.getUtilisateur().getNomComplet() : null
                )
                .username(
                        agent.getUtilisateur() != null ? agent.getUtilisateur().getUsername() : null
                )
                .build();
    }
}