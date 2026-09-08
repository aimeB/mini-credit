import { TypePaiementPersonnel } from './type-paiement-personnel';

export interface DepenseCaisseRattachementPaieRequest {
  employeId: number;
  periodePaie: string;
  typePaiementPersonnel: TypePaiementPersonnel;
  motif?: string;
  motifRetenue?: string;
  motifPaiementPartiel?: string;
  retenueDefinitive?: boolean;
  commentaireCorrection: string;
}
