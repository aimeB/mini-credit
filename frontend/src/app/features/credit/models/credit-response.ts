import { DureeUnite } from "../../../shared/enums/duree-unite.enum";
import { PeriodiciteRemboursement } from "../../../shared/enums/periodicite-remboursement.enum";
import { StatutCredit } from "./enums/statut-credit.enum";

export interface CreditResponse {
  id: number;
  numeroCredit: string;
  demandeCreditId: number;
  membreId: number;
  membreNomComplet: string;
  agentTerrainId?: number | null;
  agentTerrainNom?: string | null;
  siteId: number;
  siteNom: string;

  dateApprobation: string;
  dateDecaissement?: string;

  montantOctroye: number;
  devise: string;
  tauxInteret: number;

  dureeValeur: number;
  dureeUnite: DureeUnite;
  periodiciteRemboursement: PeriodiciteRemboursement;
  nombreEcheances: number;

  principalTotal: number;
  interetTotal: number;
  penaliteTotal: number;
  totalARembourser: number;
  encoursPrincipal: number;

  statut: StatutCredit;
  motifContentieux?: string;

  createdAt: string;
  updatedAt: string;
}