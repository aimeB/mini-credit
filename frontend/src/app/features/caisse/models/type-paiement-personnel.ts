export type TypePaiementPersonnel =
  | 'SALAIRE'
  | 'SALAIRE_COMPLET'
  | 'SALAIRE_PARTIEL'
  | 'AVANCE'
  | 'AVANCE_SALAIRE'
  | 'RETENUE_SALAIRE'
  | 'PRIME'
  | 'COMMISSION'
  | 'REGULARISATION'
  | 'AUTRE';

export interface TypePaiementPersonnelOption {
  value: TypePaiementPersonnel;
  label: string;
}

export const TYPE_PAIEMENT_PERSONNEL_OPTIONS: TypePaiementPersonnelOption[] = [
  { value: 'SALAIRE_COMPLET', label: 'Salaire complet' },
  { value: 'SALAIRE_PARTIEL', label: 'Paiement partiel de salaire' },
  { value: 'AVANCE_SALAIRE', label: 'Avance sur salaire' },
  { value: 'RETENUE_SALAIRE', label: 'Retenue sur salaire' },
  { value: 'PRIME', label: 'Prime' },
  { value: 'COMMISSION', label: 'Commission' },
  { value: 'REGULARISATION', label: 'Régularisation' },
  { value: 'AUTRE', label: 'Autre' }
];
