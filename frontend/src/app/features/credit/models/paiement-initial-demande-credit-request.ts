import { ModePaiement } from "../../../shared/enums/mode-paiement.enum";

export interface PaiementInitialDemandeCreditRequest {
  sessionCaisseId?: number;
  caisseId?: number;

  agentId?: number;
  createdById?: number;

  datePaiement?: string;

  modePaiement: ModePaiement;

  fraisPayes: number;
  depotGarantiePaye?: number;

  observation?: string;
}