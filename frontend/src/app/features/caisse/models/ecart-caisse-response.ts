import { StatutEcartCaisse } from './statut-ecart-caisse';
import { TypeEcartCaisse } from './type-ecart-caisse';

/**
 * PATCH 9 — Réponse backend pour un écart de caisse.
 * Aligné sur EcartCaisseDTO.java.
 */
export interface EcartCaisseResponse {
  id: number;
  sessionCaisseId: number;
  recetteId?: number;
  dateJour: string;
  typeEcart: TypeEcartCaisse;
  montantEcart: number;
  description?: string;
  statut: StatutEcartCaisse;
  /** Flag positionné par EcartThresholdConfigService — aucun seuil fixe côté frontend. */
  seuilDepassé: boolean;
  notesInvestigation?: string;
  raisonResolution?: string;
  enqueteParId?: number;
  dateEnquete?: string;
  valideParId?: number;
  dateValidation?: string;
}
