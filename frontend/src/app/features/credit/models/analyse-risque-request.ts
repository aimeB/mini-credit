import { NiveauRisque } from "./enums/niveau-risque.enum";
import { RecommandationRisque } from "./enums/recommandation-risque.enum";

export interface AnalyseRisqueRequest {
  analysteId?: number;
  dateVisite?: string;
  lieuVisite?: string;
  activiteVerifiee?: boolean;
  descriptionActivite?: string;
  ancienneteActivite?: string;
  chiffreAffairesEstime?: number;
  chiffreAffairesDevise?: string;
  revenuNetEstime?: number;
  revenuNetDevise?: string;
  chargesMensuelles?: number;
  chargesMensuellesDevise?: string;
  capaciteRemboursement?: number;
  capaciteRemboursementDevise?: string;
  montantDemandeDevise?: string;
  fraisDemandeDevise?: string;
  depotRequisDevise?: string;
  depotPayeDevise?: string;
  risqueNiveau?: NiveauRisque;
  scoreRisque?: number;
  scoreRisqueCorrigeManuellement?: boolean;
  recommandation: RecommandationRisque;
  commentaire?: string;
}