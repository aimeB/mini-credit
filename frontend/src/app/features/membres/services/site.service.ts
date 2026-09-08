import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { SiteResponse } from '../models/site-response';
import { CreateSiteRequest, UpdateSiteRequest } from '../models/site-create-request';
import { AgentTerrainResponse } from '../../../admin/agent-terrain/models/agent-terrain.model';

@Injectable({
  providedIn: 'root'
})
export class SiteService {
  private http = inject(HttpClient);
  private apiUrl = `${API_BASE_URL}/sites`;

  getAll(): Observable<SiteResponse[]> {
    return this.http.get<SiteResponse[]>(this.apiUrl);
  }

  getById(id: number): Observable<SiteResponse> {
    return this.http.get<SiteResponse>(`${this.apiUrl}/${id}`);
  }

  create(request: CreateSiteRequest): Observable<SiteResponse> {
    return this.http.post<SiteResponse>(this.apiUrl, request);
  }

  update(id: number, request: UpdateSiteRequest): Observable<SiteResponse> {
    return this.http.put<SiteResponse>(`${this.apiUrl}/${id}`, request);
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getAgentsTerrainBySite(siteId: number): Observable<AgentTerrainResponse[]> {
    return this.http.get<AgentTerrainResponse[]>(`${API_BASE_URL}/agents-terrain/by-site/${siteId}`);
  }
}