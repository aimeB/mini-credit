export interface PenaliteEcheanceResponse {
  echeanceId: number;
  numeroEcheance: number;
  dateEcheance: string;
  joursRetard: number;
  penaliteCumulee: number;
  resteAPayer: number;
}