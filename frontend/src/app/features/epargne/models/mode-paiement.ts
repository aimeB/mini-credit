// Synchronized with backend ModePaiement enum
export type ModePaiement = 'ESPECES' | 'MOBILE_MONEY' | 'VIREMENT' | 'CARTE' | 'AUTRE';

export const MODE_PAIEMENT_OPTIONS: { value: ModePaiement; label: string }[] = [
  { value: 'ESPECES', label: 'Espèces' },
  { value: 'MOBILE_MONEY', label: 'Mobile Money' },
  { value: 'VIREMENT', label: 'Virement' },
  { value: 'CARTE', label: 'Carte' },
  { value: 'AUTRE', label: 'Autre' }
];