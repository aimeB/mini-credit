import { CreditEcheanceDetail } from './credit-detail-response';
import { StatutCredit } from './enums/statut-credit.enum';

export interface CreditContratResponse {
  creditId: number;
  numeroCredit: string;
  demandeCreditId?: number | null;
  numeroDemande?: string | null;
  statut: StatutCredit;
  membreNomComplet?: string | null;
  membreAdresse?: string | null;
  membreTelephone?: string | null;
  montantAccorde: number;
  devise?: string | null;
  tauxInteret?: number | null;
  dureeValeur?: number | null;
  dureeUnite?: string | null;
  datePret?: string | null;
  dateDecaissement?: string | null;
  objetCredit?: string | null;
  gagePropose?: string | null;
  garantieRegleMontant?: number | null;
  garantieRegleLibelle?: string | null;
  montantGarantieRequis?: number | null;
  montantGarantieBloque?: number | null;
  sourceGarantie?: string | null;
  garantiesMateriellesAcceptees?: string | null;
  penaliteRetardLibelle?: string | null;
  totalPrincipal?: number | null;
  totalInteret?: number | null;
  totalAPayer?: number | null;
  antenneNom?: string | null;
  agentTerrainNom?: string | null;
  gestionnaireNom?: string | null;
  controleurNom?: string | null;
  chefBureauNom?: string | null;
  caissierNom?: string | null;
  echeancier: CreditEcheanceDetail[];
}
