package com.mini.credit.enums;

public enum TypeParametre {
    DECIMAL("Décimal"),
    ENTIER("Entier"),
    TEXTE("Texte");

    private final String libelle;

    TypeParametre(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
