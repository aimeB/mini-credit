export interface SessionCaisseOpeningContextResponse {
  caisseId: number;
  dateComptable: string;
  devise: string;
  premiereSession: boolean;
  soldeOuvertureAutomatique: number;
  forcageAutorise: boolean;
  soldeVerrouille: boolean;
  sessionExistante?: boolean;
  sessionExistanteId?: number;
  sessionExistanteStatut?: string;
  sessionExistanteDateOuverture?: string;
  sessionExistanteDateCloture?: string;
  sessionExistanteUtilisateurId?: number;
  sessionExistanteUtilisateurNom?: string;
}
