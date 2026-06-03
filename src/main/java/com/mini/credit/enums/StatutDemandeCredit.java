package com.mini.credit.enums;

/**
 * Statuts du workflow de demande crédit avec validation stricte PHASE 4.
 *
 * Workflow:
 * BROUILLON → SOUMISE → EN_ANALYSE → ANALYSE_TERRAIN_VALIDEE
 *   → VALIDATION_CHEF → VALIDATION_CONTROLEUR → APPROUVEE
 *
 * Ou rejection à tout moment avec statut REJETEE.
 */
public enum StatutDemandeCredit {

    BROUILLON("Brouillon - Non soumise"),
    SOUMISE("Soumise - En attente d'analyse"),
    EN_ANALYSE("En analyse - Vérification terrain"),
    ANALYSE_TERRAIN_VALIDEE("Analyse terrain validée - Attente validation chef"),
    VALIDATION_CHEF("Validation chef - Attente validation contrôleur"),
    VALIDATION_CONTROLEUR("Validation contrôleur - Attente approbation"),
    APPROUVEE("Approuvée - Crédit créé"),
    REJETEE("Rejetée - Demande refusée"),
    ANNULEE("Annulée - Demande annulée");

    private final String libelle;

    StatutDemandeCredit(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
