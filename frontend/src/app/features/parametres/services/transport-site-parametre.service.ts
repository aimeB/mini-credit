import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { SKIP_FORBIDDEN_REDIRECT } from '../../../core/interceptors/auth.interceptor';
import { TransportSiteParametreRequest, TransportSiteParametreResponse } from '../models/transport-site-parametre.model';

@Injectable({ providedIn: 'root' })
export class TransportSiteParametreService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/admin/transport-site-parametres`;

  getAll(skipForbiddenRedirect = false): Observable<TransportSiteParametreResponse[]> {
    const context = skipForbiddenRedirect
      ? new HttpContext().set(SKIP_FORBIDDEN_REDIRECT, true)
      : undefined;
    return this.http.get<TransportSiteParametreResponse[]>(this.apiUrl, context ? { context } : {});
  }

  save(request: TransportSiteParametreRequest): Observable<TransportSiteParametreResponse> {
    return this.http.post<TransportSiteParametreResponse>(this.apiUrl, request);
  }

  deactivate(id: number, commentaire: string): Observable<TransportSiteParametreResponse> {
    const params = new HttpParams().set('commentaire', commentaire);
    return this.http.post<TransportSiteParametreResponse>(`${this.apiUrl}/${id}/desactiver`, null, { params });
  }
}
