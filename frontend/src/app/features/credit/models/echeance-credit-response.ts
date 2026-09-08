import { StatutEcheance } from './enums/statut-echeance.enum';

export interface EcheanceCreditResponse {
  id: number;
  numeroEcheance: number;
  dateEcheance: string;

  principalPrevu: number;
  interetPrevu: number;
  penaliteCumulee: number;
  totalPrevu: number;

  principalPaye: number;
  interetPaye: number;
  penalitePayee: number;
  totalPaye: number;

  resteAPayer: number;

  principalRestant: number;
  interetRestant: number;
  penaliteRestante: number;

  dateDernierPaiement?: string | null;
  statut: StatutEcheance;
}