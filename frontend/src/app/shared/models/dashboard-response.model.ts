export interface PortefeuilleDashboardResponse {
  totalCredits: number;
  creditsActifs: number;
  creditsRembourses: number;
  creditsEnRetard: number;
  montantTotalOctroye: number;
  encoursPrincipal: number;
  interetsTotaux: number;
  penalitesTotales: number;
}

export interface RetardEcheanceItemResponse {
  creditsId: number;
  membreName: string;
  montantEcheance: number;
  dateEcheance: string;
  joursRetard: number;
  penalite: number;
}

export interface RetardDashboardResponse {
  nombreCreditsEnRetard: number;
  nombreEcheancesEnRetard: number;
  montantTotalEnRetard: number;
  penalitesCumulees: number;
  echeances: RetardEcheanceItemResponse[];
}

export interface CaisseCategorieItemResponse {
  categorie: string;
  montant: number;
  nombre: number;
}

export interface CaisseDashboardResponse {
  sessionCaisseId: number;
  caisseId: number;
  caisseCode: string;
  soldeOuverture: number;
  totalEntrees: number;
  totalSorties: number;
  soldeTheorique: number;
  soldePhysique: number;
  ecartCaisse: number;
  repartitionEntrees: CaisseCategorieItemResponse[];
  repartitionSorties: CaisseCategorieItemResponse[];
}

export interface DashboardGlobalResponse {
  portefeuille: PortefeuilleDashboardResponse;
  retard: RetardDashboardResponse;
  caisse: CaisseDashboardResponse;
}
