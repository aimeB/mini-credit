package com.mini.credit.enums;

/**
 * PHASE 6: Types de recettes journalières terrain.
 */
public enum TypeRecette {
    DEPOT("Dépôt épargne"),
    REMBOURSEMENT_CREDIT("Remboursement crédit"),
    INTERET("Intérêts épargne"),
    FRAIS("Frais service"),
    AUTRE("Autre");

    private final String libelle;

    TypeRecette(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
