export interface UtilisateurUpdateRequest {
  email?: string;
  nomComplet?: string;
  /** Format obligatoire : +243 suivi de 9 chiffres */
  telephone?: string;
  active?: boolean;
  roles?: string[];
  /** Employé lié (optionnel — permet de lier un compte à une fiche employé existante) */
  employeId?: number;
}
