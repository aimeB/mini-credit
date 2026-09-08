export interface PaiementSalaireResponse {
  id: number;
  employeId: number;
  employeNom: string;
  employeMatricule: string;
  datePaiement: string; // ISO date
  montant: number;
  modePaiement: string;
  statut: string;
  notes: string;
  numeroOperationCaisse: number;
  referenceExterne: string;
  dateCreation: string;
  dateModification: string;
}
