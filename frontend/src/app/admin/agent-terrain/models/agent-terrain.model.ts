/**
 * AgentTerrain Models for Admin Management
 */

export interface CreateAgentTerrainRequest {
  utilisateurId: number;   // Link to existing user
  matricule: string;       // Unique code
  siteId: number;          // Primary site assignment
  gestionnaireId: number;  // Employe avec fonction=GESTIONNAIRE (obligatoire)
  dateAffectation?: string;
  siteIds?: number[];      // Sites additionnels (Phase 4)
}

export interface UpdateAgentTerrainRequest {
  matricule?: string;
  siteId?: number;
  gestionnaireId?: number; // Changer de gestionnaire (optionnel)
  siteIds?: number[];
  actif?: boolean;
}

export interface AgentTerrainResponse {
  id: number;
  utilisateurId: number;
  username: string;
  nomCompletUtilisateur: string;
  /** Nom depuis la fiche Employé (plus fiable que nomCompletUtilisateur) */
  employeNomComplet?: string;
  /** ID de la fiche Employé liée (utile pour PATCH /agence) */
  employeId?: number;
  /** Téléphone depuis la fiche Employé */
  employeTelephone?: string;
  /** Agence de l'employé agent terrain */
  agentAgenceId?: number;
  agentAgenceNom?: string;
  matricule: string;
  siteId: number;
  sitePrincipalNom?: string;
  /** Agence du site principal */
  siteAgenceId?: number;
  siteAgenceNom?: string;
  gestionnaireId?: number;
  gestionnaireNomComplet?: string;
  /** Téléphone du gestionnaire */
  gestionnaireTelephone?: string;
  /** Agence du gestionnaire */
  gestionnaireAgenceId?: number;
  gestionnaireAgenceNom?: string;
  dateAffectation: string;
  actif: boolean;
  siteIds?: number[];
  dateCreation?: string;
  dateModification?: string;
}

export interface UtilisateurSimple {
  id: number;
  username: string;
  nomComplet: string;
  email: string;
  actif: boolean;
}

export interface SiteSimple {
  id: number;
  nomSite: string;
  ville: string;
  commune: string;
}

export interface GestionnaireSimple {
  id: number;
  nomComplet: string;
  matricule: string;
  actif: boolean;
}

export interface PortefeuilleGestionnaireResponse {
  gestionnaireId: number;
  gestionnaireNomComplet: string;
  agents: AgentTerrainResponse[];
  sites: SiteSimple[];
  totalAgents: number;
  totalSites: number;
  totalMembres: number;
}