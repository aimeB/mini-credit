import { StatutCredit } from './enums/statut-credit.enum';

export interface CreditRembourseResponse {
  id: number;
  numeroCredit: string;
  membreNomComplet?: string | null;
  montantAccorde: number;
  totalInteret: number;
  totalPaye: number;
  resteAPayer: number;
  dateDecaissement?: string | null;
  dateDernierPaiement?: string | null;
  dateCloture?: string | null;
  statut: StatutCredit;
  antenneNom?: string | null;
  devise?: string | null;
}