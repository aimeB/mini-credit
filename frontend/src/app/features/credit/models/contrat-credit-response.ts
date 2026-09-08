export interface ContratCreditResponse {
  id: number;
  creditId: number;
  numeroContrat: string;
  dateSignature: string;
  lieuSignature: string;
  objetContrat?: string | null;
  clausesSpecifiques?: string | null;
  fichierUrl?: string | null;
  signeParMembre?: boolean | null;
  signeParInstitution?: boolean | null;
  nomSignataireInstitution?: string | null;
  fonctionSignataireInstitution?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}