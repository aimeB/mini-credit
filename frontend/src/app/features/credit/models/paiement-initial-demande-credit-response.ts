import { DemandeCreditResponse } from './demande-credit-response';
import { OperationCaisseResponse } from '../../caisse/models/operation-caisse-response';

export interface PaiementInitialDemandeCreditResponse {
  demandeId: number;
  numeroDemande: string;

  membreId: number;
  membreNomComplet: string;

  devise: string;

  fraisPayesSurCetteOperation: number;
  depotGarantiePayeSurCetteOperation: number;
  totalPayeSurCetteOperation: number;

  fraisDemandeTotal: number;
  fraisDemandePayesTotal: number;
  fraisDemandeRestants: number;

  depotGarantieRequis: number;
  depotGarantiePayeTotal: number;
  depotGarantieRestant: number;

  demande: DemandeCreditResponse;
  operationsCaisse: OperationCaisseResponse[];
}