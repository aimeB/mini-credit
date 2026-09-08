package com.mini.credit.enums;

public enum CategorieParametre {
    TAUX_INTERET("Taux d'intérêt"),
    FRAIS("Frais"),
    SEUIL("Seuil"),
    LIMITE("Limite"),
    GENERAL("Général");

    private final String libelle;

    CategorieParametre(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
