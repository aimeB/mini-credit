import { TypeCompteEpargne } from "./type-compte-epargne";

export type StatutCompte = 'ACTIF' | 'INACTIF' | 'FERME' | 'BLOQUE';

export interface CompteEpargneCreateRequest {
  membreId: number;
  typeCompte: TypeCompteEpargne;
  dateOuverture: string; // yyyy-MM-dd
}

