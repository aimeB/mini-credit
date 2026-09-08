export interface AgenceResponse {
  id: number;
  codeAgence: string;
  nomAgence: string;
  ville?: string;
  commune?: string;
  quartier?: string;
  reference?: string;
  adresse?: string;
  telephone?: string;
  actif: boolean;
  description?: string;
  chefBureauId?: number;
  dateCreation?: string;
  dateModification?: string;
  // email conservé côté backend (nullable) mais non affiché
}
