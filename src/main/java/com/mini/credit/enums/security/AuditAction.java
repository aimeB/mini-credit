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
    CREDIT_VALIDATION_CHECKED("Validation stricte crédit vérifiée (PHASE 4)"),
    REMBOURSEMENT_CREATED("Remboursement enregistré"),

    // ============ PHASE 5: Retraits Épargne ============
    DEMANDE_RETRAIT_EPARGNE_CREATED("Demande retrait épargne créée"),
    DEMANDE_RETRAIT_EPARGNE_VALIDATED("Demande retrait épargne validée par CONTROLEUR"),
    DEMANDE_RETRAIT_EPARGNE_REJECTED("Demande retrait épargne rejetée"),
    DEMANDE_RETRAIT_EPARGNE_DISBURSED("Demande retrait épargne décaissée par CAISSIER"),
    DEMANDE_RETRAIT_EPARGNE_CANCELLED("Demande retrait épargne annulée"),

    // ============ PHASE 6: Recettes Journalières Terrain ============
    RECETTE_JOURNALIERE_CREATED("Recette journalière créée (encodage terrain)"),
    RECETTE_JOURNALIERE_VALIDATED("Recette journalière validée par CONTROLEUR (réconciliation)"),
    RECETTE_JOURNALIERE_REJECTED("Recette journalière rejetée"),
    RECETTE_JOURNALIERE_CANCELLED("Recette journalière annulée"),

    // ============ PHASE 7: Écarts Caisse / Investigation ============
    ECART_CAISSE_DETECTE("Écart caisse détecté"),
    ECART_CAISSE_ENQUETE("Écart caisse enquêté par CONTROLEUR"),
    ECART_CAISSE_RESOLU("Écart caisse résolu (raison documentée)"),
    ECART_CAISSE_ACCEPTE("Écart caisse accepté par R.C.I. (variance normalisée)"),
    ECART_CAISSE_REJETE("Écart caisse rejeté (erreur système)"),

    // ============ PHASE 8: Commissions Agents ============
    COMMISSION_CREATED("Commission agent créée (calcul montant)"),
    COMMISSION_VALIDATED("Commission validée par gestionnaire"),
    COMMISSION_PAID("Commission payée à l'agent"),
    COMMISSION_CANCELLED("Commission annulée"),

    // ============ PHASE 9: Intérêts Épargne ============
    INTERET_EPARGNE_GENERE("Batch intérêts épargne généré (tous comptes actifs)"),
    INTERET_EPARGNE_ACCRUED("Intérêt épargne acquis sur compte (mensuel = solde × taux / 12)"),

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
