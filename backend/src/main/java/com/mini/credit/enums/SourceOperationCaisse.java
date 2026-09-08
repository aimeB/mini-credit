package com.mini.credit.enums;

/**
 * Source d'une opération caisse - détermine la traçabilité requise.
 * Utilisé pour valider que les champs de référence sont présents selon le type.
 */
public enum SourceOperationCaisse {
    /**
     * Phase 6B.2: Généré depuis une recette journalière terrain.
     * Requiert: recetteId NON-NULL
     */
    RECETTE_JOURNALIERE("Recette journalière terrain"),

    /**
     * Phase 6B.3: Retrait épargne d'un compte membre.
     * Requiert: operationEpargneId NON-NULL
     */
    RETRAIT_EPARGNE("Retrait épargne"),

    /**
     * Phase 6B.1: Décaissement crédit au membre.
     * Requiert: creditId NON-NULL
     */
    CREDIT_DECAISSEMENT("Décaissement crédit"),

    /**
     * Phase 6B.1: Remboursement crédit par le membre.
     * Requiert: creditId NON-NULL
     */
    CREDIT_REMBOURSEMENT("Remboursement crédit"),

    /**
     * Saisie manuelle: cotisation, frais, dépenses, etc.
     * Pas de FK obligatoire.
     */
    MANUEL("Entrée/sortie manuelle"),

    /**
     * Correction ou contrepassation d'une opération erronée.
     * Référence l'opération originale via description.
     */
    AJUSTEMENT("Ajustement/contrepassation"),

    /**
     * Approvisionnement externe de la caisse.
     */
    APPROVISIONNEMENT("Approvisionnement caisse"),

    /**
     * Remboursement d'apport propriétaire validé, payé via caisse.
     */
    REMBOURSEMENT_APPORT_PROPRIETAIRE("Remboursement apport propriétaire"),

    /**
     * Dépense caisse validée et payée.
     */
    DEPENSE_CAISSE("Dépense caisse"),

    /**
     * Autre source non classée.
     */
    AUTRE("Autre");

    private final String description;

    SourceOperationCaisse(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
