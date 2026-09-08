export interface DepenseCaisseBeneficiaireSalaire {
  id: number;
  employeId?: number;
  matricule?: string;
  nom: string;
  role: string;
  agenceId?: number;
  agenceNom?: string;
  salaireBase?: number;
  primeFixe?: number;
  bonusVariable?: number;
  totalRemuneration?: number;
  affichage: string;
}
