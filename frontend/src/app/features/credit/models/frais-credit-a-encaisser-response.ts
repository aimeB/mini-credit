import { StatutDemandeCredit } from './enums/statut-demande-credit.enum';

export interface FraisCreditAEncaisserResponse {
  demandeCreditId: number;
  numeroDemande: string;
  membreNomComplet: string;
  siteNom?: string | null;
  antenneNom?: string | null;
  dateDemande: string;
  montantDemande: number;
  fraisDemande: number;
  fraisDemandePayes: number;
  resteFraisAPayer: number;
  statutDemande: StatutDemandeCredit;
  objetCredit?: string | null;
  sessionCaisseRequise: boolean;
}