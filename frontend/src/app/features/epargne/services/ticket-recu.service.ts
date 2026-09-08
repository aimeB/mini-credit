import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import {
  TicketDuplicataRequest,
  TicketPrintRequest,
  TicketRecuResponse,
  TicketVerificationResponse
} from '../models/ticket-recu.model';

@Injectable({ providedIn: 'root' })
export class TicketRecuService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/tickets-recus`;

  getById(id: number): Observable<TicketRecuResponse> {
    return this.http.get<TicketRecuResponse>(`${this.baseUrl}/${id}`);
  }

  getByOperationEpargne(operationEpargneId: number): Observable<TicketRecuResponse[]> {
    return this.http.get<TicketRecuResponse[]>(`${this.baseUrl}/by-operation-epargne/${operationEpargneId}`);
  }

  getByDemandeRetrait(demandeRetraitId: number): Observable<TicketRecuResponse[]> {
    return this.http.get<TicketRecuResponse[]>(`${this.baseUrl}/by-demande-retrait/${demandeRetraitId}`);
  }

  getByMembre(membreId: number): Observable<TicketRecuResponse[]> {
    return this.http.get<TicketRecuResponse[]>(`${this.baseUrl}/by-membre/${membreId}`);
  }

  marquerImpression(id: number, request: TicketPrintRequest = {}): Observable<TicketRecuResponse> {
    return this.http.post<TicketRecuResponse>(`${this.baseUrl}/${id}/impression`, {
      format: request.format ?? 'THERMIQUE_80MM',
      marquerImprime: request.marquerImprime ?? true,
      impressionReussie: request.impressionReussie ?? true,
      commentaire: request.commentaire
    });
  }

  genererDuplicata(id: number, motif: string): Observable<TicketRecuResponse> {
    const request: TicketDuplicataRequest = { motif };
    return this.http.post<TicketRecuResponse>(`${this.baseUrl}/${id}/duplicata`, request);
  }

  getPrintableUrl(id: number, duplicata = false): string {
    return `${this.baseUrl}/${id}/print?duplicata=${duplicata}`;
  }

  getPrintableHtml(id: number, duplicata = false): Observable<string> {
    return this.http.get(`${this.baseUrl}/${id}/print?duplicata=${duplicata}`, { responseType: 'text' });
  }

  verify(codeVerification: string): Observable<TicketVerificationResponse> {
    return this.http.get<TicketVerificationResponse>(`${this.baseUrl}/verify/${encodeURIComponent(codeVerification)}`);
  }
}
