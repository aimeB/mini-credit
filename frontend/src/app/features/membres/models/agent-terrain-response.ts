export interface AgentTerrainResponse {
  id: number;
  matricule: string;
  utilisateurId?: number;
  siteId?: number;

  nomCompletUtilisateur?: string;
  nomComplet?: string;
  username?: string;

  nomAffichage?: string;
}