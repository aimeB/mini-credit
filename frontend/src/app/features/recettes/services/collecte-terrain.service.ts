import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import {
  CollecteTerrainResponse,
  CollecteMembreLigneResponse,
  CollecteRecapResponse,
  CreateCollecteMembreLigneRequest,
  ConfirmerBilletageRequest,
  CreateCollecteTerrainRequest,
  UpdateCollecteMembreLigneRequest,
  ValidateCollecteTerrainRequest,
} from '../models/collecte-terrain.model';
import { Page } from '../../../shared/models/page.model';

@Injectable({ providedIn: 'root' })
export class CollecteTerrainService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/collectes-terrain`;

  getToday(): Observable<CollecteTerrainResponse | null> {
    return this.http.get<CollecteTerrainResponse | null>(`${this.baseUrl}/today`);
  }

  getById(id: number): Observable<CollecteTerrainResponse> {
    return this.http.get<CollecteTerrainResponse>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateCollecteTerrainRequest = {}): Observable<CollecteTerrainResponse> {
    return this.http.post<CollecteTerrainResponse>(this.baseUrl, request);
  }

  addLigne(id: number, request: CreateCollecteMembreLigneRequest): Observable<CollecteMembreLigneResponse> {
    return this.http.post<CollecteMembreLigneResponse>(`${this.baseUrl}/${id}/lignes`, request);
  }

  updateLigne(id: number, ligneId: number, request: UpdateCollecteMembreLigneRequest): Observable<CollecteMembreLigneResponse> {
    return this.http.put<CollecteMembreLigneResponse>(`${this.baseUrl}/${id}/lignes/${ligneId}`, request);
  }

  deleteLigne(id: number, ligneId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/lignes/${ligneId}`);
  }

  soumettre(id: number, request: CreateCollecteTerrainRequest = {}): Observable<CollecteTerrainResponse> {
    return this.http.post<CollecteTerrainResponse>(`${this.baseUrl}/${id}/soumettre`, request);
  }

  confirmerBilletage(id: number, request: ConfirmerBilletageRequest): Observable<CollecteTerrainResponse> {
    return this.http.post<CollecteTerrainResponse>(`${this.baseUrl}/${id}/billetage/confirmer`, request);
  }

  valider(id: number, request: ValidateCollecteTerrainRequest): Observable<CollecteTerrainResponse> {
    return this.http.post<CollecteTerrainResponse>(`${this.baseUrl}/${id}/valider`, request);
  }

  rejeter(id: number, request: ValidateCollecteTerrainRequest): Observable<CollecteTerrainResponse> {
    return this.http.post<CollecteTerrainResponse>(`${this.baseUrl}/${id}/rejeter`, request);
  }

  recap(id: number): Observable<CollecteRecapResponse> {
    return this.http.get<CollecteRecapResponse>(`${this.baseUrl}/${id}/recap`);
  }

  list(params: {
    statut?: string;
    dateDebut?: string;
    dateFin?: string;
    agentId?: number;
    siteId?: number;
    antenneId?: number;
    page?: number;
    size?: number;
  }): Observable<Page<CollecteTerrainResponse>> {
    const query = new URLSearchParams();
    if (params.statut) query.set('statut', params.statut);
    if (params.dateDebut) query.set('dateDebut', params.dateDebut);
    if (params.dateFin) query.set('dateFin', params.dateFin);
    if (params.agentId !== undefined) query.set('agentId', String(params.agentId));
    if (params.siteId !== undefined) query.set('siteId', String(params.siteId));
    if (params.antenneId !== undefined) query.set('antenneId', String(params.antenneId));
    query.set('page', String(params.page ?? 0));
    query.set('size', String(params.size ?? 20));
    return this.http.get<Page<CollecteTerrainResponse>>(`${this.baseUrl}?${query.toString()}`);
  }
}
