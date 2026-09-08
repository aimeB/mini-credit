export type StatutGarantieEpargne =
  | 'NON_VERIFIEE'
  | 'INSUFFISANTE'
  | 'SUFFISANTE'
  | 'BLOQUEE'
  | 'VALIDEE'
  | 'LIBEREE';

export type StatutGarantieMaterielle =
  | 'DECLAREE'
  | 'CONTROLEE'
  | 'ACCEPTEE'
  | 'REFUSEE'
  | 'RESTITUEE'
  | 'SAISIE';

export type StatutGarantieGlobal = 'EN_ATTENTE' | 'INSUFFISANTE' | 'VALIDEE' | 'REFUSEE' | string;

export interface GarantieMaterielleResponse {
  id: number;
  garantieCreditId: number;
  typeBien: string;
  description: string;
  valeurEstimee: number;
  devise: string;
  proprietaireDeclare?: string | null;
  localisation?: string | null;
  referenceDocument?: string | null;
  statut: StatutGarantieMaterielle;
  controleParId?: number | null;
  controleParNom?: string | null;
  dateControle?: string | null;
  commentaire?: string | null;
}

export interface GarantieCreditResponse {
  id: number;
  demandeCreditId: number;
  creditId?: number | null;
  membreId: number;
  compteEpargneId?: number | null;
  devise: string;
  montantDemande?: number | null;
  gagePropose?: string | null;
  montantCredit: number;
  montantGarantieRequis: number;
  montantGarantieBloque: number;
  montantGarantieManquant: number;
  statutGarantieEpargne: StatutGarantieEpargne;
  controleParId?: number | null;
  controleParNom?: string | null;
  dateControle?: string | null;
  dateBlocage?: string | null;
  dateLiberation?: string | null;
  commentaireControle?: string | null;
  soldeDisponible: number;
  soldeBloque: number;
  montantMaterielTotal: number;
  valeurMinimaleGageMateriel?: number | null;
  valeurTotaleGarantiesMateriellesAcceptees?: number | null;
  ratioCouvertureMaterielle?: number | null;
  garantieMaterielleSuffisante?: boolean | null;
  statutGlobal: StatutGarantieGlobal;
  garantiesMaterielles: GarantieMaterielleResponse[];
}

export interface VerifierGarantieCreditRequest {
  commentaire?: string;
}

export interface BloquerGarantieEpargneRequest {
  compteEpargneId?: number | null;
  commentaire?: string;
}

export interface AjouterGarantieMaterielleRequest {
  typeBien: string;
  description: string;
  valeurEstimee: number;
  devise: string;
  proprietaireDeclare?: string;
  localisation?: string;
  referenceDocument?: string;
  commentaire?: string;
}

export interface ValiderGarantieRequest {
  commentaire: string;
}

export interface RejeterGarantieRequest {
  commentaire: string;
}
