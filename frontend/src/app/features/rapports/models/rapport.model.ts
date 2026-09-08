export interface RapportFinancier {
  dateRapport: string;
  periodeDebut: string;
  periodeFin: string;
  
  // Résumé Crédits
  totalCreditOctroye: number;
  totalPrincipalRembourse: number;
  totalInteretCollecte: number;
  totalEncoursPrincipal: number;
  totalEcheancesEchues: number;
  
  // Taux et Risque
  tauxRemboursement: number;
  tauxDefaut: number;
  nombreCreditActif: number;
  nombreCreditEndefaut: number;
  
  // Caisse
  totalEntrees: number;
  totalSorties: number;
  soldeNet: number;
  
  // Épargne
  totalEpargneCollectee: number;
  nombreComptesActifs: number;
  
  // Opérationnel
  nombreMembres: number;
  nombreNouvelleDemande: number;
  nombreCreditDisbursee: number;
}

export interface KPIDashboard {
  totalClients: number;
  totalCreditActifs: number;
  totalPortefeuilleActual: number;
  tauxDefautPortefeuille: number;
  
  revenus30Jours: number;
  depenses30Jours: number;
  benefice30Jours: number;
  
  epargneCollectee: number;
  capacitePretageDisponible: number;
}

export interface RapportRisque {
  dateAnalyse: string;
  numeroCredit: string;
  membreNom: string;
  
  montantOctroye: number;
  encoursPrincipal: number;
  joursEnRetard: number;
  
  niveauRisque: 'FAIBLE' | 'MOYEN' | 'ELEVE' | 'CRITIQUE';
  raisons: string[];
  
  recommendationAction: string;
  urgence: 'BASSE' | 'NORMALE' | 'HAUTE' | 'CRITIQUE';
}

export interface RapportCollecte {
  dateDebut: string;
  dateFin: string;
  
  type: 'REMBOURSEMENT' | 'INTERET' | 'EPARGNE' | 'PENALITE';
  
  montantPrevu: number;
  montantCollecte: number;
  montantManquant: number;
  tauxCollecte: number;
  
  details: CollecteDetail[];
}

export interface CollecteDetail {
  date: string;
  montant: number;
  modePaiement: string;
  operationId: number;
}
