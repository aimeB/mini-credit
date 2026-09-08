/**
 * PATCH 9 — Statuts du workflow écart caisse.
 * Aligné sur StatutEcartCaisse.java backend.
 */
export type StatutEcartCaisse =
  | 'DETECTE'
  | 'EN_INVESTIGATION'
  | 'RESOLU'
  | 'ACCEPTE'
  | 'REJETE';

export const STATUT_ECART_CAISSE_LABELS: Record<StatutEcartCaisse, string> = {
  DETECTE: 'Détecté',
  EN_INVESTIGATION: 'En investigation',
  RESOLU: 'Résolu',
  ACCEPTE: 'Accepté',
  REJETE: 'Rejeté',
};

/** Classe CSS Tailwind associée à chaque statut (badge couleur). */
export const STATUT_ECART_CAISSE_CSS: Record<StatutEcartCaisse, string> = {
  DETECTE: 'bg-yellow-100 text-yellow-800',
  EN_INVESTIGATION: 'bg-blue-100 text-blue-800',
  RESOLU: 'bg-green-100 text-green-800',
  ACCEPTE: 'bg-emerald-100 text-emerald-800',
  REJETE: 'bg-red-100 text-red-800',
};
