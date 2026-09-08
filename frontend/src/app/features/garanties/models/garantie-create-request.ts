import { TypeGarantie } from './garantie.enum';

export interface GarantieCreateRequest {
  creditId: number;
  typeGarantie: TypeGarantie;
  description: string;
  valeurEstimee: number;
  taux: number;
  localisation?: string;
  notes?: string;
}
