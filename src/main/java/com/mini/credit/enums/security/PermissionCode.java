package com.mini.credit.enums.security;

/**
 * Énumération des permissions granulaires du système.
 * Chaque permission représente une action métier spécifique.
 * Les rôles possèdent un ensemble de permissions.
 *
 * Maintien du least privilege : chaque rôle n'a que les permissions nécessaires.
 */
public enum PermissionCode {
    // ============ Gestion des utilisateurs et sécurité ============
    USER_READ("Voir les utilisateurs"),
    USER_CREATE("Créer un utilisateur"),
    USER_UPDATE("Modifier un utilisateur"),
    USER_ASSIGN_ROLE("Assigner des rôles"),
    USER_LOCK("Bloquer un utilisateur"),
    USER_UNLOCK("Débloquer un utilisateur"),

    // ============ Gestion des membres ============
    MEMBRE_READ("Voir les membres"),
    MEMBRE_CREATE("Créer un membre"),
    MEMBRE_UPDATE("Modifier un membre"),
    MEMBRE_CLOSE("Clôturer un membre"),
    MEMBRE_READ_SELF("Voir son propre profil"),
    MEMBRE_UPDATE_SELF("Modifier son profil"),

    // ============ Gestion des comptes épargne ============
    EPARGNE_COMPTE_CREATE("Créer un compte épargne"),
    EPARGNE_COMPTE_READ("Voir les comptes épargne"),
    EPARGNE_OPERATION_CREATE("Créer une opération épargne"),
    EPARGNE_OPERATION_READ("Voir les opérations épargne"),

    // ============ Gestion de la caisse (séparation critique) ============
    CAISSE_CREATE("Créer une caisse"),
    CAISSE_READ("Voir les caisses"),
    SESSION_CAISSE_OPEN("Ouvrir une session caisse"),
    SESSION_CAISSE_CLOSE("Clôturer une session caisse"),
    OPERATION_CAISSE_CREATE("Enregistrer une opération caisse"),
    OPERATION_CAISSE_READ("Voir les opérations caisse"),
    DASHBOARD_CAISSE_READ("Voir le dashboard caisse"),

    // ============ Gestion des crédits (séparation critique) ============
    DEMANDE_CREDIT_CREATE("Créer une demande crédit"),
    DEMANDE_CREDIT_READ("Voir une demande crédit"),
    ANALYSE_RISQUE_CREATE("Créer une analyse de risque"),
    ANALYSE_RISQUE_READ("Voir une analyse de risque"),
    CREDIT_APPROVE("Approuver un crédit (décision)"),        // SÉPARÉ du décaissement
    CREDIT_DISBURSE("Décaisser un crédit (trésorier)"),       // SÉPARÉ de l'approbation
    CREDIT_READ("Voir un crédit"),
    REMBOURSEMENT_CREATE("Enregistrer un remboursement"),
    REMBOURSEMENT_READ("Voir les remboursements"),

    // ============ Dashboard et audit ============
    DASHBOARD_GLOBAL_READ("Voir le dashboard global"),
    AUDIT_READ("Consulter l'audit"),

    // ============ Permissions CONTROLEUR (Validation & Réconciliation) ============
    CONTROLEUR_EPARGNE_READ("Voir les comptes épargne (CONTROLEUR)"),
    CONTROLEUR_EPARGNE_OPERATION_READ("Voir les opérations épargne (CONTROLEUR)"),
    CONTROLEUR_CREDIT_READ("Voir les crédits (CONTROLEUR)"),
    CONTROLEUR_DEMANDE_CREDIT_READ("Voir les demandes crédit (CONTROLEUR)"),
    CONTROLEUR_DEMANDE_CREDIT_VALIDATE("Valider/modifier analyse risque demandes crédit"),
    CONTROLEUR_SESSION_CAISSE_READ("Voir les SessionCaisse (CONTROLEUR)"),
    CONTROLEUR_SESSION_CAISSE_VALIDATE("Valider clôture SessionCaisse"),
    CONTROLEUR_ECART_READ("Voir les écarts caisse"),
    CONTROLEUR_ECART_VALIDATE("Valider/modifier écarts investigation"),
    CONTROLEUR_RECETTES_VALIDATE("Valider recettes journalières (réconciliation)"),
    CONTROLEUR_RETRAITS_VALIDATE("Valider retraits épargne (vérif solde)"),
    CONTROLEUR_CREDITS_VALIDATE("Valider crédits (garanties, frais, décaissement)"),
    CONTROLEUR_AUDIT_READ("Consulter audit logs antenne");

    private final String description;

    PermissionCode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
