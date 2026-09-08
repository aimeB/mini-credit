/**
 * Fonctions / postes métier des employés 3N.
 * Liste fermée — correspond à l'enum PosteEmploye côté backend.
 */
export type FonctionEmploye =
  | 'AGENT_TERRAIN'
  | 'GESTIONNAIRE'
  | 'CONTROLEUR'
  | 'CAISSIER'
  | 'CHEF_BUREAU'
  | 'COO'
  | 'RCI'
  | 'GERANT_GENERAL'
  | 'ADMINISTRATIF'
  | 'CHARGE_OPERATIONS'
  | 'RESPONSABLE_CONTROLES'
  | 'AUTRE';

export const FONCTION_LABELS: Record<FonctionEmploye, string> = {
  AGENT_TERRAIN:          'Agent Terrain',
  GESTIONNAIRE:           'Gestionnaire',
  CONTROLEUR:             'Contrôleur',
  CAISSIER:               'Caissier',
  CHEF_BUREAU:            'Chef de Bureau',
  COO:                    'COO / Directeur des Opérations',
  RCI:                    'RCI / Responsable Contrôle Interne',
  GERANT_GENERAL:         'Gérant Général',
  ADMINISTRATIF:          'Administratif',
  CHARGE_OPERATIONS:      'Chargé d’Opérations',
  RESPONSABLE_CONTROLES:  'Responsable Contrôles Internes',
  AUTRE:                  'Autre',
};

export const FONCTIONS_LIST = Object.keys(FONCTION_LABELS) as FonctionEmploye[];
