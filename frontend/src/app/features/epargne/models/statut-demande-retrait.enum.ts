/**
 * Enum pour les statuts de demande de retrait épargne (PHASE 6B.3)
 * Workflow 3N: CREEE → EN_ATTENTE_VALIDATION → VALIDEE/REJETEE → DECAISSEE ou ANNULEE
 */
export enum StatutDemandeRetrait {
  CREEE = 'CREEE',
  EN_ATTENTE_VALIDATION = 'EN_ATTENTE_VALIDATION',
  VALIDEE = 'VALIDEE',
  REJETEE = 'REJETEE',
  DECAISSEE = 'DECAISSEE',
  ANNULEE = 'ANNULEE'
}

/**
 * Mapping des labels pour affichage dans l'UI
 */
export const STATUT_LABELS: Record<StatutDemandeRetrait, string> = {
  [StatutDemandeRetrait.CREEE]: 'Créée - En attente',
  [StatutDemandeRetrait.EN_ATTENTE_VALIDATION]: 'En attente de validation',
  [StatutDemandeRetrait.VALIDEE]: 'Validée - Prête pour décaissement',
  [StatutDemandeRetrait.REJETEE]: 'Rejetée',
  [StatutDemandeRetrait.DECAISSEE]: 'Décaissée - Retrait effectué',
  [StatutDemandeRetrait.ANNULEE]: 'Annulée'
};

/**
 * Mapping des couleurs pour affichage des badges
 */
export const STATUT_COLORS: Record<StatutDemandeRetrait, string> = {
  [StatutDemandeRetrait.CREEE]: 'secondary',
  [StatutDemandeRetrait.EN_ATTENTE_VALIDATION]: 'warning',
  [StatutDemandeRetrait.VALIDEE]: 'info',
  [StatutDemandeRetrait.REJETEE]: 'danger',
  [StatutDemandeRetrait.DECAISSEE]: 'success',
  [StatutDemandeRetrait.ANNULEE]: 'dark'
};
