import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/services/api.config';
import {
  AjouterGarantieMaterielleRequest,
  BloquerGarantieEpargneRequest,
  GarantieCreditResponse,
  GarantieMaterielleResponse,
  RejeterGarantieRequest,
  ValiderGarantieRequest,
  VerifierGarantieCreditRequest
} from '../models/garantie-credit.model';

@Injectable({
  providedIn: 'root'
})
export class GarantieCreditService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/demandes-credit`;

  getGarantie(demandeId: number): Observable<GarantieCreditResponse> {
    return this.http.get<GarantieCreditResponse>(`${this.baseUrl}/${demandeId}/garantie`);
  }

  verifierGarantie(demandeId: number, request: VerifierGarantieCreditRequest = {}): Observable<GarantieCreditResponse> {
    return this.http.post<GarantieCreditResponse>(`${this.baseUrl}/${demandeId}/garantie/verifier`, request);
  }

  bloquerEpargne(demandeId: number, request: BloquerGarantieEpargneRequest = {}): Observable<GarantieCreditResponse> {
    return this.http.post<GarantieCreditResponse>(`${this.baseUrl}/${demandeId}/garantie/bloquer-epargne`, request);
  }

  ajouterGarantieMaterielle(
    demandeId: number,
    request: AjouterGarantieMaterielleRequest
  ): Observable<GarantieMaterielleResponse> {
    return this.http.post<GarantieMaterielleResponse>(`${this.baseUrl}/${demandeId}/garantie/materielle`, request);
  }

  validerGarantie(demandeId: number, request: ValiderGarantieRequest): Observable<GarantieCreditResponse> {
    return this.http.post<GarantieCreditResponse>(`${this.baseUrl}/${demandeId}/garantie/valider`, request);
  }

  rejeterGarantie(demandeId: number, request: RejeterGarantieRequest): Observable<GarantieCreditResponse> {
    return this.http.post<GarantieCreditResponse>(`${this.baseUrl}/${demandeId}/garantie/rejeter`, request);
  }

  getGarantiesMaterielles(demandeId: number): Observable<GarantieMaterielleResponse[]> {
    return this.http.get<GarantieMaterielleResponse[]>(`${this.baseUrl}/${demandeId}/garantie/materielles`);
  }

  accepterGarantieMaterielle(
    demandeId: number,
    garantieMaterielleId: number,
    commentaire?: string
  ): Observable<GarantieMaterielleResponse> {
    const suffix = commentaire?.trim()
      ? `?commentaire=${encodeURIComponent(commentaire.trim())}`
      : '';
    return this.http.post<GarantieMaterielleResponse>(
      `${this.baseUrl}/${demandeId}/garantie/materielles/${garantieMaterielleId}/accepter${suffix}`,
      {}
    );
  }

  refuserGarantieMaterielle(
    demandeId: number,
    garantieMaterielleId: number,
    commentaire: string
  ): Observable<GarantieMaterielleResponse> {
    return this.http.post<GarantieMaterielleResponse>(
      `${this.baseUrl}/${demandeId}/garantie/materielles/${garantieMaterielleId}/rejeter?commentaire=${encodeURIComponent(commentaire)}`,
      {}
    );
  }
}
