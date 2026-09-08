export interface RapportCaisseSession {
  sessionId: number;
  caisseId: number | null;
  caisseCode: string | null;
  caisseLibelle: string | null;
  siteId: number | null;
  siteNom: string | null;
  dateComptable: string | null;
  statut: string | null;
  utilisateur: string | null;
  dateOuverture: string | null;
  dateCloture: string | null;
  soldeOuverture: number | null;
  totalEntrees: number | null;
  totalSorties: number | null;
  soldeTheorique: number | null;
  soldePhysique: number | null;
  ecartCaisse: number | null;
  nombreOperations: number;
  totalDepensesPayees: number | null;
}

export interface RapportCaisseJournalier {
  date: string;
  nombreSessions: number;
  nombreOperations?: number;
  nombreSessionsNonCloturees: number;
  totalEntrees: number;
  totalSorties: number;
  soldeTheoriqueTotal: number;
  soldePhysiqueTotal: number;
  ecartTotal: number;
  nombreDepenses: number;
  montantDepenses: number;
  nombreEcarts: number;
  montantEcarts: number;
}

export interface RapportCaissePeriode {
  dateDebut: string;
  dateFin: string;
  nombreSessions: number;
  nombreSessionsNonCloturees: number;
  totalEntrees: number;
  totalSorties: number;
  soldeTheoriqueTotal: number;
  soldePhysiqueTotal: number;
  ecartTotal: number;
  nombreOperations: number;
  nombreDepenses: number;
  montantDepenses: number;
  nombreEcarts: number;
  montantEcarts: number;
}

export interface RapportCaisseDepenseLigne {
  depenseId: number;
  sessionId: number | null;
  caisseId: number | null;
  caisseLibelle: string | null;
  siteId: number | null;
  siteNom: string | null;
  categorie: string | null;
  montant: number | null;
  devise: string | null;
  statut: string | null;
  demandePar: string | null;
  validePar: string | null;
  payePar: string | null;
  dateDemande: string | null;
  dateValidation: string | null;
  datePaiement: string | null;
  motif: string | null;
}

export interface RapportCaisseDepenses {
  dateDebut: string;
  dateFin: string;
  nombreDepenses: number;
  montantTotal: number;
  lignes: RapportCaisseDepenseLigne[];
}

export interface RapportCaisseEcartLigne {
  ecartId: number;
  sessionId: number | null;
  caisseId: number | null;
  caisseLibelle: string | null;
  siteId: number | null;
  siteNom: string | null;
  dateJour: string | null;
  typeEcart: string | null;
  statut: string | null;
  montantEcart: number | null;
  seuilDepasse: boolean | null;
  enquetePar: string | null;
  validePar: string | null;
  dateEnquete: string | null;
  dateValidation: string | null;
  description: string | null;
}

export interface RapportCaisseEcarts {
  dateDebut: string;
  dateFin: string;
  nombreEcarts: number;
  montantTotal: number;
  nombreEcartsOuverts: number;
  lignes: RapportCaisseEcartLigne[];
}
