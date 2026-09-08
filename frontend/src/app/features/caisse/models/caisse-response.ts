export interface CaisseResponse {
  id: number;
  codeCaisse: string;
  libelle: string;
  agenceId?: number;
  agenceNom?: string;
  siteId?: number;
  siteNom?: string;
  antenneId?: number;
  antenneNom?: string;
  caissierResponsableId?: number;
  caissierResponsableNom?: string;
  caissierAffecteId?: number;
  caissierAffecteNom?: string;
  devise: string;
  actif: boolean;
  soldeTheoriqueSessionOuverte?: number;
  dernierSoldeCloture?: number;
  soldeDisponibleActuel?: number;
  statutSession?: string;
  createdAt: string;
  updatedAt: string;
}