import { TypeAnomalieSessionCaisse } from './session-caisse-anomalie-response';

export interface DemanderAnnulationSessionRequest {
  typeAnomalie?: TypeAnomalieSessionCaisse;
  motif: string;
  commentaire?: string;
}
