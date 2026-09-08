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
    USER_PASSWORD_RESET("Réinitialiser le mot de passe d'un utilisateur"),
    USER_PASSWORD_CHANGE("Changer son mot de passe"),

    // ============ Gestion des membres ============
    MEMBRE_READ("Voir les membres"),
    MEMBRE_CREATE("Créer un membre"),
    MEMBRE_UPDATE("Modifier un membre"),
    MEMBRE_CLOSE("Clôturer un membre"),
    MEMBRE_READ_SELF("Voir son propre profil"),
    MEMBRE_UPDATE_SELF("Modifier son profil"),

    // ============ Consultation terrain gestionnaire ============
    AGENT_TERRAIN_READ("Voir les agents terrain"),
    SITE_READ("Voir les sites"),
    TERRAIN_ACTIVITY_READ("Voir les activités terrain"),
    PERFORMANCE_TERRAIN_READ("Voir les performances terrain"),
    CREDIT_PRE_ANALYSE("Effectuer la pré-analyse crédit"),
    RECLAMATION_READ("Voir les réclamations"),
    RECLAMATION_COMMENT("Commenter les réclamations"),

    // ============ Gestion des comptes épargne ============
    EPARGNE_COMPTE_CREATE("Créer un compte épargne"),
    EPARGNE_COMPTE_READ("Voir les comptes épargne"),
    EPARGNE_OPERATION_CREATE("Créer une opération épargne"),
    EPARGNE_OPERATION_READ("Voir les opérations épargne"),
    TICKET_RECU_READ("Consulter les tickets reçus"),
    TICKET_RECU_PRINT("Imprimer les tickets reçus"),
    TICKET_RECU_DUPLICATA("Générer un duplicata de ticket reçu"),
    TICKET_RECU_VERIFY("Vérifier l'authenticité d'un ticket reçu"),
    TICKET_RECU_ADMIN("Administrer les tickets reçus"),

    // ============ Gestion de la caisse (séparation critique) ============
    CAISSE_CREATE("Créer une caisse"),
    CAISSE_READ("Voir les caisses"),
    SESSION_CAISSE_OPEN("Ouvrir une session caisse"),
    SESSION_CAISSE_OPEN_OVERRIDE("Forcer une ouverture de session caisse avec solde manuel"),
    SESSION_CAISSE_PRE_CLOSE("Pré-clôturer une session caisse"),
    SESSION_CAISSE_CLOSE("Clôturer une session caisse"),
    SESSION_CAISSE_CONTROL_VALIDATE("Valider le contrôle d'une session caisse"),
    SESSION_CAISSE_FINAL_CLOSE("Clôturer définitivement une session caisse"),
    SESSION_CAISSE_ANOMALIE_READ("Consulter les anomalies de session caisse"),
    SESSION_CAISSE_ANOMALIE_REQUEST("Demander une annulation de session caisse"),
    SESSION_CAISSE_ANOMALIE_VALIDATE("Valider une annulation de session caisse"),
    SESSION_CAISSE_ADMIN_CANCEL("Annuler administrativement une session caisse"),
    SESSION_CAISSE_REOPEN_CONTROLLED("Réouvrir de manière contrôlée une session pré-clôturée"),
    OPERATION_CAISSE_CREATE("Enregistrer une opération caisse"),
    OPERATION_CAISSE_READ("Voir les opérations caisse"),
    JOURNAL_CAISSE_READ("Consulter le journal de caisse"),
    DEPENSE_CAISSE_CREATE("Créer une dépense caisse"),
    DEPENSE_CAISSE_SUBMIT("Soumettre une dépense caisse"),
    DEPENSE_CAISSE_VALIDATE("Valider une dépense caisse"),
    DEPENSE_CAISSE_REJECT("Rejeter une dépense caisse"),
    DEPENSE_CAISSE_PAY("Payer une dépense caisse"),
    DEPENSE_CAISSE_READ("Voir les dépenses caisse"),
    DEPENSE_CAISSE_CANCEL("Annuler une dépense caisse"),
    DASHBOARD_CAISSE_READ("Voir le dashboard caisse"),
    RAPPORT_CAISSE_READ("Consulter les rapports caisse"),
    RAPPORT_CAISSE_EXPORT("Exporter les rapports caisse"),
    RAPPORT_CAISSE_AUDIT_READ("Consulter les éléments d'audit des rapports caisse"),

    // ============ Gestion des crédits (séparation critique) ============
    DEMANDE_CREDIT_CREATE("Créer une demande crédit"),
    DEMANDE_CREDIT_READ("Voir une demande crédit"),
    ANALYSE_RISQUE_CREATE("Créer une analyse de risque"),
    ANALYSE_RISQUE_READ("Voir une analyse de risque"),
    CREDIT_APPROVE("Approuver un crédit (décision)"),        // SÉPARÉ du décaissement
    CREDIT_DISBURSE("Décaisser un crédit (trésorier)"),       // SÉPARÉ de l'approbation
    CREDIT_READ("Voir un crédit"),
    GARANTIE_CONTROL("Contrôler les garanties crédit"),
    GARANTIE_READ("Lire la garantie crédit"),
    GARANTIE_VALIDATE("Valider la garantie crédit"),
    GARANTIE_BLOQUER_EPARGNE("Bloquer l'épargne de garantie"),
    GARANTIE_MATERIELLE_CREATE("Créer une garantie matérielle"),
    REMBOURSEMENT_CREATE("Enregistrer un remboursement"),
    REMBOURSEMENT_READ("Voir les remboursements"),

    // ============ Dashboard et audit ============
    DASHBOARD_GLOBAL_READ("Voir le dashboard global"),
    DASHBOARD_CONTROLE_INTERNE_READ("Voir le dashboard controle interne"),
    AUDIT_READ("Consulter l'audit"),
    AUDIT_LOG_READ("Consulter les logs d'audit"),
    AUDIT_LOG_EXPORT("Exporter les logs d'audit"),
    AUDIT_SECURITY_READ("Consulter les événements de sécurité"),

    // ============ WorkflowTask / Actions utilisateur ============
    TASK_READ_OWN("Lire ses tâches utilisateur"),
    TASK_READ_ANTENNE("Lire les tâches de son antenne"),
    TASK_COMPLETE("Terminer une tâche utilisateur"),
    TASK_SUPERVISE("Superviser les tâches d'une antenne"),
    TASK_AUDIT("Auditer les tâches workflow"),

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
    CONTROLEUR_AUDIT_READ("Consulter audit logs antenne"),

    // ============ PHASE 6B.1: Fiches Journalières Agent Terrain ============
    FICHE_JOURNALIERE_CREATE("Créer une fiche journalière agent"),
    FICHE_JOURNALIERE_READ("Voir les fiches journalières agent"),
    FICHE_JOURNALIERE_EDIT("Modifier une fiche journalière agent (BROUILLON)"),
    FICHE_JOURNALIERE_DELETE("Supprimer une fiche journalière agent (BROUILLON)");

    private final String description;

    PermissionCode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
