package com.mini.credit.enums;

/**
 * Enum représentant les statuts possibles d'une recette terrain journalière
 * 
 * Workflow:
 * - BROUILLON: Recette en cours de remplissage par l'agent
 * - SOUMISE: Recette soumise pour validation par le responsable site
 * - VALIDEE: Recette validée par le responsable site ou contrôleur
 * - REJETEE: Recette rejetée pour correction par l'agent
 */
public enum RecetteStatut {
    BROUILLON("Brouillon"),
    SOUMISE("Soumise pour validation"),
    VALIDEE("Validée"),
    REJETEE("Rejetée");

    private final String libelle;

    RecetteStatut(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
