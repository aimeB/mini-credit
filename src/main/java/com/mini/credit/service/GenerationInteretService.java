package com.mini.credit.service;

import com.mini.credit.dto.epargne.OperationEpargneDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * PHASE 9: Service pour génération automatique des intérêts épargne
 *
 * Logique:
 * - Intérêt mensuel = soldeDisponible × TAUX_INTERET_EPARGNE / 12
 * - Crée OperationEpargne type INTERET (entrée)
 * - Incrémente soldeDisponible du compte
 * - Audit trail complet
 */
public interface GenerationInteretService {

    /**
     * Génère les intérêts pour UN compte épargne
     *
     * @param compteEpargneId ID du compte
     * @return OperationEpargneDTO créée
     */
    OperationEpargneDTO genererInteretCompte(Long compteEpargneId);

    /**
     * Génère les intérêts pour TOUS les comptes actifs
     * Appelé mensuel via @Scheduled (batch job)
     *
     * @return List des opérations créées
     */
    List<OperationEpargneDTO> genererInteretsTous();

    /**
     * Génère les intérêts pour tous les comptes d'un membre
     *
     * @param membreId ID du membre
     * @return List des opérations créées
     */
    List<OperationEpargneDTO> genererInteretsParMembre(Long membreId);

    /**
     * Calcule le montant d'intérêt pour un solde donné
     *
     * @param soldeDisponible Solde actuel
     * @return Montant intérêt = solde × taux / 12
     */
    BigDecimal calculerMontantInteret(BigDecimal soldeDisponible);

    /**
     * Récupère le taux intérêt épargne courant
     *
     * @return Taux (ex: 0.05 pour 5%)
     */
    BigDecimal getTauxInteretEpargne();

    /**
     * Récupère intérêts générés pour une période (stats)
     *
     * @param membreId ID du membre
     * @return Total intérêts générés
     */
    BigDecimal getTotalInteretsGeneres(Long membreId);
}
