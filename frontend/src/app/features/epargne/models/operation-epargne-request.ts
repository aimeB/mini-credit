import { TypeOperationEpargne } from './type-operation-epargne';
import { ModePaiement } from './mode-paiement';
import { SensOperation } from './sens-operation';

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