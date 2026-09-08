import { CategorieOperationCaisse } from './categorie-operation-caisse';
import { SourceOperationCaisse } from './source-operation-caisse';
import { TypeOperationCaisse } from './type-operation-caisse';

export interface JournalCaisseResponse {
  operationId: number;
  sessionCaisseId?: number;
  caisseId?: number;
  caisseLibelle?: string;
  siteId?: number;
  siteLibelle?: string;
  dateOperation: string;
  dateOperationJour?: string;
  heureOperation?: string;
  utilisateurId?: number;
  utilisateurNom?: string;
  roleUtilisateur?: string;
  typeOperation: TypeOperationCaisse;
  categorie: CategorieOperationCaisse;
  source?: SourceOperationCaisse;
  montant: number;
  devise: string;
  soldeApresOperation?: number;
  commentaire?: string;
  referenceMetier?: string;
  recetteId?: number;
  depenseCaisseId?: number;
  creditId?: number;
  retraitEpargneId?: number;
  operationEpargneId?: number;
  statutSession?: string;
}