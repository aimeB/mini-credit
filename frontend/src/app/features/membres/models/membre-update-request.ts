import { Sexe } from "../../../shared/enums/sexe.enum";
import { StatutMembre } from "../enum/statut-membre";


export interface MembreUpdateRequest {
  nom?: string;
  postnom?: string;
  prenom?: string;

  sexe?: Sexe;
  dateNaissance?: string;

  telephonePrincipal?: string;
  telephoneSecondaire?: string;

  adresse?: string;
  ville?: string;
  commune?: string;
  quartier?: string;

  professionActivite?: string;
  lieuActivite?: string;
  sourceInscription?: string;

  dateAdhesion?: string;
  statut?: StatutMembre;

  observation?: string;
  agentId?: number;
  siteId?: number;
}