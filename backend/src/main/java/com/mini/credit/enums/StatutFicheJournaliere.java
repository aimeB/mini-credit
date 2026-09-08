package com.mini.credit.enums;

/**
 * PHASE 6B.1: Statut des fiches journalières consolidées agent terrain.
 *
 * Workflow:
 * BROUILLON → SOUMISE → VALIDEE (ou REJETEE)
 * REJETEE → BROUILLON (agent retravaille)
 * VALIDEE ou REJETEE → ANNULEE (admin cancel)
 */
public enum StatutFicheJournaliere {
    BROUILLON("Créée, recettes encodées, pas encore consolidée"),
    SOUMISE("Consolidation effectuée, en attente validation contrôleur"),
    VALIDEE("Validée par contrôleur, variance OK, prête pour génération"),
    REJETEE("Rejetée par contrôleur, variance > seuil"),
     ANNULEE("Annulée (admin ou système)");

    private final String description;

    StatutFicheJournaliere(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this is a terminal state (no further transitions)
     */
    public boolean isTerminal() {
        return this == ANNULEE;
    }

    /**
     * Check if valid transition to target status
     */
    public boolean canTransitionTo(StatutFicheJournaliere target) {
        if (target == null) return false;
        if (this == ANNULEE) return false; // Terminal state - no transitions

        if (this == target) return true;

        return switch (this) {
            case BROUILLON -> target == SOUMISE || target == ANNULEE;
            case SOUMISE -> target == VALIDEE || target == REJETEE || target == ANNULEE;
            case VALIDEE -> target == ANNULEE;
            case REJETEE -> target == BROUILLON || target == ANNULEE;
            case ANNULEE -> false; // Terminal state
        };
    }
}
