import { RecetteStatut, EpargneSourceType } from '.';

// Request DTOs
export interface CreateRecetteTerrainRequest {
  agentTerrainId?: number;
  siteId: number;
  dateRecette: string;
  membresVisites: number;
  nouveauxMembres: number;
  carnetDistribues: number;
  epargneCollectee: number;
  remboursementsCreditCollectes: number;
  creditIdsTraites: string | null;
  fraisCollectes: number;
  demandesCreditRecueillies: number;
  demandesCreditIds: string | null;
  especesRemises: number;
  especesEmises: number;
  observations?: string;
}

export interface UpdateRecetteTerrainRequest {
  membresVisites: number;
  nouveauxMembres: number;
  carnetDistribues: number;
  epargneCollectee: number;
  remboursementsCreditCollectes: number;
  creditIdsTraites: string | null;
  fraisCollectes: number;
  demandesCreditRecueillies: number;
  demandesCreditIds: string | null;
  especesRemises: number;
  especesEmises: number;
  observations?: string;
}

export interface ValidateRecetteTerrainRequest {
  decision: 'VALIDEE' | 'REJETEE';
  motifRejet?: string;
  validePar: number;
}

// Response DTO
export interface RecetteTerrainResponse {
  id: number;
  agentTerrainId: number;
  agentTerrainNom: string;
  siteId: number;
  siteNom: string;
  dateRecette: string;
  membresVisites: number;
  nouveauxMembres: number;
  carnetDistribues: number;
  epargneCollectee: number;
  epargneSourceType: EpargneSourceType;
  remboursementsCreditCollectes: number;
  creditIdsTraites: string | null;
  fraisCollectes: number;
  demandesCreditRecueillies: number;
  demandesCreditIds: string | null;
  especesRemises: number;
  especesEmises: number;
  excedent: number;
  manquant: number;
  totalCollecte: number;
  observations: string;
  statut: RecetteStatut;
  dateCreation: string;
  dateModification: string;
  modifiePar: string | null;
  dateValidation: string | null;
  valideParNom: string | null;
  motifRejet: string | null;
  actif: boolean;

  // PHASE 6B.2: Statut de génération des opérations
  operationGenerationStatus?: string; // NON_GENEREE, GENEREE, PARTIELLE, ERREUR
  operationEpargneCount?: number;
  operationCaisseCount?: number;
  operationGenerationErrorMessage?: string | null;
}
