import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';

import {
  RecetteTerrainResponse,
  CreateRecetteTerrainRequest,
  UpdateRecetteTerrainRequest,
  ValidateRecetteTerrainRequest,
  RecetteStatut
} from '../models';

/**
 * Page Response DTO from Spring Data
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  last: boolean;
  first: boolean;
  empty: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class RecetteTerrainService {

  private http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/recettes-terrain`;

  // === CRUD ===

  getAll(): Observable<RecetteTerrainResponse[]> {
    return this.http.get<RecetteTerrainResponse[]>(this.baseUrl);
  }

  getById(id: number): Observable<RecetteTerrainResponse> {
    return this.http.get<RecetteTerrainResponse>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateRecetteTerrainRequest): Observable<RecetteTerrainResponse> {
    return this.http.post<RecetteTerrainResponse>(this.baseUrl, request);
  }

  update(id: number, request: UpdateRecetteTerrainRequest): Observable<RecetteTerrainResponse> {
    return this.http.put<RecetteTerrainResponse>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  // === Recherches ===

  getByAgentTerrain(agentId: number): Observable<RecetteTerrainResponse[]> {
    return this.http.get<RecetteTerrainResponse[]>(`${this.baseUrl}/agent/${agentId}`);
  }

  getMine(): Observable<RecetteTerrainResponse[]> {
    return this.http.get<RecetteTerrainResponse[]>(`${this.baseUrl}/me`);
  }

  getBySite(siteId: number): Observable<RecetteTerrainResponse[]> {
    return this.http.get<RecetteTerrainResponse[]>(`${this.baseUrl}/site/${siteId}`);
  }

  getByStatut(statut: RecetteStatut): Observable<RecetteTerrainResponse[]> {
    return this.http.get<RecetteTerrainResponse[]>(`${this.baseUrl}/statut/${statut}`);
  }

  // === Workflow ===

  soumettre(id: number): Observable<RecetteTerrainResponse> {
    return this.http.post<RecetteTerrainResponse>(`${this.baseUrl}/${id}/soumettre`, {});
  }

  valider(id: number, request: ValidateRecetteTerrainRequest): Observable<RecetteTerrainResponse> {
    return this.http.post<RecetteTerrainResponse>(`${this.baseUrl}/${id}/valider`, request);
  }

  rejeter(id: number, motif: string, validePar: number): Observable<RecetteTerrainResponse> {
    return this.http.post<RecetteTerrainResponse>(
      `${this.baseUrl}/${id}/rejeter`,
      {},
      { params: { motif, validePar: validePar.toString() } }
    );
  }

  // === Métiers ===

  calculerEcarts(id: number): Observable<{ excedent: number; manquant: number; isBalanced: boolean }> {
    return this.http.get<{ excedent: number; manquant: number; isBalanced: boolean }>(
      `${this.baseUrl}/${id}/ecarts`
    );
  }

  getTotalCollecte(agentId: number, dateRecette: string): Observable<number> {
    return this.http.get<number>(
      `${this.baseUrl}/agent/${agentId}/date/${dateRecette}/total`
    );
  }

  /**
   * PHASE 6B.2: Déclenche manuellement la génération des opérations pour une recette
   */
  generateOperations(id: number): Observable<RecetteTerrainResponse> {
    return this.http.post<RecetteTerrainResponse>(
      `${this.baseUrl}/${id}/generate-operations`,
      {}
    );
  }

  /**
   * Récupère les recettes paginées avec filtres optionnels
   * @param page page number (0-indexed)
   * @param pageSize items per page
   * @param sortBy sort field (default: dateRecette)
   * @param sortDir sort direction (asc, desc)
   * @param statut optional status filter
   * @param siteId optional site filter
   * @param agentTerrainId optional agent filter
   * @param dateDebut optional start date filter
   * @param dateFin optional end date filter
   */
  searchAndFilterPaginated(
    page: number = 0,
    pageSize: number = 10,
    sortBy: string = 'dateRecette',
    sortDir: 'asc' | 'desc' = 'desc',
    statut?: string,
    siteId?: number,
    agentTerrainId?: number,
    dateDebut?: string,
    dateFin?: string
  ): Observable<PageResponse<RecetteTerrainResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString())
      .set('sort', `${sortBy},${sortDir}`);

    if (statut) params = params.set('statut', statut);
    if (siteId) params = params.set('siteId', siteId.toString());
    if (agentTerrainId) params = params.set('agentTerrainId', agentTerrainId.toString());
    if (dateDebut) params = params.set('dateDebut', dateDebut);
    if (dateFin) params = params.set('dateFin', dateFin);

    return this.http.get<PageResponse<RecetteTerrainResponse>>(
      `${this.baseUrl}/paginated`,
      { params }
    );
  }
}
