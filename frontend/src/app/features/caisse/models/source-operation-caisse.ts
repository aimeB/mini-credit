/**
 * PATCH 7 — Source d'une opération caisse.
 * Obligatoire côté backend (@NotNull).
 * Ne jamais utiliser LEGACY pour une nouvelle opération côté frontend.
 */
export enum SourceOperationCaisse {
  RECETTE_JOURNALIERE = 'RECETTE_JOURNALIERE',
  RETRAIT_EPARGNE     = 'RETRAIT_EPARGNE',
  CREDIT_DECAISSEMENT = 'CREDIT_DECAISSEMENT',
  CREDIT_REMBOURSEMENT = 'CREDIT_REMBOURSEMENT',
  MANUEL              = 'MANUEL',
  AJUSTEMENT          = 'AJUSTEMENT',
  APPROVISIONNEMENT   = 'APPROVISIONNEMENT',
  AUTRE               = 'AUTRE',
  DEPENSE_CAISSE      = 'DEPENSE_CAISSE',
}

/** Labels lisibles pour les formulaires */
export const SOURCE_OPERATION_CAISSE_LABELS: Record<SourceOperationCaisse, string> = {
  [SourceOperationCaisse.RECETTE_JOURNALIERE]:  'Recette journalière terrain',
  [SourceOperationCaisse.RETRAIT_EPARGNE]:      'Retrait épargne',
  [SourceOperationCaisse.CREDIT_DECAISSEMENT]:  'Décaissement crédit',
  [SourceOperationCaisse.CREDIT_REMBOURSEMENT]: 'Remboursement crédit',
  [SourceOperationCaisse.MANUEL]:               'Saisie manuelle',
  [SourceOperationCaisse.AJUSTEMENT]:           'Ajustement / contrepassation',
  [SourceOperationCaisse.APPROVISIONNEMENT]:    'Approvisionnement caisse',
  [SourceOperationCaisse.AUTRE]:                'Autre',
  [SourceOperationCaisse.DEPENSE_CAISSE]:       'Dépense caisse',
};
