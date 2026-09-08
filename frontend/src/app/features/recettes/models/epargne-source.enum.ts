export enum EpargneSourceType {
  VOLONTAIRE = 'VOLONTAIRE',
  COTISATION = 'COTISATION',
  DIVIDENDE = 'DIVIDENDE'
}

export const EPARGNE_SOURCE_LABELS: Record<EpargneSourceType, string> = {
  [EpargneSourceType.VOLONTAIRE]: 'Épargne Volontaire',
  [EpargneSourceType.COTISATION]: 'Cotisation',
  [EpargneSourceType.DIVIDENDE]: 'Dividende'
};
