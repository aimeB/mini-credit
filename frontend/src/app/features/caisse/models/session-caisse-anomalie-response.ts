import { StatutSessionCaisse } from './statut-session-caisse';

export type TypeAnomalieSessionCaisse =
  | 'OUVERTURE_ERRONEE_SANS_MOUVEMENT'
  | 'PRE_CLOTURE_ERRONEE'
  | 'CLOTURE_ERRONEE_SANS_MOUVEMENT'
  | 'SESSION_AVEC_MOUVEMENTS_RECTIFICATION_REQUISE';

export type StatutDossierAnomalieSession = 'DEMANDEE' | 'VALIDEE' | 'REJETEE' | 'EXECUTEE';

export interface SessionCaisseAnomalieResponse {
  id: number;
  sessionId: number;
  typeAnomalie: TypeAnomalieSessionCaisse;
  ancienStatut: StatutSessionCaisse;
  nouveauStatut?: StatutSessionCaisse;
  motif: string;
  commentaire?: string;
  statutDossier: StatutDossierAnomalieSession;
  demandePar?: string;
  validePar?: string;
  dateDemande: string;
  dateValidation?: string;
  actionExecutee?: string;
}
