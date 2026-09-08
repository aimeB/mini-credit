package com.mini.credit.service;

import com.mini.credit.dto.credit.CreditValidationResult;
import com.mini.credit.entity.credit.DemandeCredit;

/**
 * Service de validation stricte des demandes crédit (PHASE 4).
 *
 * Responsabilités:
 * - Vérifier frais de demande intégralement payés si un montant est attendu
 * - Vérifier garantie 20% déposée
 * - Vérifier analyse terrain complète et validée
 * - Déterminer si crédit peut être approuvé
 */
public interface CreditValidationService {

    /**
    * Valide si les frais de demande attendus sont intégralement payés.
     *
     * @param demandeCredit la demande à vérifier
    * @return true si fraisDemande <= 0 ou fraisDemandePayes >= fraisDemande
     */
    boolean validerFraisDemandePayes(DemandeCredit demandeCredit);

    /**
    * Bloque si fraisDemande > 0 et fraisDemandePayes < fraisDemande.
    *
    * @param demandeCredit la demande à vérifier
    */
    void verifierFraisDemandeIntegralementPayes(DemandeCredit demandeCredit);

    /**
     * Valide si la garantie déposée est >= 20% du montant demandé
     *
     * @param demandeCredit la demande à vérifier
     * @return true si garantie suffisante
     */
    boolean validerGarantieDeposee(DemandeCredit demandeCredit);

    /**
     * Valide si l'analyse terrain est complète et correctement validée
     *
     * @param demandeCredit la demande à vérifier
     * @return true si analyse terrain validée (adresse + emploi + terrain)
     */
    boolean validerAnalyseTerrainComplete(DemandeCredit demandeCredit);

    /**
     * Effectue la validation complète PHASE 4 avec tous les critères
     *
     * @param demandeCredit la demande à valider complètement
     * @return CreditValidationResult avec détails de validation
     */
    CreditValidationResult validerCreditComplet(DemandeCredit demandeCredit);

    /**
     * Retourne le montant de garantie requis (20% du montant demandé)
     *
     * @param montantDemande le montant de la demande
     * @return 20% du montant ou null
     */
    java.math.BigDecimal calculerGarantieRequise(java.math.BigDecimal montantDemande);
}
