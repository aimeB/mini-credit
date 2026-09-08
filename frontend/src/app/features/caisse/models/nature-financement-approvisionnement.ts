export type NatureFinancementApprovisionnement =
  | 'TRANSFERT_INTERNE'
  | 'APPORT_PROPRIETAIRE'
  | 'PRET_RECU'
  | 'REMBOURSEMENT_AVANCE'
  | 'AUTRE_FINANCEMENT';

export const NATURE_FINANCEMENT_APPROVISIONNEMENT_LABELS: Record<NatureFinancementApprovisionnement, string> = {
  TRANSFERT_INTERNE: 'Transfert interne',
  APPORT_PROPRIETAIRE: 'Apport propriétaire / capital injecté',
  PRET_RECU: 'Prêt reçu par l’institution',
  REMBOURSEMENT_AVANCE: 'Remboursement d’avance',
  AUTRE_FINANCEMENT: 'Autre financement',
};
