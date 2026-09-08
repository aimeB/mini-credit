// Codes de roles alignes sur les RoleCode backend (RoleCode.java)
export const ROLES = {
  ADMIN:         'ADMIN',
  GERANT_GENERAL:'GERANT_GENERAL',
  COO:           'COO',
  CHEF_BUREAU:   'CHEF_BUREAU',
  GESTIONNAIRE:  'GESTIONNAIRE',
  AGENT_TERRAIN: 'AGENT_TERRAIN',
  CONTROLEUR:    'CONTROLEUR',
  CAISSIER:      'CAISSIER',
  RCI:           'RCI',
  MEMBER:        'MEMBER'
} as const;

/** Libelle court affiche dans la navbar et les listes */
export const ROLE_LABELS: Record<string, string> = {
  [ROLES.ADMIN]:         'Administrateur',
  [ROLES.GERANT_GENERAL]:'Gérant Général',
  [ROLES.COO]:           'COO',
  [ROLES.CHEF_BUREAU]:   'Chef de Bureau',
  [ROLES.GESTIONNAIRE]:  'Gestionnaire',
  [ROLES.AGENT_TERRAIN]: 'Agent Terrain',
  [ROLES.CONTROLEUR]:    'Contrôleur',
  [ROLES.CAISSIER]:      'Caissier',
  [ROLES.RCI]:           'Responsable Contrôle Interne (RCI)',
  [ROLES.MEMBER]:        'Membre'
};

/** Description courte du role */
export const ROLE_DESCRIPTIONS: Record<string, string> = {
  [ROLES.ADMIN]:         'Accès complet au système - tous les modules et toutes les actions',
  [ROLES.GERANT_GENERAL]:'Arbitrage stratégique - supervision globale - suivi des alertes critiques et décisions exceptionnelles',
  [ROLES.COO]:           'Supervision transverse des opérations et du pilotage métier',
  [ROLES.CHEF_BUREAU]:   'Chef de Bureau - supervision antenne - approbation crédits - supervision personnel - rapports',
  [ROLES.GESTIONNAIRE]:  'Supervision agents terrain - suivi sites - pré-analyse crédit - saisie dossiers - observations',
  [ROLES.AGENT_TERRAIN]: 'Recrutement membres - collecte épargne - collecte remboursements - demandes crédit terrain',
  [ROLES.CONTROLEUR]:    'Validation recettes journalières - validation retraits - contrôle caisse - contrôle crédits et garanties',
  [ROLES.CAISSIER]:      'Ouverture/tenue caisse - encaissements - décaissements - paiements',
  [ROLES.RCI]:           'Audit - enquête - contrôle interne - rôle distinct du Chef de Bureau et du Contrôleur',
  [ROLES.MEMBER]:        'Accès membre - consultation profil, crédits et comptes épargne'
};

/** Modules/actions principales accessibles par role */
export const ROLE_MODULES: Record<string, string[]> = {
  [ROLES.ADMIN]:         ['Tous les modules du système'],
  [ROLES.GERANT_GENERAL]:['Supervision stratégique', 'Audit & Logs', 'Rapports & Analytics', 'Arbitrage des cas exceptionnels'],
  [ROLES.COO]:           ['Supervision opérationnelle', 'Audit & Logs', 'Rapports & Analytics'],
  [ROLES.CHEF_BUREAU]:   ['Supervision antenne', 'Approbation crédits', 'Personnel', 'Rapports & Analytics'],
  [ROLES.GESTIONNAIRE]:  ['Agents terrain', 'Sites', 'Supervision terrain', 'Pré-analyse crédit'],
  [ROLES.AGENT_TERRAIN]: ['Membres terrain', 'Recettes journalières', 'Demandes crédit terrain'],
  [ROLES.CONTROLEUR]:    ['Recettes', 'Retraits épargne', 'Caisse', 'Écarts', 'Garanties', 'Contrôle crédit'],
  [ROLES.CAISSIER]:      ['Caisse', 'Sessions caisse', 'Encaissements', 'Décaissements', 'Paiements'],
  [ROLES.RCI]:           ['Audit & Logs', 'Enquêtes internes', 'Contrôle interne', 'Rapports écarts'],
  [ROLES.MEMBER]:        ['Mon Profil', 'Mes crédits', 'Mon épargne']
};

/** Roles necessitant un employe lie (obligatoire en creation) */
export const OPERATIONAL_ROLES = new Set([
  ROLES.AGENT_TERRAIN,
  ROLES.GESTIONNAIRE,
  ROLES.CONTROLEUR,
  ROLES.CAISSIER,
  ROLES.CHEF_BUREAU
]);

/** Roles de supervision globale : pas d'employe/site obligatoire. */
export const GLOBAL_SUPERVISION_ROLES = new Set([
  ROLES.GERANT_GENERAL,
  ROLES.COO,
  ROLES.RCI
]);
