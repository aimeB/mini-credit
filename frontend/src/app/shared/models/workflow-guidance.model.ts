export type WorkflowModule = 'CREDIT' | 'RECETTE_JOURNALIERE' | 'RETRAIT_EPARGNE' | 'CAISSE' | 'DEPENSE_CAISSE' | 'EPARGNE_OPERATION' | 'DASHBOARD_ROLE';

export type WorkflowSeverity = 'info' | 'success' | 'warning' | 'danger';

export interface WorkflowGuidanceAction {
  label: string;
  visible: boolean;
}

export interface WorkflowGuidance {
  title: string;
  message: string;
  currentStep: string;
  nextStep?: string;
  expectedRole?: string;
  expectedAction?: string;
  severity: WorkflowSeverity;
  canCurrentUserAct: boolean;
  blockedReason?: string;
  successMessage?: string;
  action?: WorkflowGuidanceAction;
}

export interface WorkflowGuidanceContext {
  module: WorkflowModule;
  status: string;
  currentRole?: string;
  permissions?: string[];
  blockingReason?: string;
  nextStep?: string;
  expectedRole?: string;
  metadata?: Record<string, unknown>;
}