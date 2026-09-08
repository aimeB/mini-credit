export enum RecetteStatut {
  BROUILLON = 'BROUILLON',
  SOUMISE = 'SOUMISE',
  VALIDEE = 'VALIDEE',
  REJETEE = 'REJETEE'
}

export const RECETTE_STATUT_LABELS: Record<RecetteStatut, string> = {
  [RecetteStatut.BROUILLON]: 'Brouillon',
  [RecetteStatut.SOUMISE]: 'Soumise',
  [RecetteStatut.VALIDEE]: 'Validée',
  [RecetteStatut.REJETEE]: 'Rejetée'
};

export const RECETTE_STATUT_COLORS: Record<RecetteStatut, string> = {
  [RecetteStatut.BROUILLON]: 'warning',
  [RecetteStatut.SOUMISE]: 'info',
  [RecetteStatut.VALIDEE]: 'success',
  [RecetteStatut.REJETEE]: 'danger'
};
