import { DureeUnite } from "../../../shared/enums/duree-unite.enum";
import { PeriodiciteRemboursement } from "../../../shared/enums/periodicite-remboursement.enum";
import { StatutDemandeCredit } from "./enums/statut-demande-credit.enum";

export interface DemandeCreditResponse {
  id: number;
  numeroDemande: string;

  membreId: number;
  membreNomComplet: string;

  siteId: number;
  siteNom: string;
  agentId?: number | null;

  dateDemande: string;

  montantDemande: number;

  fraisDemande: number;
  fraisDemandePayes: number;

  depotGarantieRequis: number;
  depotGarantiePaye: number;
  montantGarantieBloque?: number;
  garantieBloquee?: boolean;
  statutGarantie?: 'NON_BLOQUEE' | 'BLOQUEE' | 'INSUFFISANTE';

  devise: string;

  dureeValeur: number;
  dureeUnite: DureeUnite;
  periodiciteRemboursement: PeriodiciteRemboursement;

  tauxInteret: number;

  objetCredit: string;
  gagePropose?: string | null;
  activiteFinancee?: string | null;

  revenusEstimes: number;
  chargesEstimees: number;

  statut: StatutDemandeCredit;

  commentaireDecision?: string | null;
  dateDecision?: string | null;

  createdAt?: string | null;
  updatedAt?: string | null;
  analyseRisque?: AnalyseRisqueCreditResponse | null;
}

export interface AnalyseRisqueCreditResponse {
  scoreTotal: number;
  niveauRisque: 'FAIBLE' | 'MOYEN' | 'ELEVE';
  montantDemande: number;
  revenusMensuels: number;
  chargesMensuelles: number;
  capaciteRemboursement: number;
  mensualiteEstimee: number;
  ratioMensualiteCapacite?: number | null;
  ratioMontantRevenu?: number | null;
  garantieEpargneRequise: number;
  garantieEpargneDisponible: number;
  garantieMaterielleRequise?: number | null;
  garantieMaterielleDeclaree?: number | null;
  criteres: CritereAnalyseRisque[];
}

export interface CritereAnalyseRisque {
  codeCritere: string;
  libelle: string;
  points: number;
  niveau: string;
  commentaire: string;
}