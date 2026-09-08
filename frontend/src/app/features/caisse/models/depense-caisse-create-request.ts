import { DepenseCaisseCategorie } from './depense-caisse-categorie';
import { TypePaiementPersonnel } from './type-paiement-personnel';

export interface DepenseCaisseCreateRequest {
  caisseId: number;
  categorie: DepenseCaisseCategorie;
  montant?: number;
  devise?: string;
  motif: string;
  beneficiaire?: string;
  beneficiaireId?: number;
  beneficiaireNom?: string;
  beneficiaireRole?: string;
  beneficiaireAgence?: string;
  employeId?: number;
  periodePaie?: string;
  typePaiementPersonnel?: TypePaiementPersonnel;
  montantRemunerationReference?: number;
  montantEcartRemuneration?: number;
  motifEcartRemuneration?: string;
  naturePaiementPaie?: string;
  montantSalaireDu?: number;
  montantDejaPaye?: number;
  montantRestantApresPaiement?: number;
  montantRetenue?: number;
  motifRetenue?: string;
  motifPaiementPartiel?: string;
  commentairePaie?: string;
  retenueDefinitive?: boolean;
  primeMotivationManuelle?: number;
  motifPrimeMotivationManuelle?: string;
  justificatifUrl?: string;
}