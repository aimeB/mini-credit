package com.mini.credit.service;

import com.mini.credit.dto.referentiel.*;
import com.mini.credit.enums.RecetteStatut;
import org.springframework.data.domain.Page;

public interface CollecteTerrainService {

    CollecteTerrainResponse getToday();

    CollecteTerrainResponse getById(Long collecteId);

    CollecteTerrainResponse create(CreateCollecteTerrainRequest request);

    CollecteMembreLigneResponse addLigne(Long collecteId, CreateCollecteMembreLigneRequest request);

    CollecteMembreLigneResponse updateLigne(Long collecteId, Long ligneId, UpdateCollecteMembreLigneRequest request);

    void deleteLigne(Long collecteId, Long ligneId);

    CollecteTerrainResponse soumettre(Long collecteId, CreateCollecteTerrainRequest request);

    CollecteTerrainResponse confirmerBilletage(Long collecteId, ConfirmerBilletageRequest request);

    CollecteTerrainResponse valider(Long collecteId, ValidateCollecteTerrainRequest request);

    CollecteTerrainResponse rejeter(Long collecteId, ValidateCollecteTerrainRequest request);

    CollecteRecapResponse recap(Long collecteId);

    Page<CollecteTerrainResponse> list(
        RecetteStatut statut,
        java.time.LocalDate dateDebut,
        java.time.LocalDate dateFin,
        Long agentId,
        Long siteId,
        Long antenneId,
        int page,
        int size
    );
}
