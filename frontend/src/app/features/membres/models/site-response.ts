export interface SiteResponse {
  id: number;
  codeSite: string;
  nomSite: string;
  zone?: string;          // Zone operationnelle du site
  actif?: boolean;
  agenceId?: number;
  nomAgence?: string;     // Denomination de l agence
  villeAgence?: string;   // Ville de l agence (localisation administrative)
  communeAgence?: string; // Commune de l agence (localisation administrative)
}