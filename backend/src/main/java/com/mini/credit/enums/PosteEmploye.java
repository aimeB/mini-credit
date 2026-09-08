package com.mini.credit.enums;

/**
 * Énumération des postes/fonctions métier des employés.
 * Définit les postes spécifiques pour chaque rôle utilisateur.
 * Un même rôle peut avoir plusieurs postes métier.
 */
public enum PosteEmploye {
    /**
     * Chef de Bureau/Antenne - Supervision locale
     */
    CHEF_BUREAU("Chef de Bureau", "Responsable d'une antenne/bureau"),

    /**
     * Gestionnaire/Superviseur Terrain - Supervision agents terrain
     */
    GESTIONNAIRE("Gestionnaire", "Superviseur terrain, gestion agents terrain"),

    /**
     * Contrôleur - Validation et réconciliation caisse
     */
    CONTROLEUR("Contrôleur", "Validation recettes, retraits, réconciliation caisse"),

    /**
     * Caissier - Gestion des opérations caisse
     */
    CAISSIER("Caissier", "Gestion caisse, décaissements, dépôts"),

    /**
     * Agent Terrain - Prospection et suivi membres
     */
    AGENT_TERRAIN("Agent Terrain", "Prospection, suivi membres sur site"),

    /**
     * Chargé d'Opérations - Multi-antennes
     */
    CHARGE_OPERATIONS("Chargé d'Opérations", "Supervision multi-antennes, audit terrain"),

    /**
     * Responsable Contrôles Internes - Multi-antennes
     */
    RESPONSABLE_CONTROLES("Responsable Contrôles Internes", "Audit interne, réconciliation inter-antennes"),

    /**
     * Gérant Général - Top management
     */
    GERANT_GENERAL("Gérant Général", "Direction générale, top management"),

    /**
     * Chief Operating Officer - Direction des opérations
     */
    COO("COO / Directeur des Opérations", "Direction des opérations, coordination inter-agences"),

    /**
     * Responsable du Contrôle Interne - Audit interne
     */
    RCI("RCI / Responsable Contrôle Interne", "Audit interne, contrôle des opérations — rôle distinct du Contrôleur"),

    /**
     * Administratif - Support et administration
     */
    ADMINISTRATIF("Administratif", "Gestion administrative, secrétariat, support"),

    /**
     * Autre - Poste non listé
     */
    AUTRE("Autre", "Poste non catégorisé dans la liste standard");

    private final String libelle;
    private final String description;

    PosteEmploye(String libelle, String description) {
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
