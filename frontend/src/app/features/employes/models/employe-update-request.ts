import { FonctionEmploye } from './fonction-employe';

export interface UpdateEmployeRequest {
  nom?: string;
  prenom?: string;
  /** Format obligatoire : +243 suivi de 9 chiffres */
  telephone?: string;
  /** URL http(s) ou chemin relatif de la photo ; vide pour supprimer */
  photoUrl?: string | null;
  adresse?: string;
  commune?: string;
  fonction?: FonctionEmploye;
  dateEmbauche?: string; // ISO date
  salaireBase?: number;
  primeFixe?: number;
  bonusVariable?: number;
  actif?: boolean;
  agenceId?: number;
  siteId?: number | null;
  /** Permet de lier un compte utilisateur après création de l'employé */
  utilisateurId?: number;
}
