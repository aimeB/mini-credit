import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/services/api.config';
import { ApprobationCreditRequest } from '../models/approbation-credit-request';
import { CreditResponse } from '../models/credit-response';
import { RemboursementRequest } from '../models/remboursement-request';
import { EcheanceCreditResponse } from '../models/echeance-credit-response';
import { DecaissementCreditRequest } from '../models/decaissement-credit-request';
import { Page } from '../../../shared/models/page.model';
import { CreditContratResponse } from '../models/credit-contrat-response';
import { CreditDetailResponse } from '../models/credit-detail-response';
import { CreditEnCoursResponse } from '../models/credit-en-cours-response';
import { CreditRembourseResponse } from '../models/credit-rembourse-response';

@Injectable({
  providedIn: 'root'
})
export class CreditService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/credits`;

  approuverDemande(demandeId: number, request: ApprobationCreditRequest): Observable<CreditResponse> {
    return this.http.post<CreditResponse>(`${this.baseUrl}/demande/${demandeId}/approbation`, request);
  }

  enregistrerRemboursement(creditId: number, request: RemboursementRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${creditId}/remboursements`, request);
  }

  getById(id: number): Observable<CreditResponse> {
    return this.http.get<CreditResponse>(`${this.baseUrl}/${id}`);
  }

  getDetail(id: number): Observable<CreditDetailResponse> {
    return this.http.get<CreditDetailResponse>(`${this.baseUrl}/${id}/detail`);
  }

  getContrat(id: number): Observable<CreditContratResponse> {
    return this.http.get<CreditContratResponse>(`${this.baseUrl}/${id}/contrat`);
  }

  // PHASE 3B: Paginated version
  getAllPaginated(page: number = 0, size: number = 10): Observable<Page<CreditResponse>> {
    return this.http.get<Page<CreditResponse>>(`${this.baseUrl}?page=${page}&size=${size}`);
  }

  // Backward compatibility: keep original method
  getAll(): Observable<CreditResponse[]> {
    return this.http.get<CreditResponse[]>(this.baseUrl);
  }

  getCreditsADecaisser(): Observable<CreditResponse[]> {
    return this.http.get<CreditResponse[]>(`${this.baseUrl}/a-decaisser`);
  }

  getCreditsEnCours(): Observable<CreditEnCoursResponse[]> {
    return this.http.get<CreditEnCoursResponse[]>(`${this.baseUrl}/en-cours`);
  }

  getCreditsRembourses(): Observable<CreditRembourseResponse[]> {
    return this.http.get<CreditRembourseResponse[]>(`${this.baseUrl}/rembourses`);
  }

  getByMembre(membreId: number): Observable<CreditResponse[]> {
    return this.http.get<CreditResponse[]>(`${this.baseUrl}/membre/${membreId}`);
  }

    getEcheancesByCreditId(creditId: number): Observable<EcheanceCreditResponse[]> {
    return this.http.get<EcheanceCreditResponse[]>(`${this.baseUrl}/${creditId}/echeances`);
  }


decaisserCredit(creditId: number, request: DecaissementCreditRequest): Observable<CreditResponse> {
  return this.http.post<CreditResponse>(
    `${this.baseUrl}/${creditId}/decaissement`,
    request
  );
}

appliquerPenalites(creditId: number, dateReference: string): Observable<void> {
  return this.http.post<void>(
    `${API_BASE_URL}/penalites/credit/${creditId}/appliquer`,
    null,
    {
      params: { dateReference }
    }
  );
}
}