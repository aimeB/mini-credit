import { DureeUnite } from '../../../shared/enums/duree-unite.enum';

export type CollecteStatut = 'BROUILLON' | 'SOUMISE' | 'VALIDEE' | 'REJETEE';

export type TypeLigneCollecte =
  | 'EPARGNE'
  | 'REMBOURSEMENT_CREDIT'
  | 'CARNET'
  | 'FRAIS_ANALYSE'
  | 'DEMANDE_CREDIT';

export type ModaliteRemboursementCollecte =
  | 'JOURNALIERE'
  | 'HEBDOMADAIRE'
  | 'MENSUELLE';

export interface CreateCollecteTerrainRequest {
  especesRemises?: number;
  especesDeclareesAgent?: number;
  observations?: string;
}

export interface ConfirmerBilletageRequest {
  especesConfirmeesCaissier: number;
  observationBilletage?: string;
}

export interface CreateCollecteMembreLigneRequest {
  membreId: number;
  compteEpargneId?: number | null;
  creditId?: number | null;
  demandeCreditId?: number | null;
  typeLigne: TypeLigneCollecte;
  montant?: number;
  quantite?: number;
  reference?: string;
  commentaire?: string;
  montantSouhaite?: number;
  objetCredit?: string;
  gagePropose?: string;
  dureeValeur?: number;
  dureeUnite?: DureeUnite;
  modaliteRemboursement?: ModaliteRemboursementCollecte;
}

export interface UpdateCollecteMembreLigneRequest extends CreateCollecteMembreLigneRequest {}

export interface CollecteMembreLigneResponse {
  id: number;
  membreId: number;
  membreCode?: string;
  membreNom?: string;
  compteEpargneId?: number | null;
  creditId?: number | null;
  demandeCreditId?: number | null;
  typeLigne: TypeLigneCollecte;
  montant: number;
  quantite: number;
  reference?: string;
  commentaire?: string;
  totalLigne?: number;
  montantSouhaite?: number;
  objetCredit?: string;
  gagePropose?: string;
  dureeValeur?: number;
  dureeUnite?: DureeUnite;
  modaliteRemboursement?: ModaliteRemboursementCollecte;
}

export interface CollecteTerrainResponse {
  id: number;
  agentTerrainId: number;
  agentTerrainNom?: string;
  siteId: number;
  siteNom?: string;
  antenneId: number;
  dateCollecte: string;
  statut: CollecteStatut;
  especesRemises: number;
  especesDeclareesAgent?: number;
  especesConfirmeesCaissier?: number;
  dateConfirmationBilletage?: string;
  confirmeParCaissierId?: number;
  confirmeParCaissierNom?: string;
  billetagePar?: number;
  billetageParNom?: string;
  dateBilletage?: string;
  observationBilletage?: string;
  billetageConfirme?: boolean;
  totalEpargneCalcule: number;
  totalRemboursementsCalcule: number;
  totalFraisCalcule: number;
  totalCarnetsCalcule: number;
  totalGeneralCalcule: number;
  ecartTresorerie: number;
  observations?: string;
  operationsGeneratedAt?: string;
  operationsGeneratedCount?: number;
  generationSummary?: string;
  lignes: CollecteMembreLigneResponse[];
}

export interface CollecteRecapResponse {
  collecteId: number;
  membresVisites: number;
  nouveauxMembres: number;
  carnetsVendusDistribues: number;
  totalEpargne: number;
  totalRemboursements: number;
  totalFrais: number;
  totalGeneralAttendu: number;
  especesRemises: number;
  ecartTresorerie: number;
}

export interface ValidateCollecteTerrainRequest {
  decision: 'VALIDEE' | 'REJETEE';
  motifRejet?: string;
}
