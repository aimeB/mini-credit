export type PreAnalyseAction = 'TRANSMETTRE_ANALYSE' | 'RETOUR_COMPLEMENT';

export interface PreAnalyseRequest {
  action: PreAnalyseAction;
  commentaire: string;
  dossierComplet?: boolean;
}
