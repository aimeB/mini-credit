import { ModePaiement } from '../../epargne/models/mode-paiement';
import { CategorieOperationCaisse } from './categorie-operation-caisse';
import { NatureFinancementApprovisionnement } from './nature-financement-approvisionnement';
import { SourceOperationCaisse } from './source-operation-caisse';
import { TypeOperationCaisse } from './type-operation-caisse';

export interface OperationCaisseResponse {
  id: number;
  numeroPiece: string;
  sessionCaisseId: number;
  caisseId: number;
  dateOperation: string;

  typeOperation: TypeOperationCaisse;
  categorieOperation: CategorieOperationCaisse;
  natureFinancement?: NatureFinancementApprovisionnement;

  /** PATCH 7 — Source de tracabilité retournée par le backend. */
  source?: SourceOperationCaisse;

  /** PATCH 7 — Référence externe lisible selon la source. */
  referenceExterne?: string;

  montant: number;
  devise: string;

  membreId?: number;
  creditId?: number;
  remboursementId?: number;
  operationEpargneId?: number;
  paiementCreditId?: number;

  agentId?: number;
  createdById?: number;
  modePaiement?: ModePaiement;
  description?: string;

  createdAt: string;
  updatedAt: string;
}