export interface CreditDetailResponse {
  resume: ResumeCredit;
  responsables: CreditResponsables;
  garantie: CreditGarantieDetail;
  echeancier: CreditEcheanceDetail[];
  suiviFinancier: CreditSuiviFinancier;
  remboursements: CreditRemboursementDetail[];
  historique: CreditHistoriqueEvent[];
}

export interface ResumeCredit {
  id: number;
  numeroCredit: string;
  demandeCreditId?: number | null;
  numeroDemande?: string | null;
  membreNomComplet?: string | null;
  statut: string;
  montantAccorde: number;
  montantDecaisse: number;
  dateDemande?: string | null;
  dateApprobation?: string | null;
  dateDecaissement?: string | null;
  dureeValeur?: number | null;
  dureeUnite?: string | null;
  periodiciteRemboursement?: string | null;
  objetCredit?: string | null;
  gagePropose?: string | null;
  antenneId?: number | null;
  antenneNom?: string | null;
  devise?: string | null;
}

export interface CreditResponsables {
  agentTerrainId?: number | null;
  agentTerrainNom?: string | null;
  gestionnaireId?: number | null;
  gestionnaireNom?: string | null;
  gestionnaireNomComplet?: string | null;
  controleurId?: number | null;
  controleurNom?: string | null;
  chefBureauId?: number | null;
  chefBureauNom?: string | null;
  caissierId?: number | null;
  caissierNom?: string | null;
  caissierNomComplet?: string | null;
}

export interface CreditGarantieDetail {
  montantGarantieRequis: number;
  montantGarantieBloque: number;
  sourceGarantie?: string | null;
  garantiesMateriellesAcceptees?: string | null;
  statutGarantie?: string | null;
  dateBlocage?: string | null;
  dateLiberation?: string | null;
}

export interface CreditEcheanceDetail {
  id: number;
  numeroEcheance: number;
  dateEcheance?: string | null;
  capitalDu: number;
  interetDu: number;
  penalite: number;
  totalDu: number;
  montantPaye: number;
  resteAPayer: number;
  statut: string;
}

export interface CreditSuiviFinancier {
  capitalInitial: number;
  capitalRembourse: number;
  capitalRestant: number;
  interetsAttendus: number;
  interetsPayes: number;
  interetsRestants: number;
  penalitesDues: number;
  penalitesPayees: number;
  totalPaye: number;
  totalRestant: number;
}

export interface CreditRemboursementDetail {
  id: number;
  numeroRecu?: string | null;
  datePaiement?: string | null;
  montantPaye: number;
  capitalPaye: number;
  interetPaye: number;
  penalitePayee: number;
  utilisateurNom?: string | null;
  source?: string | null;
  observation?: string | null;
}

export interface CreditHistoriqueEvent {
  type: string;
  utilisateur?: string | null;
  dateHeure?: string | null;
  antenneId?: number | null;
  antenneNom?: string | null;
  commentaire?: string | null;
}
