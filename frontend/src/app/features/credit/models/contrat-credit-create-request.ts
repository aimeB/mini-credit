export interface ContratCreditCreateRequest {
  creditId: number;
  dateSignature: string;
  lieuSignature: string;
  objetContrat?: string | null;
  clausesSpecifiques?: string | null;
  signeParMembre?: boolean;
  signeParInstitution?: boolean;
  nomSignataireInstitution?: string | null;
  fonctionSignataireInstitution?: string | null;
}