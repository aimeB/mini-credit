export interface UtilisateurCreateRequest {
  username: string;
  password: string;
  /** Email OPTIONNEL */
  email?: string;
  /** OPTIONNEL — repris depuis l'employé lié si employeId fourni */
  nomComplet?: string;
  /** OPTIONNEL — repris depuis l'employé lié si employeId fourni */
  telephone?: string;
  roles: string[];
  /** Obligatoire pour les rôles opérationnels (sauf ADMIN et MEMBER) */
  employeId?: number;
}
