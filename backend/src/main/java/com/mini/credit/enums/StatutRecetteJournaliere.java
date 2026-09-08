package com.mini.credit.enums;

/**
 * PHASE 6: Statut des recettes journalières terrain.
 *
 * Workflow:
 * CREEE → EN_ATTENTE_VALIDATION → VALIDEE (ou REJETEE)
 */
public enum StatutRecetteJournaliere {
    CREEE("Créée, en cours d'encodage"),
    EN_ATTENTE_VALIDATION("En attente validation CONTROLEUR"),
    VALIDEE("Validée par CONTROLEUR (réconciliation OK)"),
    REJETEE("Rejetée (variance non conforme)"),
    ANNULEE("Annulée");

    private final String description;

    StatutRecetteJournaliere(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
