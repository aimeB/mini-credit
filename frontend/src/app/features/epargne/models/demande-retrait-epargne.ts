import { StatutDemandeRetrait } from './statut-demande-retrait.enum';

/**
 * Réponse API pour une demande de retrait épargne (PHASE 6B.3)
 * Utilisé pour les opérations GET (détail, liste)
 */
export interface DemandeRetraitEpargneResponse {
  id: number;
  compteEpargneId: number;
  referenceRetrait?: string;
  numeroCompte?: string;
  compteEpargneNumero?: string;      // Pour affichage
  membreId: number;
  membreNom?: string;                // Pour affichage
  montantDemande: number;
  fraisRetrait: number;
  tauxCommissionRetrait?: number;
  montantTotalDebite?: number;
  montantRemisAuMembre?: number;
  soldeDisponible?: number;
  soldeBloque?: number;
  operationCaisseSortieId?: number | null;
  operationCaisseFraisId?: number | null;
  statut: StatutDemandeRetrait;
  dateDemande: Date;
  createdAt?: Date;
  motifRejet?: string | null;
  valideParId?: number;
  valideParNom?: string;             // Pour affichage
  dateValidation?: Date;
  observation?: string;
}

/**
 * Requête pour créer une demande de retrait épargne
 */
export interface CreateDemandeRetraitEpargneRequest {
  compteEpargneId: number;
  montantDemande: number;
  fraisRetrait?: number;
  observation?: string;
}

/**
 * Requête pour valider une demande (rejeter ou approuver)
 */
export interface ValidateDemandeRetraitEpargneRequest {
  approuvee: boolean;
  motifRejet?: string;
}

/**
 * Requête pour rejeter une demande avec motif
 */
export interface RejectDemandeRetraitEpargneRequest {
  motif: string;
}

/**
 * Requête pour payer/décaisser un retrait validé
 */
export interface PayDemandeRetraitEpargneRequest {
  // Vide généralement, le backend utilise juste l'ID
}
