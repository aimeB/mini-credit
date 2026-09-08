export interface ValiderAnnulationSessionRequest {
  decision: 'VALIDER' | 'REJETER';
  motifDecision?: string;
  commentaireDecision?: string;
}
