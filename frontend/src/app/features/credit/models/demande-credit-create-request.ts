import { DureeUnite } from "../../../shared/enums/duree-unite.enum";
import { PeriodiciteRemboursement } from "../../../shared/enums/periodicite-remboursement.enum";

export interface DemandeCreditCreateRequest {
  membreId: number;
  siteId: number;
  agentId?: number | null;

  montantDemande: number;
  devise: string;

  dureeValeur: number;
  dureeUnite: DureeUnite;
  periodiciteRemboursement: PeriodiciteRemboursement;

  tauxInteret: number;

  objetCredit: string;
  gagePropose?: string | null;
  activiteFinancee?: string | null;

  revenusEstimes: number;
  chargesEstimees: number;

  fraisDemande?: number | null;
}