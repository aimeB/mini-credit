package com.mini.credit.enums;

/**
 * PHASE 7: Statut des écarts caisse.
 *
 * Workflow:
 * DETECTE → EN_INVESTIGATION → RESOLU (ou ACCEPTE, REJETE)
 */
public enum StatutEcartCaisse {
    DETECTE("Écart détecté, investigation initiée"),
    EN_INVESTIGATION("En cours d'investigation (CONTROLEUR)"),
    RESOLU("Écart résolu (différence justifiée/corrigée)"),
    ACCEPTE("Écart accepté par R.C.I. (variance normalisée)"),
    REJETE("Écart rejeté (présumé erreur système)");

    private final String description;

    StatutEcartCaisse(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
