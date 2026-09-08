export type WorkflowTaskStatus = 'A_FAIRE' | 'EN_COURS' | 'TERMINEE' | 'ANNULEE';

export type WorkflowTaskPriority = 'BASSE' | 'MOYENNE' | 'NORMALE' | 'HAUTE' | 'CRITIQUE';

export type WorkflowTaskModule = 'CAISSE' | 'CREDIT' | 'EPARGNE' | 'RECETTE';

export interface WorkflowTaskItem {
  id: number;
  typeAction: string;
  module: WorkflowTaskModule;
  referenceMetier: string;
  entityType: string;
  entityId: number;
  titre: string;
  description?: string;
  type?: string;
  roleAttendu?: string;
  actionAttendue?: string;
  route?: string;
  roleDestinataire?: string;
  utilisateurDestinataireId?: number;
  antenneId?: number;
  siteId?: number;
  priorite: WorkflowTaskPriority;
  statut: WorkflowTaskStatus;
  dateEcheance?: string;
  dateCreation: string;
  viewedAt?: string;
  completedAt?: string;
  commentaireCompletion?: string;
}

export interface WorkflowTaskCount {
  totalAFaire?: number;
  total?: number;
}

export interface WorkflowTaskDashboard {
  totalAFaire?: number;
  totalEnCours?: number;
  totalTerminee?: number;
  urgentCount?: number;
  overdueCount?: number;
  countAFaire?: number;
  tasks?: WorkflowTaskItem[];
}
