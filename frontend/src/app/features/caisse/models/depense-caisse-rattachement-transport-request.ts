export interface DepenseCaisseRattachementTransportRequest {
  employeId: number;
  periodeCharge: string;
  dateDebutPeriode?: string;
  dateFinPeriode?: string;
  siteId: number;
  typeChargeFixe: 'TRANSPORT_SITE';
  commentaireCorrection: string;
}
