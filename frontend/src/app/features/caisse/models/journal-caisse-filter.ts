import { CategorieOperationCaisse } from './categorie-operation-caisse';
import { SourceOperationCaisse } from './source-operation-caisse';
import { TypeOperationCaisse } from './type-operation-caisse';

export interface JournalCaisseFilter {
  sessionCaisseId?: number;
  caisseId?: number;
  siteId?: number;
  utilisateurId?: number;
  typeOperation?: TypeOperationCaisse;
  categorie?: CategorieOperationCaisse;
  source?: SourceOperationCaisse;
  referenceMetier?: string;
  dateDebut?: string;
  dateFin?: string;
  recetteId?: number;
  depenseCaisseId?: number;
  creditId?: number;
  retraitEpargneId?: number;
  operationEpargneId?: number;
}