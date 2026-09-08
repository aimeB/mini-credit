import { StatutCredit } from './enums/statut-credit.enum';

export interface CreditEnCoursResponse {
  id: number;
  numeroCredit: string;
  membreNomComplet?: string | null;
  montantAccorde: number;
  totalPaye: number;
  resteAPayer: number;
  dateDecaissement?: string | null;
  dureeValeur?: number | null;
  dureeUnite?: string | null;
  statut: StatutCredit;
  antenneNom?: string | null;
  agentTerrainNom?: string | null;
  gestionnaireNom?: string | null;
  controleurNom?: string | null;
  chefBureauNom?: string | null;
  caissierNom?: string | null;
  devise?: string | null;
}
