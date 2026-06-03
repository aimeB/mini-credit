package com.mini.credit.service;

import com.mini.credit.dto.caisse.EcartCaisseDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PHASE 7: Service pour écarts caisse.
 */
public interface EcartCaisseService {

    /**
     * Crée un écart caisse détecté
     */
    EcartCaisseDTO detecterEcart(
            Long sessionCaisseId, Long recetteId, LocalDate dateJour, String typeEcart,
            BigDecimal montantEcart, String description, Boolean seuilDepassé);

    /**
     * Récupère un écart par ID
     */
    EcartCaisseDTO getById(Long id);

    /**
     * Récupère les écarts détectés pour un jour
     */
    List<EcartCaisseDTO> getByDateJour(LocalDate dateJour);

    /**
     * Récupère les écarts en investigation (CONTROLEUR)
     */
    List<EcartCaisseDTO> getEnInvestigation();

    /**
     * Récupère les écarts nécessitant validation R.C.I.
     */
    List<EcartCaisseDTO> getRequiringRCIValidation();

    /**
     * PHASE 7: Enquête d'un écart (CONTROLEUR)
     * Passe en EN_INVESTIGATION
     */
    EcartCaisseDTO enqueterEcart(Long ecartId, String notesInvestigation);

    /**
     * PHASE 7: Résout un écart (CONTROLEUR)
     * Passe en RESOLU avec raison
     */
    EcartCaisseDTO resoudreEcart(Long ecartId, String raisonResolution);

    /**
     * PHASE 7: Valide un écart (R.C.I.)
     * Passe en ACCEPTE (variance normalisée)
     */
    EcartCaisseDTO accepterEcart(Long ecartId);

    /**
     * Rejette un écart (présumé erreur)
     */
    EcartCaisseDTO rejeterEcart(Long ecartId);

    /**
     * Total écarts non résolus pour un jour
     */
    BigDecimal getTotalEcartsNonResolusByDateJour(LocalDate dateJour);
}
