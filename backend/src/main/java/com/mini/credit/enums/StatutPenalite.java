package com.mini.credit.enums;

/**
 * PHASE 10: Statut de pénalité de retard crédit
 *
 * Workflow:
 * CREEE → ACQUITTEE (payée)
 * CREEE → EFFACEE (pardon/annulation)
 */
public enum StatutPenalite {
    CREEE("Créée"),
    ACQUITTEE("Acquittée - payée"),
    EFFACEE("Effacée - pardon");

    private final String libelle;

    StatutPenalite(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
