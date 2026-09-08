/**
 * Énumération des permissions granulaires côté client (Angular).
 * Utilisée pour les vérifications de permissions dans les templates et les services.
 *
 * IMPORTANT : Cette énumération est SYNCHRONE avec PermissionCode.java du backend.
 * Toute modification doit être répercutée des deux côtés.
 *
 * Les permissions fonctionnent en complément des rôles pour un contrôle granulaire.
 * Exemple : CREDIT_APPROVE ≠ CREDIT_DISBURSE (séparation des tâches)
 */
export enum PermissionCode {
  // Utilisateurs et sécurité
  USER_READ = 'USER_READ',
  USER_CREATE = 'USER_CREATE',
  USER_UPDATE = 'USER_UPDATE',
  USER_ASSIGN_ROLE = 'USER_ASSIGN_ROLE',
  USER_LOCK = 'USER_LOCK',
  USER_UNLOCK = 'USER_UNLOCK',

  // Membres
  MEMBRE_READ = 'MEMBRE_READ',
  MEMBRE_CREATE = 'MEMBRE_CREATE',
  MEMBRE_UPDATE = 'MEMBRE_UPDATE',
  MEMBRE_CLOSE = 'MEMBRE_CLOSE',
  MEMBRE_READ_SELF = 'MEMBRE_READ_SELF',
  MEMBRE_UPDATE_SELF = 'MEMBRE_UPDATE_SELF',

  // Épargne
  EPARGNE_COMPTE_CREATE = 'EPARGNE_COMPTE_CREATE',
  EPARGNE_COMPTE_READ = 'EPARGNE_COMPTE_READ',
  EPARGNE_OPERATION_CREATE = 'EPARGNE_OPERATION_CREATE',
  EPARGNE_OPERATION_READ = 'EPARGNE_OPERATION_READ',
  TICKET_RECU_READ = 'TICKET_RECU_READ',
  TICKET_RECU_PRINT = 'TICKET_RECU_PRINT',
  TICKET_RECU_DUPLICATA = 'TICKET_RECU_DUPLICATA',
  TICKET_RECU_VERIFY = 'TICKET_RECU_VERIFY',
  TICKET_RECU_ADMIN = 'TICKET_RECU_ADMIN',

  // Caisse
  CAISSE_CREATE = 'CAISSE_CREATE',
  CAISSE_READ = 'CAISSE_READ',
  SESSION_CAISSE_OPEN = 'SESSION_CAISSE_OPEN',
  SESSION_CAISSE_CLOSE = 'SESSION_CAISSE_CLOSE',
  SESSION_CAISSE_FINAL_CLOSE = 'SESSION_CAISSE_FINAL_CLOSE',
  SESSION_CAISSE_ANOMALIE_READ = 'SESSION_CAISSE_ANOMALIE_READ',
  SESSION_CAISSE_ANOMALIE_REQUEST = 'SESSION_CAISSE_ANOMALIE_REQUEST',
  SESSION_CAISSE_ANOMALIE_VALIDATE = 'SESSION_CAISSE_ANOMALIE_VALIDATE',
  SESSION_CAISSE_ADMIN_CANCEL = 'SESSION_CAISSE_ADMIN_CANCEL',
  SESSION_CAISSE_REOPEN_CONTROLLED = 'SESSION_CAISSE_REOPEN_CONTROLLED',
  OPERATION_CAISSE_CREATE = 'OPERATION_CAISSE_CREATE',
  OPERATION_CAISSE_READ = 'OPERATION_CAISSE_READ',
  DASHBOARD_CAISSE_READ = 'DASHBOARD_CAISSE_READ',
  RAPPORT_CAISSE_READ = 'RAPPORT_CAISSE_READ',
  RAPPORT_CAISSE_EXPORT = 'RAPPORT_CAISSE_EXPORT',
  RAPPORT_CAISSE_AUDIT_READ = 'RAPPORT_CAISSE_AUDIT_READ',
  TASK_READ_OWN = 'TASK_READ_OWN',
  TASK_READ_ANTENNE = 'TASK_READ_ANTENNE',
  TASK_COMPLETE = 'TASK_COMPLETE',
  TASK_SUPERVISE = 'TASK_SUPERVISE',
  TASK_AUDIT = 'TASK_AUDIT',

  // Crédit (séparation critique)
  DEMANDE_CREDIT_CREATE = 'DEMANDE_CREDIT_CREATE',
  DEMANDE_CREDIT_READ = 'DEMANDE_CREDIT_READ',
  ANALYSE_RISQUE_CREATE = 'ANALYSE_RISQUE_CREATE',
  ANALYSE_RISQUE_READ = 'ANALYSE_RISQUE_READ',
  CREDIT_APPROVE = 'CREDIT_APPROVE',         // Approbation (décision)
  CREDIT_DISBURSE = 'CREDIT_DISBURSE',       // Décaissement (exécution)
  CREDIT_READ = 'CREDIT_READ',
  REMBOURSEMENT_CREATE = 'REMBOURSEMENT_CREATE',
  REMBOURSEMENT_READ = 'REMBOURSEMENT_READ',

  // Dashboard et audit
  DASHBOARD_GLOBAL_READ = 'DASHBOARD_GLOBAL_READ',
  DASHBOARD_CONTROLE_INTERNE_READ = 'DASHBOARD_CONTROLE_INTERNE_READ',
  AUDIT_READ = 'AUDIT_READ',
  AUDIT_LOG_READ = 'AUDIT_LOG_READ',
  AUDIT_LOG_EXPORT = 'AUDIT_LOG_EXPORT',
  AUDIT_SECURITY_READ = 'AUDIT_SECURITY_READ'
}

