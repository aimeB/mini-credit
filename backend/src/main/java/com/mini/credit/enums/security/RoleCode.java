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
    * Chef de bureau - rôle officiel 3N.
     */
    CHEF_BUREAU("Chef de Bureau", "Supervision, validation des crédits, rapports"),

    /**
    * Gestionnaire - rôle métier officiel pour la pré-analyse crédit.
     */
    GESTIONNAIRE("Gestionnaire", "Pré-analyse crédit, suivi administratif et supervision terrain"),

    /**
     * Agent de terrain - prospection et suivi membres sur site
     */
    AGENT_TERRAIN("Agent terrain", "Prospection, suivi membres, saisie initiale"),

    /**
     * Contrôleur - validation et réconciliation caisse/épargne
     */
    CONTROLEUR("Contrôleur", "Validation recettes, retraits, crédits, réconciliation caisse"),

    /**
     * Caissier - opérations de caisse uniquement
     */
    CAISSIER("Caissier", "Gestion caisse, versements, retraits, remboursements"),

    /**
     * COO - supervision transverse des opérations.
     *
     * RBAC-1: formalisation du rôle dans le référentiel technique.
     * La matrice détaillée des permissions est traitée dans RBAC-2.
     */
    COO("COO", "Supervision transverse des opérations et du pilotage métier"),

    /**
    * Responsable du Contrôle Interne (RCI) - rôle métier officiel distinct.
     *
     * PATCH 11 — Mapping 3N :
    * Le RCI est un rôle métier officiel distinct du Chef de Bureau
     * et du Contrôleur (CONTROLEUR). Ces trois rôles ne sont pas interchangeables.
     *
     * Périmètre RCI selon les documents 3N :
     *   - Audit et contrôle interne des opérations de caisse.
     *   - Investigation et enquête sur les écarts détectés.
     *   - Consultation de l'ensemble des écarts et rapports.
     *   - Supervision des contrôleurs et caissiers (sans rôle opérationnel dans la
     *     chaîne d'approbation commerciale).
     *   - Recommandations après audit (hors scope technique Phase 11).
     *
     * Ce que RCI NE fait PAS :
    *   - N'accepte pas les variances (= Chef de Bureau).
     *   - Ne valide pas les opérations courantes (= CONTROLEUR).
     *   - N'administre pas le système (= ADMIN).
     */
    RCI("Responsable du Contrôle Interne", "Audit, enquête, contrôle interne des opérations - rôle distinct du Chef de Bureau et du Contrôleur"),

    /**
     * Gérant Général - gouvernance et supervision globale.
     *
     * RBAC-1: formalisation du rôle dans le référentiel technique.
     * La matrice détaillée des permissions est traitée dans RBAC-2.
     */
    GERANT_GENERAL("Gérant Général", "Gouvernance globale et supervision de haut niveau"),

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
