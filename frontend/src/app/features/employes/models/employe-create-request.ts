import { FonctionEmploye } from './fonction-employe';

/**
 * Requête de création d'un employé.
 * Le matricule n'est PAS inclus : il est généré automatiquement côté backend
 * au format AGENCE-FONCTION-AA-SEQ (ex. DEL1-GES-26-001).
 */
export interface CreateEmployeRequest {
  nom: string;
  prenom: string;
  /** Format obligatoire : +243 suivi de 9 chiffres */
  telephone: string;
  /** URL http(s) ou chemin relatif de la photo */
  photoUrl?: string | null;
  adresse?: string;
  commune?: string;
  /** Fonction/poste métier — liste fermée */
  fonction: FonctionEmploye;
  dateEmbauche: string; // ISO date
  salaireBase: number;
  primeFixe?: number;
  bonusVariable?: number;
  agenceId: number;
  siteId?: number | null;
  /** OPTIONNEL : un employé peut exister sans compte utilisateur */
  utilisateurId?: number;
}
