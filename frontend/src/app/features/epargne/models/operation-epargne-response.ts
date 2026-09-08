import { ModePaiement } from './mode-paiement';

export type TypeOperationEpargne =
  | 'COTISATION'
  | 'EPARGNE'
  | 'RETRAIT'
  | 'BLOCAGE_GARANTIE'
  | 'DEBLOCAGE_GARANTIE'
  | 'AJUSTEMENT';

export type SensOperation = 'ENTREE' | 'SORTIE';

export interface OperationEpargneRequest {
  compteEpargneId: number;
  membreId: number;
  dateOperation: string;
  typeOperation: TypeOperationEpargne;
  montant: number;
  sens?: SensOperation;
  modePaiement?: ModePaiement;
  referenceExterne?: string;
  agentId?: number;
  sessionCaisseId?: number;
  createdBy?: number;
  observation?: string;
}

export interface OperationEpargneResponse {
  id: number;
  compteEpargneId: number;
  numeroCompte: string;
  membreId: number;
  membreNomComplet: string;
  dateOperation: string;
  typeOperation: TypeOperationEpargne;
  montant: number;
  sens: SensOperation;
  modePaiement?: ModePaiement;
  referenceExterne?: string;
  agentId?: number;
  sessionCaisseId?: number;
  observation?: string;
  createdAt: string;
  updatedAt: string;
}