import { ModePaiement } from '../../epargne/models/mode-paiement';
import { CategorieOperationCaisse } from './categorie-operation-caisse';
import { NatureFinancementApprovisionnement } from './nature-financement-approvisionnement';
import { SourceOperationCaisse } from './source-operation-caisse';
import { TypeOperationCaisse } from './type-operation-caisse';


export interface OperationCaisseRequest {
  sessionCaisseId: number;
  caisseId: number;
  dateOperation: string;
  typeOperation: TypeOperationCaisse;
  categorieOperation: CategorieOperationCaisse;
  natureFinancement?: NatureFinancementApprovisionnement;
  montant: number;

  /** PATCH 7 — Obligatoire côté backend (@NotNull). Ne jamais utiliser LEGACY. */
  source: SourceOperationCaisse;

  /** PATCH 7 — Référence externe lisible (bon d'appro, numéro crédit, etc.). */
  referenceExterne?: string;

  /** PATCH 7 — Obligatoire uniquement si source = RECETTE_JOURNALIERE. */
  recetteId?: number;

  devise?: string;
  membreId?: number;
  creditId?: number;
  remboursementId?: number;
  operationEpargneId?: number;
  agentId?: number;
  description?: string;
  createdBy?: number;
  modePaiement?: ModePaiement;
  observation?: string;
  commentaire?: string;
}