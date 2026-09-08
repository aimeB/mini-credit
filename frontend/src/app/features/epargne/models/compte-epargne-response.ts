import { StatutCompte } from "./compte-epargne-create-request";
import { TypeCompteEpargne } from "./type-compte-epargne";

export interface CompteEpargneResponse {
  id: number;
  membreId: number;
  membreNomComplet: string;
  numeroCompte: string;

  typeCompte: TypeCompteEpargne; // ✅ CORRIGÉ
  soldeDisponible: number;
  soldeBloque: number;

  statut: StatutCompte; // ✅ CORRIGÉ

  dateOuverture: string;
  dateFermeture?: string;

  createdAt: string;  // ✅ pas optionnel
  updatedAt: string;  // ✅ pas optionnel
}