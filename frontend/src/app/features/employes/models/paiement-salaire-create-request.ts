export interface CreatePaiementSalaireRequest {
  employeId: number;
  datePaiement: string; // ISO date
  montant: number;
  modePaiement: string;
  notes?: string;
  referenceExterne?: string;
}
