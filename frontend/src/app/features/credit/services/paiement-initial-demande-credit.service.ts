import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';

import { PaiementInitialDemandeCreditRequest } from '../models/paiement-initial-demande-credit-request';
import { PaiementInitialDemandeCreditResponse } from '../models/paiement-initial-demande-credit-response';

@Injectable({
  providedIn: 'root'
})
export class PaiementInitialDemandeCreditService {
  private readonly apiUrl = `${API_BASE_URL}/demandes-credit`;

  constructor(private http: HttpClient) {}

  enregistrerPaiementInitial(
    demandeId: number,
    request: PaiementInitialDemandeCreditRequest
  ): Observable<PaiementInitialDemandeCreditResponse> {
    return this.http.post<PaiementInitialDemandeCreditResponse>(
      `${this.apiUrl}/${demandeId}/paiement-initial`,
      request
    );
  }
}