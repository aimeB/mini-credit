export interface Transaction {
  id: string;
  date: Date;
  montant: number;
  type: 'DEBIT' | 'CREDIT';
  description: string;
  status: 'COMPLETED' | 'PENDING' | 'FAILED';
  compteContrepartie: string;
  nomBeneficiaire: string;
  reference?: string;
}

export interface Compte {
  id: string;
  numero: string;
  solde: number;
  disponible: number;
  titulaire: string;
  type: 'PAIEMENT' | 'EPARGNE' | 'CREDIT';
  devise: string;
}
