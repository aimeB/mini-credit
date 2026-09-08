import { Sexe } from "../../../shared/enums/sexe.enum";


export interface MembreCreateRequest {
  nom: string;

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

  email: string;

  siteId: number;
  agentId?: number;

  dateAdhesion: string;

  observation?: string;
}