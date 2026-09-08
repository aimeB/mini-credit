export interface CaisseCreateRequest {
  libelle: string;
  agenceId: number;
  siteId?: number;
  caissierResponsableId?: number;
  caissierAffecteId?: number;
  devise: string;
}