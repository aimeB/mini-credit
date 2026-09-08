import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/services/api.config';
import { DemandeCreditCreateRequest } from '../models/demande-credit-create-request';
import { DemandeCreditResponse } from '../models/demande-credit-response';
import { FraisCreditAEncaisserResponse } from '../models/frais-credit-a-encaisser-response';
import { AnalyseRisqueRequest } from '../models/analyse-risque-request';
import { PreAnalyseRequest } from '../models/pre-analyse-request';
import { Page } from '../../../shared/models/page.model';

@Injectable({
  providedIn: 'root'
})
export class DemandeCreditService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/demandes-credit`;

  create(request: DemandeCreditCreateRequest): Observable<DemandeCreditResponse> {
    return this.http.post<DemandeCreditResponse>(this.baseUrl, request);
  }

  ajouterAnalyse(demandeId: number, request: AnalyseRisqueRequest): Observable<DemandeCreditResponse> {
    return this.http.post<DemandeCreditResponse>(`${this.baseUrl}/${demandeId}/analyse`, request);
  }

  preAnalyser(demandeId: number, commentaire?: string): Observable<DemandeCreditResponse> {
    const params = commentaire ? new HttpParams().set('commentaire', commentaire) : undefined;
    return this.http.post<DemandeCreditResponse>(`${this.baseUrl}/${demandeId}/pre-analyse`, {}, { params });
  }

  preAnalyserDecision(demandeId: number, request: PreAnalyseRequest): Observable<DemandeCreditResponse> {
    return this.http.post<DemandeCreditResponse>(`${this.baseUrl}/${demandeId}/pre-analyse/decision`, request);
  }

  controlerRisque(demandeId: number, commentaire?: string): Observable<DemandeCreditResponse> {
    return this.validerAnalyseRisque(demandeId, commentaire);
  }

  enregistrerObservationRisque(demandeId: number, commentaire: string): Observable<DemandeCreditResponse> {
    const params = new HttpParams().set('commentaire', commentaire);
    return this.http.post<DemandeCreditResponse>(`${this.baseUrl}/${demandeId}/analyse-risque/observation`, {}, { params });
  }

  validerAnalyseRisque(demandeId: number, commentaire?: string): Observable<DemandeCreditResponse> {
    const params = commentaire ? new HttpParams().set('commentaire', commentaire) : undefined;
    return this.http.post<DemandeCreditResponse>(`${this.baseUrl}/${demandeId}/analyse-risque/valider`, {}, { params });
  }

  controlerGarantie(demandeId: number, commentaire?: string): Observable<DemandeCreditResponse> {
    const params = commentaire ? new HttpParams().set('commentaire', commentaire) : undefined;
    return this.http.post<DemandeCreditResponse>(`${this.baseUrl}/${demandeId}/controle-garantie`, {}, { params });
  }

  rejeter(demandeId: number, commentaire: string): Observable<DemandeCreditResponse> {
    const params = new HttpParams().set('commentaire', commentaire);
    return this.http.post<DemandeCreditResponse>(`${this.baseUrl}/${demandeId}/rejeter`, {}, { params });
  }

  getById(id: number): Observable<DemandeCreditResponse> {
    return this.http.get<DemandeCreditResponse>(`${this.baseUrl}/${id}`);
  }

  /**
   * Get all credit requests with pagination support
   * @param page Page number (default 0)
   * @param size Page size (default 10)
   */
  getAll(page: number = 0, size: number = 10): Observable<Page<DemandeCreditResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<Page<DemandeCreditResponse>>(this.baseUrl, { params });
  }

  getFraisCreditAEncaisser(): Observable<FraisCreditAEncaisserResponse[]> {
    return this.http.get<FraisCreditAEncaisserResponse[]>(`${this.baseUrl}/frais-a-encaisser`);
  }

  // PHASE 3B: Paginated version
  getByMembrePaginated(membreId: number, page: number = 0, size: number = 10): Observable<Page<DemandeCreditResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<Page<DemandeCreditResponse>>(`${this.baseUrl}/membre/${membreId}`, { params });
  }

  // Backward compatibility: keep original method
  getByMembre(membreId: number): Observable<DemandeCreditResponse[]> {
    return this.http.get<DemandeCreditResponse[]>(`${this.baseUrl}/membre/${membreId}`);
  }

  /**
   * Get current member's credit requests
   */
  getMesDemandes(): Observable<DemandeCreditResponse[]> {
    return this.http.get<DemandeCreditResponse[]>(`${this.baseUrl}/mes-demandes`);
  }
}