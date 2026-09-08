import { Sexe } from "../../../shared/enums/sexe.enum";
import { StatutMembre } from "../enum/statut-membre";



export interface MembreResponse {
  id: number;

  codeMembre: string;

  nom: string;
  postnom?: string;
  prenom?: string;
  nomComplet?: string;

  sexe?: Sexe;
  dateNaissance?: string;

  telephonePrincipal?: string;
  telephoneSecondaire?: string;
  email?: string;

  adresse?: string;
  quartier?: string;
  commune?: string;
  ville?: string;

  professionActivite?: string;
  lieuActivite?: string;
  sourceInscription?: string;

  siteId: number;
  siteNom: string;

  agentId?: number;
  agentMatricule?: string;

  dateAdhesion: string;
  statut: StatutMembre;

  photoUrl?: string;
  observation?: string;

  createdAt: string;
  updatedAt: string;
}