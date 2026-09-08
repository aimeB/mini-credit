export type RecommandationRisque =
  | 'FAVORABLE'
  | 'FAVORABLE_AVEC_RESERVE'
  | 'DEFAVORABLE'
  | 'DOSSIER_INCOMPLET';

export const RECOMMANDATION_RISQUE_OPTIONS: RecommandationRisque[] = [
  'FAVORABLE',
  'FAVORABLE_AVEC_RESERVE',
  'DEFAVORABLE',
  'DOSSIER_INCOMPLET'
];

export const RECOMMANDATION_RISQUE_LABELS: Record<RecommandationRisque, string> = {
  FAVORABLE: 'Favorable',
  FAVORABLE_AVEC_RESERVE: 'Favorable avec réserve',
  DEFAVORABLE: 'Défavorable',
  DOSSIER_INCOMPLET: 'Dossier incomplet'
};