export const PERMISSION_LABELS = {
  [PermissionCode.USER_READ]: 'Voir les utilisateurs',
  [PermissionCode.USER_CREATE]: 'Créer un utilisateur',
  [PermissionCode.USER_UPDATE]: 'Modifier un utilisateur',
  [PermissionCode.USER_ASSIGN_ROLE]: 'Assigner des rôles',
  [PermissionCode.USER_LOCK]: 'Bloquer un utilisateur',
  [PermissionCode.USER_UNLOCK]: 'Débloquer un utilisateur',

  [PermissionCode.MEMBRE_READ]: 'Voir les membres',
  [PermissionCode.MEMBRE_CREATE]: 'Créer un membre',
  [PermissionCode.MEMBRE_UPDATE]: 'Modifier un membre',
  [PermissionCode.MEMBRE_CLOSE]: 'Clôturer un membre',
  [PermissionCode.MEMBRE_READ_SELF]: 'Voir son profil',
  [PermissionCode.MEMBRE_UPDATE_SELF]: 'Modifier son profil',

  [PermissionCode.EPARGNE_COMPTE_CREATE]: 'Créer un compte épargne',
  [PermissionCode.EPARGNE_COMPTE_READ]: 'Voir les comptes épargne',
  [PermissionCode.EPARGNE_OPERATION_CREATE]: 'Créer une opération épargne',
  [PermissionCode.EPARGNE_OPERATION_READ]: 'Voir les opérations épargne',
  [PermissionCode.TICKET_RECU_READ]: 'Voir les tickets reçus',
  [PermissionCode.TICKET_RECU_PRINT]: 'Imprimer les tickets reçus',
  [PermissionCode.TICKET_RECU_DUPLICATA]: 'Générer un duplicata de ticket reçu',
  [PermissionCode.TICKET_RECU_VERIFY]: 'Vérifier un ticket reçu',
  [PermissionCode.TICKET_RECU_ADMIN]: 'Administrer les tickets reçus',

  [PermissionCode.CAISSE_CREATE]: 'Créer une caisse',
  [PermissionCode.CAISSE_READ]: 'Voir les caisses',
  [PermissionCode.SESSION_CAISSE_OPEN]: 'Ouvrir une session caisse',
  [PermissionCode.SESSION_CAISSE_CLOSE]: 'Clôturer une session caisse',
  [PermissionCode.SESSION_CAISSE_FINAL_CLOSE]: 'Clôturer définitivement une session caisse',
  [PermissionCode.SESSION_CAISSE_ANOMALIE_READ]: 'Consulter les anomalies de session caisse',
  [PermissionCode.SESSION_CAISSE_ANOMALIE_REQUEST]: 'Demander une annulation de session caisse',
  [PermissionCode.SESSION_CAISSE_ANOMALIE_VALIDATE]: 'Valider une annulation de session caisse',
  [PermissionCode.SESSION_CAISSE_ADMIN_CANCEL]: 'Annuler administrativement une session caisse',
  [PermissionCode.SESSION_CAISSE_REOPEN_CONTROLLED]: 'Réouvrir de manière contrôlée une session pré-clôturée',
  [PermissionCode.OPERATION_CAISSE_CREATE]: 'Enregistrer une opération caisse',
  [PermissionCode.OPERATION_CAISSE_READ]: 'Voir les opérations caisse',
  [PermissionCode.DASHBOARD_CAISSE_READ]: 'Voir le dashboard caisse',
  [PermissionCode.RAPPORT_CAISSE_READ]: 'Voir les rapports caisse',
  [PermissionCode.RAPPORT_CAISSE_EXPORT]: 'Exporter les rapports caisse',
  [PermissionCode.RAPPORT_CAISSE_AUDIT_READ]: 'Consulter les rapports caisse (audit)',
  [PermissionCode.TASK_READ_OWN]: 'Consulter ses actions workflow',
  [PermissionCode.TASK_READ_ANTENNE]: 'Consulter les actions workflow de l\'antenne',
  [PermissionCode.TASK_COMPLETE]: 'Terminer une action workflow',
  [PermissionCode.TASK_SUPERVISE]: 'Superviser les actions workflow',
  [PermissionCode.TASK_AUDIT]: 'Auditer les actions workflow',

  [PermissionCode.DEMANDE_CREDIT_CREATE]: 'Créer une demande crédit',
  [PermissionCode.DEMANDE_CREDIT_READ]: 'Voir une demande crédit',
  [PermissionCode.ANALYSE_RISQUE_CREATE]: 'Créer une analyse de risque',
  [PermissionCode.ANALYSE_RISQUE_READ]: 'Voir une analyse de risque',
  [PermissionCode.CREDIT_APPROVE]: 'Approuver un crédit',
  [PermissionCode.CREDIT_DISBURSE]: 'Décaisser un crédit',
  [PermissionCode.CREDIT_READ]: 'Voir un crédit',
  [PermissionCode.REMBOURSEMENT_CREATE]: 'Enregistrer un remboursement',
  [PermissionCode.REMBOURSEMENT_READ]: 'Voir les remboursements',

  [PermissionCode.DASHBOARD_GLOBAL_READ]: 'Voir le dashboard global',
  [PermissionCode.DASHBOARD_CONTROLE_INTERNE_READ]: 'Voir le dashboard contrôle interne',
  [PermissionCode.AUDIT_READ]: 'Consulter l\'audit',
  [PermissionCode.AUDIT_LOG_READ]: 'Consulter les logs d\'audit',
  [PermissionCode.AUDIT_LOG_EXPORT]: 'Exporter les logs d\'audit',
  [PermissionCode.AUDIT_SECURITY_READ]: 'Consulter les événements de sécurité'
};
