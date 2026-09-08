import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { AgentTerrainResponse } from '../models/agent-terrain-response';

@Injectable({
  providedIn: 'root'
})
export class AgentTerrainService {
  private http = inject(HttpClient);
  private apiUrl = `${API_BASE_URL}/agents-terrain`;

  getAll(): Observable<AgentTerrainResponse[]> {
    return this.http.get<AgentTerrainResponse[]>(this.apiUrl);
  }

  getBySite(siteId: number): Observable<AgentTerrainResponse[]> {
    return this.http.get<AgentTerrainResponse[]>(`${this.apiUrl}/by-site/${siteId}`);
  }
}