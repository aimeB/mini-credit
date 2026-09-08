/**
 * Énumération des codes de rôle côté client (Angular).
 * Utilisée pour les vérifications de rôles dans les templates et les services.
 * 
 * IMPORTANT : Cette énumération est SYNCHRONE avec RoleCode.java du backend.
 * Toute modification doit être répercutée des deux côtés.
 */
export enum RoleCode {
  ADMIN = 'ADMIN',
  GERANT_GENERAL = 'GERANT_GENERAL',
  COO = 'COO',
  CHEF_BUREAU = 'CHEF_BUREAU',
  GESTIONNAIRE = 'GESTIONNAIRE',
  RCI = 'RCI',
  CONTROLEUR = 'CONTROLEUR',
  AGENT_TERRAIN = 'AGENT_TERRAIN',
  CAISSIER = 'CAISSIER',
  MEMBER = 'MEMBER'
}

export const ROLE_LABELS = {
  [RoleCode.ADMIN]: 'Administrateur',
  [RoleCode.GERANT_GENERAL]: 'Gérant Général',
  [RoleCode.COO]: 'COO',
  [RoleCode.CHEF_BUREAU]: 'Chef de Bureau',
  [RoleCode.GESTIONNAIRE]: 'Gestionnaire',
  [RoleCode.RCI]: 'RCI',
  [RoleCode.CONTROLEUR]: 'Contrôleur',
  [RoleCode.AGENT_TERRAIN]: 'Agent de terrain',
  [RoleCode.CAISSIER]: 'Caissier',
  [RoleCode.MEMBER]: 'Membre client'
};
