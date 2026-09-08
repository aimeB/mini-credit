import { ModePaiement } from "../../../shared/enums/mode-paiement.enum";

export interface RemboursementRequest {
  echeanceId?: number | null;
  membreId: number;
  datePaiement: string;
  montantTotal: number;
  modePaiement: ModePaiement;
  sessionCaisseId?: number | null;
  agentId?: number | null;
  createdBy?: number | null;
  observation?: string | null;
}