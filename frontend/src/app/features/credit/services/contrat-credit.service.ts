import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/services/api.config';
import { ContratCreditCreateRequest } from '../models/contrat-credit-create-request';
import { ContratCreditResponse } from '../models/contrat-credit-response';

@Injectable({
  providedIn: 'root'
})
export class ContratCreditService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/contrats-credit`;

  create(request: ContratCreditCreateRequest): Observable<ContratCreditResponse> {
    return this.http.post<ContratCreditResponse>(this.baseUrl, request);
  }

  getById(id: number): Observable<ContratCreditResponse> {
    return this.http.get<ContratCreditResponse>(`${this.baseUrl}/${id}`);
  }

  getByCreditId(creditId: number): Observable<ContratCreditResponse> {
    return this.http.get<ContratCreditResponse>(`${this.baseUrl}/credit/${creditId}`);
  }
}