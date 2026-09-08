export interface UtilisateurResponse {
  id: number;
  username: string;
  email?: string;
  nomComplet?: string;
  telephone?: string;
  active: boolean;
  roles: string[];
  passwordResetRequired?: boolean;
  passwordChangeRequired?: boolean;
  /** Employé lié — null si ADMIN technique ou compte sans fiche employé */
  employeId?: number;
  employeMatricule?: string;
  employeNomComplet?: string;
  /** Fonction métier de l'employé lié, ex : CAISSIER, AGENT_TERRAIN */
  employeFonction?: string;
  /** Téléphone de l'employé lié */
  employeTelephone?: string;
  employeAgenceNom?: string;
  employeSiteNom?: string;
  dateCreation?: string;
  dateModification?: string;
}
