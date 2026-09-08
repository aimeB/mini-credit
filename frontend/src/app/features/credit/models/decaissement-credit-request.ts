import { ModePaiement } from '../../../shared/enums/mode-paiement.enum';

export interface DecaissementCreditRequest {
  dateDecaissement: string;
  sessionCaisseId: number;
  createdBy: number;
  modePaiement: ModePaiement;
  observation?: string | null;
}