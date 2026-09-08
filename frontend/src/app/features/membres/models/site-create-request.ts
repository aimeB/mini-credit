// Modele pour la creation d un Site
// La localisation administrative (ville, commune) vient de l Agence.
// Le Site definit sa propre localisation operationnelle via zone uniquement.

export interface CreateSiteRequest {
  agenceId: number;       // Obligatoire : rattachement a l agence
  codeSite: string;       // Obligatoire : code unique du site
  nomSite: string;        // Obligatoire : nom du site
  zone: string;           // Obligatoire : zone operationnelle terrain
}

export interface UpdateSiteRequest {
  nomSite?: string;
  zone?: string;
  actif?: boolean;
  agenceId?: number;      // Optionnel : pour changer d agence
}