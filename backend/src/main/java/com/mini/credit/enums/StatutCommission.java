package com.mini.credit.enums;

/**
 * PHASE 8: Statut des commissions agents.
 *
 * Workflow:
 * CREEE → VALIDEE → PAYEE (ou ANNULEE)
 */
public enum StatutCommission {
    CREEE("Créée, en attente de validation"),
    VALIDEE("Validée, prête à payer"),
    PAYEE("Payée à l'agent"),
    ANNULEE("Annulée");

    private final String description;

    StatutCommission(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
