package com.mini.credit.enums.security;

/**
 * Énumération des codes de rôle du système.
 * Principaux rôles pour l'application mini-crédit dans une logique de séparation des tâches.
 */
public enum RoleCode {
    /**
     * Super administrateur - accès complet à toutes les fonctionnalités
     */
    ADMIN("Administrateur système", "Super administrateur avec accès complet"),

    /**
     * Responsable de bureau - supervision et validation des opérations
     */
    RESPONSABLE("Responsable bureau", "Supervision, validation des crédits, rapports"),

    /**
     * Agent de bureau - saisie des données, analyse administrative
     */
    AGENT_BUREAU("Agent de bureau", "Saisie membres, demandes crédit, suivi administratif"),

    /**
     * Agent de terrain - prospection et suivi membres sur site
     */
    AGENT_TERRAIN("Agent terrain", "Prospection, suivi membres, saisie initiale"),

    /**
     * Caissier - opérations de caisse uniquement
     */
    CAISSIER("Caissier", "Gestion caisse, versements, retraits, remboursements"),

    /**
     * Contrôleur - validation et réconciliation caisse/épargne
     */
    CONTROLEUR("Contrôleur", "Validation recettes, retraits, crédits, réconciliation caisse"),

    /**
     * Membre/Client - accès limité à ses propres données
     */
    MEMBER("Membre client", "Accès à son profil, ses crédits et comptes épargne");

    private final String libelle;
    private final String description;

    RoleCode(String libelle, String description) {
        this.libelle = libelle;
        this.description = description;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getDescription() {
        return description;
    }
}
