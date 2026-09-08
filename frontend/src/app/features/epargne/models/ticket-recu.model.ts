export type TypeTicketRecu = 'DEPOT_EPARGNE' | 'RETRAIT_EPARGNE' | 'COLLECTE_TERRAIN' | 'DUPLICATA' | 'AJUSTEMENT_CONTROLE';
export type StatutTicketRecu = 'GENERE' | 'IMPRIME' | 'ECHEC_IMPRESSION' | 'DUPLICATA_GENERE' | 'ANNULE';
export type FormatTicketRecu = 'THERMIQUE_58MM' | 'THERMIQUE_80MM' | 'A4';

export interface TicketRecuResponse {
  id: number;
  numeroTicket: string;
  typeTicket: TypeTicketRecu;
  statut: StatutTicketRecu;
  duplicata?: boolean;
  originalTicketId?: number | null;
  nombreImpressions?: number;
  nombreDuplicatas?: number;
  dateGeneration?: string;
  dateDerniereImpression?: string | null;
  operationEpargneId?: number | null;
  operationCaisseId?: number | null;
  operationCaisseCommissionId?: number | null;
  demandeRetraitEpargneId?: number | null;
  sessionCaisseId?: number | null;
  caisseId?: number | null;
  membreId: number;
  membreNom?: string;
  compteEpargneId: number;
  numeroCompte?: string;
  agenceNom?: string;
  siteNom?: string;
  devise?: string;
  montantPrincipal: number;
  tauxCommission?: number | null;
  montantCommission?: number | null;
  montantTotalDebite?: number | null;
  montantRemisMembre?: number | null;
  ancienSolde: number;
  nouveauSolde: number;
  commentaire?: string | null;
  motifDuplicata?: string | null;
  codeVerification: string;
  qrPayload?: string | null;
  utilisateurCreateurNom?: string | null;
  utilisateurImpressionNom?: string | null;
  utilisateurDuplicataNom?: string | null;
}

export interface TicketPrintRequest {
  format?: FormatTicketRecu;
  marquerImprime?: boolean;
  impressionReussie?: boolean;
  commentaire?: string;
}

export interface TicketDuplicataRequest {
  motif: string;
}

export interface TicketVerificationResponse {
  valide: boolean;
  numeroTicket?: string;
  typeTicket?: TypeTicketRecu;
  statut?: StatutTicketRecu;
  membreMasque?: string;
  montantPrincipal?: number;
  devise?: string;
  dateGeneration?: string;
  message?: string;
}
