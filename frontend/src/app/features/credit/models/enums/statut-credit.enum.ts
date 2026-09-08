export type StatutCredit =
  | 'APPROUVE'
  | 'DECAISSE'
  | 'EN_COURS'
  | 'EN_RETARD'
  | 'REMBOURSE'
  | 'CONTENTIEUX'
  | 'ANNULE';

export const STATUT_CREDIT_OPTIONS: StatutCredit[] = [
  'APPROUVE',
  'DECAISSE',
  'EN_COURS',
  'EN_RETARD',
  'REMBOURSE',
  'CONTENTIEUX',
  'ANNULE'
];