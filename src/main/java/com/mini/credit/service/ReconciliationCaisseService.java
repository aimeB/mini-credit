package com.mini.credit.service;

import com.mini.credit.dto.caisse.ReconciliationCaisseDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * PHASE 11: Service Réconciliation Caisse Automatique
 *
 * Logique:
 * - Batch job quotidien après fermeture caisse
 * - Détecte SessionCaisse avec soldePhysique != soldeTheorique
 * - Crée ReconciliationCaisse automatiquement
 * - Permet rapprochement ou rejet manuel
 */
public interface ReconciliationCaisseService {

    /**
     * Batch job: génère réconciliations pour sessions fermées
     *
     * @return liste des réconciliations créées
     */
    List<ReconciliationCaisseDTO> genererReconciliationsToutes();

    /**
     * Crée réconciliation pour une session spécifique
     *
     * @param sessionCaisseId ID de la session
     * @return ReconciliationCaisseDTO créée
     */
    ReconciliationCaisseDTO creerReconciliation(Long sessionCaisseId);

    /**
     * Récupère réconciliations d'une session
     *
     * @param sessionCaisseId ID session
     * @return liste
     */
    List<ReconciliationCaisseDTO> getBySessionCaisseId(Long sessionCaisseId);

    /**
     * Récupère réconciliations en attente (statut CREEE)
     *
     * @return liste
     */
    List<ReconciliationCaisseDTO> getEnAttente();

    /**
     * Marque réconciliation comme rapprochée (écart résolu)
     *
     * @param reconciliationId ID
     * @param motif motif du rapprochement
     * @return ReconciliationCaisseDTO mise à jour
     */
    ReconciliationCaisseDTO rapprocheer(Long reconciliationId, String motif);

    /**
     * Rejette réconciliation (écart confirmé intentionnel)
     *
     * @param reconciliationId ID
     * @param raison raison du rejet
     * @return ReconciliationCaisseDTO mise à jour
     */
    ReconciliationCaisseDTO rejeter(Long reconciliationId, String raison);

    /**
     * Total écarts en attente
     *
     * @return montant total
     */
    BigDecimal getTotalEcartsEnAttente();

    /**
     * Nombre de réconciliations en attente
     *
     * @return nombre
     */
    Long getNombreReconciliationsEnAttente();
}
