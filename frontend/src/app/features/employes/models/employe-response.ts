import { FonctionEmploye } from './fonction-employe';

export interface EmployeResponse {
  id: number;
  matricule: string;
  nom: string;
  prenom: string;
  nomComplet: string;
  telephone: string;
  photoUrl?: string;
  adresse?: string;
  commune?: string;
  fonction?: FonctionEmploye;
  dateEmbauche: string;
  salaireBase: number;
  primeFixe: number;
  bonusVariable: number;
  totalRemuneration: number;
  actif: boolean;
  agenceId: number;
  nomAgence?: string;
  siteId?: number | null;
  nomSite?: string | null;
  utilisateurId?: number;
  roleUtilisateur?: string;
  dateCreation: string;
  dateModification: string;
}
