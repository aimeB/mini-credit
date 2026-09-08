import { TypeGarantie, StatutGarantie } from './garantie.enum';

export interface GarantieResponse {
  id: number;
  creditId?: number | null;
  demandeCreditId?: number | null;
  creditNumero?: string | null;
  membreId?: number | null;
  membreNom?: string | null;
  
  typeGarantie: TypeGarantie;
  description: string;
  valeurEstimee: number;
  
  taux?: number | null;
  montantBloque?: number | null;
  localisation?: string;
  
  statut: StatutGarantie;
  dateCreation: string;
  dateModification: string;
  
  notes?: string;
}
