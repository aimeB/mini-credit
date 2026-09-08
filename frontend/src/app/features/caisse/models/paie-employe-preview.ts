export type ModeCalculPaie = 'AGENT_TERRAIN_AUTOMATIQUE' | 'PERSONNEL_BUREAU_MANUEL';

export interface PaieEmployePreview {
  employeId: number;
  matricule?: string;
  nomComplet?: string;
  poste?: string;
  periodePaie: string;
  salaireBase: number;
  epargneCollecteeValidee: number;
  remboursementCreditCollecteValide: number;
  nombreCarnetsVendus: number;
  primeMobilisationEpargne: number;
  primeMobilisationRemboursement: number;
  bonusCarnets: number;
  primeMotivationManuelle: number;
  primeMotivationManuelleAutorisee: boolean;
  totalPrimes: number;
  totalBonus: number;
  totalAPayer: number;
  modeCalcul: ModeCalculPaie;
}