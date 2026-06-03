package com.mini.credit.enums;

/**
 * Statuts du workflow de demande de retrait épargne (PHASE 5).
 *
 * Workflow:
 * CREEE → EN_ATTENTE_VALIDATION
 *   → VALIDEE → DECAISSEE
 *   OU REJETEE
 *
 * ANNULEE possible à tout moment.
 */
public enum StatutDemandeRetrait {

    CREEE("Créée - Demande initiale"),
    EN_ATTENTE_VALIDATION("En attente de validation CONTROLEUR"),
    VALIDEE("Validée - Prête pour décaissement"),
    REJETEE("Rejetée - Solde insuffisant ou autre motif"),
    DECAISSEE("Décaissée - Retrait effectué par caissier"),
    ANNULEE("Annulée - Annulation par demandeur");

    private final String libelle;

    StatutDemandeRetrait(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
