package com.mini.credit.enums;

/**
 * PHASE 11: Statuts de réconciliation caisse automatique
 *
 * - CREEE: Réconciliation créée automatiquement (écart détecté)
 * - RAPPROCHEE: Écart résolu/rapproché
 * - REJETEE: Réconciliation rejetée (écart confirmé intentionnel)
 */
public enum StatutReconciliation {

    CREEE,
    RAPPROCHEE,
    REJETEE
}
