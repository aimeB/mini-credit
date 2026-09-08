package com.mini.credit.service;

import com.mini.credit.dto.credit.PenaliteCreditDTO;
import com.mini.credit.enums.StatutPenalite;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PHASE 10: Service pour gestion des pénalités de retard crédit
 *
 * Logique:
 * - Montant = PENALITE_RETARD_JOURNALIERE × nombre_jours_retard
 * - Batch job quotidien: 00:01 chaque jour
 * - Vérification crédit.dateEchéance < today
 * - Création automatique si retard détecté
 */
public interface PenaliteCreditService {

    /**
     * Crée une pénalité pour un crédit en retard
     *
     * @param creditId ID du crédit
     * @return PenaliteCreditDTO créée
     */
    PenaliteCreditDTO creerPenalite(Long creditId);

    /**
     * Vérifie et crée les pénalités pour TOUS les crédits en retard
     * Batch job: executé quotidiennement à 00:01
     *
     * @return List des pénalités créées
     */
    List<PenaliteCreditDTO> genererPenalitesTous();

    /**
     * Récupère les pénalités d'un crédit
     *
     * @param creditId ID du crédit
     * @return List des pénalités
     */
    List<PenaliteCreditDTO> getByCreditId(Long creditId);

    /**
     * Récupère les pénalités d'une demande de crédit
     *
     * @param demandeCreditId ID de la demande
     * @return List des pénalités
     */
    List<PenaliteCreditDTO> getByDemandeCreditId(Long demandeCreditId);

    /**
     * Récupère les pénalités en attente (non payées)
     *
     * @return List des pénalités CREEES
     */
    List<PenaliteCreditDTO> getEnAttente();

    /**
     * Acquitte une pénalité (marquer comme payée)
     *
     * @param penaliteId ID de la pénalité
     * @return PenaliteCreditDTO mise à jour
     */
    PenaliteCreditDTO acquitterPenalite(Long penaliteId);

    /**
     * Efface une pénalité (pardon/remise)
     *
     * @param penaliteId ID de la pénalité
     * @param motif Motif de l'effacement
     * @return PenaliteCreditDTO mise à jour
     */
    PenaliteCreditDTO effacerPenalite(Long penaliteId, String motif);

    /**
     * Récupère le montant total des pénalités pour un crédit
     *
     * @param creditId ID du crédit
     * @return Somme des montants
     */
    BigDecimal getTotalPenalitesCredit(Long creditId);

    /**
     * Récupère les pénalités en attente pour un crédit
     *
     * @param creditId ID du crédit
     * @return Somme montants CREEES uniquement
     */
    BigDecimal getTotalPenalitesEnAttenteCredit(Long creditId);

    /**
     * Calcule le montant d'une pénalité pour X jours de retard
     *
     * @param joursRetard Nombre de jours
     * @return Montant calculé
     */
    BigDecimal calculerMontantPenalite(Long joursRetard);

    /**
     * Récupère le taux de pénalité journalière (PHASE 1)
     *
     * @return Taux en BigDecimal
     */
    BigDecimal getTauxPenaliteJournaliere();
}
