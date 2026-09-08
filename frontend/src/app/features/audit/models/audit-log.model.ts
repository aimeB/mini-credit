import { ActionAudit, AuditModule, SeveriteLog } from './audit.enum';

export interface AuditLog {
  id: number;

  action: ActionAudit;
  module: AuditModule;
  entityType: string;
  entityId?: number;
  userId?: number;
  username: string;
  userRole?: string;
  siteId?: number;
  siteLibelle?: string;
  caisseId?: number;
  sessionCaisseId?: number;
  dateAction: string;
  ipAddress?: string;
  userAgent?: string;
  oldValue?: string;
  newValue?: string;
  commentaire?: string;
  severite: SeveriteLog;
  success: boolean;
  errorMessage?: string;
  referenceMetier?: string;
}

export interface AuditLogPage {
  content: AuditLog[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface AuditStats {
  totalLogs: number;
  failedLogs: number;
  logsParModule: Record<string, number>;
  logsParSeverite: Record<string, number>;
  actionsCritiquesRecentes: AuditLog[];
}

export interface AuditLogFilters {
  dateDebut?: string;
  dateFin?: string;
  module?: string;
  action?: string;
  severity?: string;
  success?: boolean;
  userId?: number;
  siteId?: number;
  caisseId?: number;
  sessionCaisseId?: number;
  entityType?: string;
  entityId?: number;
  referenceMetier?: string;
}
