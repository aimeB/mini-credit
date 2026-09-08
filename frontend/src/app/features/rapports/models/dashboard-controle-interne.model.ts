export interface ActionDashboardResponse {
  code: string;
  libelle: string;
  route: string;
}

export interface AlerteDashboardResponse {
  code?: string;
  type: string;
  module?: string;
  niveau: string;
  titre: string;
  message: string;
  nombre: number;
  routeFrontend?: string;
  actionUrl?: string;
  referenceId?: number;
  referenceType?: string;
  date?: string;
  action?: ActionDashboardResponse;
}

export interface ResumeCaisse {
  sessionsOuvertes: number;
  sessionsFermeesNonValidees: number;
  ecartsDetectes: number;
  ecartsEnInvestigation: number;
  ecartsNonResolus: number;
}

export interface ResumeDepenses {
  brouillon: number;
  enAttenteValidation: number;
  valideesNonPayees: number;
  payees: number;
  rejetees: number;
}

export interface ResumeRecettes {
  brouillon: number;
  soumises: number;
  validees: number;
  rejetees: number;
}

export interface ResumeRetraits {
  creees: number;
  valides: number;
  rejetees: number;
  decaissees: number;
}

export interface ResumeCredits {
  approuves: number;
  enCours: number;
  enRetard: number;
  contentieux: number;
  clotures: number;
}

export interface ResumeAudit {
  totalEvenements: number;
  infos: number;
  warnings: number;
  critiques: number;
}

export interface DashboardControleInterneResponse {
  dateDebut: string;
  dateFin: string;
  siteId?: number;
  caisseId?: number;
  caisse: ResumeCaisse;
  depenses: ResumeDepenses;
  recettes: ResumeRecettes;
  retraits: ResumeRetraits;
  credits: ResumeCredits;
  audit: ResumeAudit;
  alertes: AlerteDashboardResponse[];
}

export interface DashboardControleInterneFilters {
  date?: string;
  dateDebut?: string;
  dateFin?: string;
  siteId?: number;
  caisseId?: number;
}
