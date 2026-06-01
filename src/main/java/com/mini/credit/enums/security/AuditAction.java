package com.mini.credit.enums.security;

/**
 * Énumération des actions auditées du système.
 * Toutes les opérations sensibles et les événements de sécurité doivent être loggés.
 *
 * OWASP recommande : journaliser les événements de sécurité, opérations sensibles,
 * avec contexte, résultat et protection contre les manipulations.
 */
public enum AuditAction {
    // ============ Authentification ============
    LOGIN_SUCCESS("Connexion réussie"),
    LOGIN_FAILURE("Tentative de connexion échouée"),
    LOGOUT("Déconnexion"),

    // ============ Gestion des utilisateurs ============
    USER_CREATED("Utilisateur créé"),
    USER_UPDATED("Utilisateur modifié"),
    USER_ROLE_CHANGED("Rôle utilisateur modifié"),
    USER_LOCKED("Utilisateur bloqué"),
    USER_UNLOCKED("Utilisateur débloqué"),

    // ============ Gestion des membres ============
    MEMBRE_CREATED("Membre créé"),
    MEMBRE_UPDATED("Membre modifié"),
    MEMBRE_CLOSED("Membre clôturé (logiquement)"),

    // ============ Gestion des comptes épargne ============
    COMPTE_EPARGNE_CREATED("Compte épargne créé"),
    OPERATION_EPARGNE_CREATED("Opération épargne enregistrée"),

    // ============ Gestion de la caisse ============
    SESSION_CAISSE_OPENED("Session caisse ouverte"),
    SESSION_CAISSE_CLOSED("Session caisse clôturée"),
    OPERATION_CAISSE_CREATED("Opération caisse enregistrée"),

    // ============ Gestion des crédits ============
    DEMANDE_CREDIT_CREATED("Demande crédit créée"),
    ANALYSE_RISQUE_CREATED("Analyse de risque créée"),
    PAIEMENT_INITIAL_DEMANDE_CREATED("Paiement initial de demande crédit enregistré"),
    CREDIT_APPROVED("Crédit approuvé (DÉCISION, non décaissement)"),
    CREDIT_DISBURSED("Crédit décaissé (EXÉCUTION)"),
    REMBOURSEMENT_CREATED("Remboursement enregistré"),

    // ============ Sécurité et audit ============
    ACCESS_DENIED("Accès refusé (autorisation)"),
    DATA_EXPORT("Données exportées"),
    PERMISSION_DENIED("Permission insuffisante"),
    INVALID_OPERATION("Opération métier invalide");

    private final String description;

    AuditAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
