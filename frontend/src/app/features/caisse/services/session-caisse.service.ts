import { Injectable } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { SKIP_FORBIDDEN_REDIRECT } from '../../../core/interceptors/auth.interceptor';
import { SessionCaisseOpenRequest } from '../models/session-caisse-open-request';
import { SessionCaisseCloseRequest } from '../models/session-caisse-close-request';
import { SessionCaisseResponse } from '../models/session-caisse-response';
import { SessionCaisseValidationRequest } from '../models/session-caisse-validation-request';
import { SessionCaisseOpeningContextResponse } from '../models/session-caisse-opening-context-response';
import { DemanderAnnulationSessionRequest } from '../models/demander-annulation-session-request';
import { ReouvrirSessionControleeRequest } from '../models/reouvrir-session-controlee-request';
import { SessionCaisseAnomalieResponse } from '../models/session-caisse-anomalie-response';
import { ValiderAnnulationSessionRequest } from '../models/valider-annulation-session-request';

@Injectable({
  providedIn: 'root'
})
export class SessionCaisseService {
  private readonly apiUrl = `${API_BASE_URL}/sessions-caisse`;
  private readonly workflowApiUrl = `${API_BASE_URL}/caisses/sessions`;

  constructor(private http: HttpClient) {}

  ouvrir(request: SessionCaisseOpenRequest): Observable<SessionCaisseResponse> {
    return this.http.post<SessionCaisseResponse>(`${this.apiUrl}/ouverture`, request);
  }

  getOuvertureContext(caisseId: number, dateComptable?: string): Observable<SessionCaisseOpeningContextResponse> {
    const params: Record<string, string> = { caisseId: `${caisseId}` };
    if (dateComptable) {
      params['dateComptable'] = dateComptable;
    }
    return this.http.get<SessionCaisseOpeningContextResponse>(`${this.apiUrl}/ouverture-context`, { params });
  }

  preCloturer(id: number, request: SessionCaisseCloseRequest): Observable<SessionCaisseResponse> {
    return this.http.post<SessionCaisseResponse>(`${this.workflowApiUrl}/${id}/pre-cloturer`, {
      soldePhysique: request.soldePhysique,
      observation: request.observation
    });
  }

  cloturer(id: number, request: SessionCaisseCloseRequest): Observable<SessionCaisseResponse> {
    // Alias backward-compatible: /cloturer réalise une pré-clôture.
    return this.preCloturer(id, request);
  }

  getById(id: number): Observable<SessionCaisseResponse> {
    return this.http.get<SessionCaisseResponse>(`${this.workflowApiUrl}/${id}`);
  }

  getAll(): Observable<SessionCaisseResponse[]> {
    return this.http.get<SessionCaisseResponse[]>(this.apiUrl);
  }

  getSessionActive(): Observable<SessionCaisseResponse> {
    return this.http.get<SessionCaisseResponse>(`${this.apiUrl}/active`);
  }

  validerControle(id: number, request: SessionCaisseValidationRequest = {}): Observable<SessionCaisseResponse> {
    return this.http.post<SessionCaisseResponse>(`${this.workflowApiUrl}/${id}/valider-controle`, request);
  }

  cloturerFinale(id: number, request: SessionCaisseValidationRequest = {}): Observable<SessionCaisseResponse> {
    return this.http.post<SessionCaisseResponse>(`${this.workflowApiUrl}/${id}/cloturer-finale`, request);
  }

  demanderAnnulation(id: number, request: DemanderAnnulationSessionRequest): Observable<SessionCaisseAnomalieResponse> {
    return this.http.post<SessionCaisseAnomalieResponse>(`${this.workflowApiUrl}/${id}/demander-annulation`, request);
  }

  validerAnnulation(id: number, request: ValiderAnnulationSessionRequest): Observable<SessionCaisseAnomalieResponse> {
    return this.http.post<SessionCaisseAnomalieResponse>(`${this.workflowApiUrl}/${id}/valider-annulation`, request);
  }

  annulerAdministrativement(id: number, request: DemanderAnnulationSessionRequest): Observable<SessionCaisseResponse> {
    return this.http.post<SessionCaisseResponse>(`${this.workflowApiUrl}/${id}/annuler-administrativement`, request);
  }

  reouvrirControlee(id: number, request: ReouvrirSessionControleeRequest): Observable<SessionCaisseResponse> {
    return this.http.post<SessionCaisseResponse>(`${this.workflowApiUrl}/${id}/reouvrir-controlee`, request);
  }

  getAnomaliesBySession(id: number): Observable<SessionCaisseAnomalieResponse[]> {
    return this.http.get<SessionCaisseAnomalieResponse[]>(`${this.workflowApiUrl}/${id}/anomalies`, {
      context: new HttpContext().set(SKIP_FORBIDDEN_REDIRECT, true)
    });
  }

  getAnomaliesGlobales(): Observable<SessionCaisseAnomalieResponse[]> {
    return this.http.get<SessionCaisseAnomalieResponse[]>(`${this.workflowApiUrl}/anomalies`);
  }
}