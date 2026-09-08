export interface SessionAuditLogRaw {
  id: number;
  action: string;
  entityType: string;
  entityId: number;
  username?: string;
  roleCode?: string;
  userRole?: string;
  userId?: number;
  reason?: string;
  commentaire?: string;
  success?: boolean;
  errorMessage?: string;
  oldValuesJson?: string;
  newValuesJson?: string;
  oldValue?: string;
  newValue?: string;
  dateAction?: string;
  createdDate?: string;
  dateCreation?: string;
}

export interface SessionAuditEvent {
  id: number;
  date: string;
  action: string;
  actionLabel: string;
  utilisateur: string;
  role: string;
  observation?: string;
  motif?: string;
  ancienStatut?: string;
  nouveauStatut?: string;
  success: boolean;
}

export interface SessionAuditTimelinePage {
  events: SessionAuditEvent[];
  page: number;
  size: number;
  hasNext: boolean;
  totalElements: number;
}
