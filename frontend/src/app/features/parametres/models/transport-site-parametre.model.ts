export interface TransportSiteParametreResponse {
  id: number;
  siteId: number;
  siteNom?: string | null;
  agenceId?: number | null;
  agenceNom?: string | null;
  montantTransportJournalierParAgent: number;
  actif: boolean;
  dateDebutValidite: string;
  dateFinValidite?: string | null;
  commentaire?: string | null;
  createdBy?: number | null;
  createdAt?: string | null;
  updatedBy?: number | null;
  updatedAt?: string | null;
}

export interface TransportSiteParametreRequest {
  siteId: number;
  montantTransportJournalierParAgent: number;
  actif: boolean;
  dateDebutValidite?: string | null;
  dateFinValidite?: string | null;
  commentaire: string;
}
