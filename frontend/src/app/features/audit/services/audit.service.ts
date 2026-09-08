import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { AuditLog, AuditLogFilters, AuditLogPage, AuditStats } from '../models/audit-log.model';

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  private readonly apiUrl = `${API_BASE_URL}/audit/logs`;

  constructor(private http: HttpClient) {}

  getLogs(filters: AuditLogFilters, page = 0, size = 50): Observable<AuditLogPage> {
    let params = this.toHttpParams(filters)
      .set('page', `${page}`)
      .set('size', `${size}`);

    return this.http.get<AuditLogPage>(this.apiUrl, { params });
  }

  getById(id: number): Observable<AuditLog> {
    return this.http.get<AuditLog>(`${this.apiUrl}/${id}`);
  }

  getByEntity(entityType: string, entityId: number, page = 0, size = 50): Observable<AuditLogPage> {
    return this.http.get<AuditLogPage>(`${this.apiUrl}/entity/${entityType}/${entityId}`, {
      params: new HttpParams().set('page', `${page}`).set('size', `${size}`)
    });
  }

  getByUser(userId: number, page = 0, size = 50): Observable<AuditLogPage> {
    return this.http.get<AuditLogPage>(`${this.apiUrl}/user/${userId}`, {
      params: new HttpParams().set('page', `${page}`).set('size', `${size}`)
    });
  }

  getBySessionCaisse(sessionCaisseId: number, page = 0, size = 50): Observable<AuditLogPage> {
    return this.http.get<AuditLogPage>(`${this.apiUrl}/session-caisse/${sessionCaisseId}`, {
      params: new HttpParams().set('page', `${page}`).set('size', `${size}`)
    });
  }

  getStats(filters: Pick<AuditLogFilters, 'dateDebut' | 'dateFin' | 'module'>): Observable<AuditStats> {
    const params = this.toHttpParams(filters);
    return this.http.get<AuditStats>(`${this.apiUrl}/stats`, { params });
  }

  exportCsv(filters: AuditLogFilters): Observable<Blob> {
    const params = this.toHttpParams(filters);
    return this.http.get(`${this.apiUrl}/export`, { params, responseType: 'blob' });
  }

  private toHttpParams(filters: Partial<AuditLogFilters>): HttpParams {
    let params = new HttpParams();

    Object.entries(filters ?? {}).forEach(([key, value]) => {
      if (value !== undefined && value !== null && `${value}`.trim() !== '') {
        params = params.set(key, `${value}`);
      }
    });

    return params;
  }
}
