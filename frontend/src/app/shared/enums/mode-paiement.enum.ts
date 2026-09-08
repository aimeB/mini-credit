export type ModePaiement = 'ESPECES' | 'MOBILE_MONEY' | 'VIREMENT' | 'CARTE' | 'COMPTE_EPARGNE' | 'AUTRE';

export const MODE_PAIEMENT_OPTIONS: ModePaiement[] = [
  'ESPECES',
  'COMPTE_EPARGNE',
  'MOBILE_MONEY',
  'VIREMENT',
  'CARTE',
  'AUTRE'
];