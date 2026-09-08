export interface SessionCaisseOpenRequest {
  caisseId: number;
  dateComptable: string;
  dateOuverture: string;
  soldeOuverture?: number;
  observation?: string;
}