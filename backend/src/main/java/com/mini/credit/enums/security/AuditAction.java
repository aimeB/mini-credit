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
    USER_PASSWORD_RESET("Mot de passe utilisateur réinitialisé"),
    USER_PASSWORD_CHANGED("Mot de passe utilisateur changé"),

    EMPLOYE_PHOTO_UPDATED("Photo employé ajoutée, remplacée ou supprimée"),

    // ============ Gestion des membres ============
    MEMBRE_CREATED("Membre créé"),
    MEMBRE_UPDATED("Membre modifié"),
    MEMBRE_CLOSED("Membre clôturé (logiquement)"),

    // ============ Gestion des comptes épargne ============
    COMPTE_EPARGNE_CREATED("Compte épargne créé"),
    OPERATION_EPARGNE_CREATED("Opération épargne enregistrée"),

    // ============ Tickets / reçus épargne ============
    TICKET_RECU_GENERATED("Ticket reçu généré"),
    TICKET_RECU_PRINTED("Ticket reçu imprimé"),
    TICKET_RECU_PRINT_FAILED("Échec impression ticket reçu"),
    TICKET_RECU_DUPLICATED("Duplicata ticket reçu généré"),
    TICKET_RECU_VERIFIED("Ticket reçu vérifié"),

    // ============ Gestion de la caisse ============
    CAISSE_CREATED("Caisse créée"),
    SESSION_CAISSE_OPENED("Session caisse ouverte"),
    SESSION_CAISSE_CLOSED("Session caisse clôturée"),
    OPERATION_CAISSE_CREATED("Opération caisse enregistrée"),
    DEPENSE_CAISSE_CREATED("Dépense caisse créée"),
    DEPENSE_CAISSE_SUBMITTED("Dépense caisse soumise"),
    DEPENSE_CAISSE_VALIDATED("Dépense caisse validée"),
    DEPENSE_CAISSE_REJECTED("Dépense caisse rejetée"),
    DEPENSE_CAISSE_PAID("Dépense caisse payée"),
    DEPENSE_CAISSE_CANCELLED("Dépense caisse annulée"),
    OUVERTURE_SESSION("Ouverture session caisse"),
    PRE_CLOTURE("Pré-clôture session caisse"),
    VALIDATION_CONTROLE("Validation de contrôle session caisse"),
    CLOTURE_FINALE("Clôture finale session caisse"),
    SESSION_ANOMALIE_DEMANDEE("Anomalie session caisse demandée"),
    SESSION_ANOMALIE_REJETEE("Anomalie session caisse rejetée"),
    SESSION_ANNULEE("Session caisse annulée"),
    SESSION_ANNULEE_ADMINISTRATIVEMENT("Session caisse annulée administrativement"),
    SESSION_REOUVERTE_CONTROLEE("Session caisse réouverte de manière contrôlée"),
    REFUS_TRANSITION("Transition workflow session refusée"),
    MODIFICATION_OPERATION("Modification opération caisse"),
    ANNULATION_OPERATION("Annulation opération caisse"),
    SUPPRESSION_REFUSEE("Suppression refusée"),

    // ============ Gestion des crédits ============
    DEMANDE_CREDIT_CREATED("Demande crédit créée"),
    ANALYSE_RISQUE_CREATED("Analyse de risque créée"),
    PAIEMENT_INITIAL_DEMANDE_CREATED("Paiement initial de demande crédit enregistré"),
    CREDIT_APPROVED("Crédit approuvé (DÉCISION, non décaissement)"),
    CREDIT_DISBURSED("Crédit décaissé (EXÉCUTION)"),
    CREDIT_DISBURSE_REFUSED("Décaissement crédit refusé"),
    CREDIT_VALIDATION_CHECKED("Validation stricte crédit vérifiée (PHASE 4)"),
    GARANTIE_VERIFIED("Garantie crédit vérifiée"),
    GARANTIE_BLOCKED("Garantie épargne bloquée"),
    GARANTIE_MATERIAL_ADDED("Garantie matérielle ajoutée"),
    GARANTIE_MATERIAL_ACCEPTED("Garantie matérielle acceptée"),
    GARANTIE_MATERIAL_REJECTED("Garantie matérielle refusée"),
    GARANTIE_VALIDATED("Garantie crédit validée"),
    GARANTIE_REJECTED("Garantie crédit rejetée"),
    GARANTIE_RELEASED("Garantie crédit libérée"),
    REMBOURSEMENT_CREATED("Remboursement enregistré"),

    // ============ PHASE 5: Retraits Épargne ============
    DEMANDE_RETRAIT_EPARGNE_CREATED("Demande retrait épargne créée"),
    DEMANDE_RETRAIT_EPARGNE_VALIDATED("Demande retrait épargne validée par CONTROLEUR"),
    DEMANDE_RETRAIT_EPARGNE_REJECTED("Demande retrait épargne rejetée"),
    DEMANDE_RETRAIT_EPARGNE_DISBURSED("Demande retrait épargne décaissée par CAISSIER"),
    DEMANDE_RETRAIT_EPARGNE_CANCELLED("Demande retrait épargne annulée"),

    // ============ PHASE 6: Recettes Journalières Terrain ============
    RECETTE_JOURNALIERE_CREATED("Recette journalière créée (encodage terrain)"),
    RECETTE_JOURNALIERE_UPDATED("Recette journalière mise à jour (billetage/ajustement)"),
    RECETTE_JOURNALIERE_VALIDATED("Recette journalière validée par CONTROLEUR (réconciliation)"),
    RECETTE_JOURNALIERE_REJECTED("Recette journalière rejetée"),
    RECETTE_JOURNALIERE_CANCELLED("Recette journalière annulée"),

    // ============ PHASE 7: Écarts Caisse / Investigation ============
    ECART_CAISSE_DETECTE("Écart caisse détecté"),
    ECART_CAISSE_JUSTIFIE("Justification initiale ajoutée par Caissier ou Contrôleur"),
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

    // ============ PHASE 10: Pénalités de Retard Crédit ============
    PENALITE_CREEE("Pénalité retard crédit créée (montant = jours × taux)"),
    PENALITE_ACQUITTEE("Pénalité retard acquittée (payée)"),
    PENALITE_EFFACEE("Pénalité retard effacée (pardon/remise documentée)"),
    PENALITE_BATCH_GENERE("Batch pénalités retard généré quotidien"),
    // ============ PHASE 11: Réconciliation Caisse Automatique ============
    RECONCILIATION_CREEE("Réconciliation caisse créée (écart détecté: montant = solde physique - théorique)"),
    RECONCILIATION_RAPPROCHEE("Réconciliation caisse rapprochée (écart résolu/approuvé)"),
    RECONCILIATION_REJETEE("Réconciliation caisse rejetée (écart confirmé intentionnel)"),
    RECONCILIATION_BATCH_GENERO("Batch réconciliations caisse généré quotidien (02:00)"),

    // ============ PHASE 12: Rapports Financiers Complets ============
    RAPPORT_GENERE("Rapport financier généré (Bilan, Compte résultat, KPIs)"),
    RAPPORT_VALIDE("Rapport financier validé (approuvé par admin)"),
    RAPPORT_ARCHIVE("Rapport financier archivé (historique)"),
    RAPPORT_BATCH_GENERO("Batch rapports financiers quotidiens généré (03:00)"),

    // ============ PHASE 6B.1: Fiches Journalières Agent Terrain ============
    FICHE_JOURNALIERE_CREATED("Fiche journalière agent créée (BROUILLON)"),
    FICHE_JOURNALIERE_UPDATED("Fiche journalière agent modifiée (BROUILLON)"),
    FICHE_JOURNALIERE_DELETED("Fiche journalière agent supprimée (BROUILLON)"),

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
