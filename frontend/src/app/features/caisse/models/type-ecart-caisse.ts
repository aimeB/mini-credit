/**
 * PATCH 9 — Types d'écart caisse.
 * Aligné sur TypeEcartCaisse.java backend.
 */
export type TypeEcartCaisse = 'DEFICIT' | 'SURPLUS';

export const TYPE_ECART_CAISSE_LABELS: Record<TypeEcartCaisse, string> = {
  DEFICIT: 'Déficit (solde physique < théorique)',
  SURPLUS: 'Surplus (solde physique > théorique)',
};
