import { Injectable } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { SKIP_FORBIDDEN_REDIRECT } from '../../../core/interceptors/auth.interceptor';
import { Page } from '../../../shared/models/page.model';
import { SessionAuditEvent, SessionAuditLogRaw, SessionAuditTimelinePage } from '../models/session-audit.model';

@Injectable({
  providedIn: 'root'
})
export class SessionAuditService {
  private readonly auditApiUrl = `${API_BASE_URL}/audit/logs`;

  constructor(private http: HttpClient) {}

  getSessionTimeline(sessionId: number, page: number = 0, size: number = 50): Observable<SessionAuditTimelinePage> {
    const normalizedPage = Math.max(0, page);
    const normalizedSize = Math.min(Math.max(1, size), 100);

    return this.http
      .get<Page<SessionAuditLogRaw>>(
        `${this.auditApiUrl}/entity/SessionCaisse/${sessionId}?page=${normalizedPage}&size=${normalizedSize}`,
        { context: new HttpContext().set(SKIP_FORBIDDEN_REDIRECT, true) }
      )
      .pipe(
        map((response) => {
          const events = (response?.content ?? [])
            .map((entry) => this.toEvent(entry))
            .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());

          return {
            events,
            page: response?.currentPage ?? normalizedPage,
            size: response?.pageSize ?? normalizedSize,
            hasNext: response?.hasNext ?? false,
            totalElements: response?.totalElements ?? events.length
          };
        })
      );
  }

  private toEvent(entry: SessionAuditLogRaw): SessionAuditEvent {
    const oldValues = this.parseJson(entry.oldValue || entry.oldValuesJson);
    const newValues = this.parseJson(entry.newValue || entry.newValuesJson);

    const observation = this.pickText(
      newValues?.['observation'],
      oldValues?.['observation'],
      entry.reason
    );

    const motif = this.pickText(
      newValues?.['motif'],
      oldValues?.['motif'],
      entry.reason,
      entry.errorMessage
    );

    return {
      id: entry.id,
      date: entry.dateAction || entry.createdDate || entry.dateCreation || new Date().toISOString(),
      action: entry.action,
      actionLabel: this.actionLabel(entry.action),
      utilisateur: entry.username || 'SYSTEM',
      role: entry.userRole || entry.roleCode || 'INCONNU',
      observation,
      motif,
      ancienStatut: this.pickText(oldValues?.['statut'], oldValues?.['ancienStatut']),
      nouveauStatut: this.pickText(newValues?.['statut'], newValues?.['nouveauStatut']),
      success: entry.success !== false
    };
  }

  private actionLabel(action: string): string {
    const labels: Record<string, string> = {
      OUVERTURE_SESSION: 'Session ouverte',
      PRE_CLOTURE: 'Pré-clôture effectuée',
      VALIDATION_CONTROLE: 'Validation contrôle',
      CLOTURE_FINALE: 'Clôture finale',
      REFUS_TRANSITION: 'Transition refusée',
      MODIFICATION_OPERATION: 'Modification opération',
      ANNULATION_OPERATION: 'Annulation opération',
      SUPPRESSION_REFUSEE: 'Suppression refusée'
    };

    return labels[action] || action;
  }

  private parseJson(value?: string): Record<string, any> | null {
    if (!value) {
      return null;
    }

    try {
      return JSON.parse(value);
    } catch {
      return null;
    }
  }

  private pickText(...values: Array<string | null | undefined>): string | undefined {
    for (const value of values) {
      if (value && value.trim().length > 0) {
        return value.trim();
      }
    }

    return undefined;
  }
}
