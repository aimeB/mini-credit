package com.mini.credit.dto.referentiel;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * DTO représentant le portefeuille calculé d'un Gestionnaire.
 * Agrège ses agents terrain actifs, leurs sites et les membres couverts.
 * Pas d'entité Portefeuille — tout est calculé à la volée.
 */
@Getter
@Builder
public class PortefeuilleGestionnaireResponse {

    /** ID de l'employé Gestionnaire */
    private Long gestionnaireId;

    /** Nom complet du Gestionnaire */
    private String gestionnaireNomComplet;

    /** Agents terrain actifs supervisés par ce gestionnaire */
    private List<AgentTerrainResponse> agents;

    /** Sites distincts couverts par les agents du gestionnaire */
    private List<SiteResponse> sites;

    /** Nombre d'agents actifs */
    private int totalAgents;

    /** Nombre de sites distincts couverts */
    private int totalSites;

    /** Nombre total de membres affiliés aux sites couverts */
    private long totalMembres;
}
