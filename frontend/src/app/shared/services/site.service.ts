import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../core/services/api.config';

export interface SiteResponse {
  id: number;
  codeSite: string;
  nomSite: string;
  zone: string;
  actif: boolean;
  agenceId: number | null;
  nomAgence: string | null;
  villeAgence: string | null;
  communeAgence: string | null;
}

@Injectable({ providedIn: 'root' })
export class SiteService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/sites`;

  getActifs(): Observable<SiteResponse[]> {
    return this.http.get<SiteResponse[]>(`${this.apiUrl}/actifs`);
  }

  getAll(): Observable<SiteResponse[]> {
    return this.http.get<SiteResponse[]>(this.apiUrl);
  }
}
