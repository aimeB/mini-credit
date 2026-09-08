import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { AgenceResponse } from '../../organisation/models/agence-response';
import { AgenceCreateRequest } from '../../organisation/models/agence-create-request';
import { SiteResponse } from '../../membres/models/site-response';

export interface AgenceSimple {
  id: number;
  codeAgence: string;
  nomAgence: string;
  ville?: string;
  actif?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AgenceService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/agences`;

  getAll(): Observable<AgenceResponse[]> {
    return this.http.get<AgenceResponse[]>(this.apiUrl);
  }

  getById(id: number): Observable<AgenceResponse> {
    return this.http.get<AgenceResponse>(`${this.apiUrl}/${id}`);
  }

  create(request: AgenceCreateRequest): Observable<AgenceResponse> {
    return this.http.post<AgenceResponse>(this.apiUrl, request);
  }

  update(id: number, request: Partial<AgenceCreateRequest>): Observable<AgenceResponse> {
    return this.http.put<AgenceResponse>(`${this.apiUrl}/${id}`, request);
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getSitesByAgence(agenceId: number): Observable<SiteResponse[]> {
    return this.http.get<SiteResponse[]>(`${API_BASE_URL}/sites/by-agence/${agenceId}`);
  }

  /**
   * Anomalies de personnel : agents terrain dont l'employé ou le gestionnaire
   * n'appartient pas à la même agence que leur site principal.
   */
  getAnomaliesPersonnel(agenceId: number): Observable<any[]> {
    return this.http.get<any[]>(`${API_BASE_URL}/agents-terrain/anomalies/${agenceId}`);
  }
}
