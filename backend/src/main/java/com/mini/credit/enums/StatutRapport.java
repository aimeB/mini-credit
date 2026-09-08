package com.mini.credit.enums;

/**
 * PHASE 12: Statuts des rapports financiers
 *
 * - GENERE: Rapport généré (prêt à consultation)
 * - EN_GENERATION: Rapport en cours de génération (batch long)
 * - VALIDE: Rapport validé (approuvé par admin)
 * - ARCHIVE: Rapport archivé (pas modifiable)
 * - ERREUR: Génération échouée
 */
public enum StatutRapport {

    GENERE,
    EN_GENERATION,
    VALIDE,
    ARCHIVE,
    ERREUR
}
