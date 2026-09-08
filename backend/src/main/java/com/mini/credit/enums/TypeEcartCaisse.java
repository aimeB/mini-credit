package com.mini.credit.enums;

/**
 * PHASE 7: Types d'écarts caisse.
 */
public enum TypeEcartCaisse {
    DEFICIT("Déficit caisse (argent manquant)"),
    EXCEDENT("Excédent caisse (argent supplémentaire)"),
    VARIANCE_RECETTE("Variance sur recette journalière (cash != montant)"),
    VARIANCE_SESSION("Variance sur session caisse (solde physique != théorique)");

    private final String libelle;

    TypeEcartCaisse(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
