import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { DepenseCaisseCreateRequest } from '../models/depense-caisse-create-request';
import { DepenseCaissePayRequest } from '../models/depense-caisse-pay-request';
import { DepenseCaisseRejectRequest } from '../models/depense-caisse-reject-request';
import { DepenseCaisseRattachementPaieRequest } from '../models/depense-caisse-rattachement-paie-request';
import { DepenseCaisseBeneficiaireSalaire } from '../models/depense-caisse-beneficiaire-salaire';
import { DepenseCaisseResponse } from '../models/depense-caisse-response';
import { DepenseCaisseSubmitRequest } from '../models/depense-caisse-submit-request';
import { DepenseCaisseValidateRequest } from '../models/depense-caisse-validate-request';
import { PaieEmployePreview } from '../models/paie-employe-preview';

@Injectable({
  providedIn: 'root'
})
export class DepenseCaisseService {
  private readonly apiUrl = `${API_BASE_URL}/depenses-caisse`;

  constructor(private http: HttpClient) {}

  creer(request: DepenseCaisseCreateRequest): Observable<DepenseCaisseResponse> {
    return this.http.post<DepenseCaisseResponse>(this.apiUrl, request);
  }

  getAll(filtres: {
    statut?: string;
    caisseId?: number;
    siteId?: number;
    sessionCaisseId?: number;
    dateDebut?: string;
    dateFin?: string;
  } = {}): Observable<DepenseCaisseResponse[]> {
    let params = new HttpParams();
    Object.entries(filtres).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return this.http.get<DepenseCaisseResponse[]>(this.apiUrl, { params });
  }

  getById(id: number): Observable<DepenseCaisseResponse> {
    return this.http.get<DepenseCaisseResponse>(`${this.apiUrl}/${id}`);
  }

  getBeneficiairesSalaire(caisseId: number): Observable<DepenseCaisseBeneficiaireSalaire[]> {
    const params = new HttpParams().set('caisseId', String(caisseId));
    return this.http.get<DepenseCaisseBeneficiaireSalaire[]>(`${this.apiUrl}/beneficiaires-salaire`, { params });
  }

  getPaiePreview(employeId: number, periodePaie: string): Observable<PaieEmployePreview> {
    const params = new HttpParams()
      .set('employeId', String(employeId))
      .set('periodePaie', periodePaie);
    return this.http.get<PaieEmployePreview>(`${this.apiUrl}/paie-preview`, { params });
  }

  soumettre(id: number, request: DepenseCaisseSubmitRequest = {}): Observable<DepenseCaisseResponse> {
    return this.http.post<DepenseCaisseResponse>(`${this.apiUrl}/${id}/soumettre`, request);
  }

  valider(id: number, request: DepenseCaisseValidateRequest = {}): Observable<DepenseCaisseResponse> {
    return this.http.post<DepenseCaisseResponse>(`${this.apiUrl}/${id}/valider`, request);
  }

  rejeter(id: number, request: DepenseCaisseRejectRequest): Observable<DepenseCaisseResponse> {
    return this.http.post<DepenseCaisseResponse>(`${this.apiUrl}/${id}/rejeter`, request);
  }

  payer(id: number, request: DepenseCaissePayRequest = {}): Observable<DepenseCaisseResponse> {
    return this.http.post<DepenseCaisseResponse>(`${this.apiUrl}/${id}/payer`, request);
  }

  rattacherPaie(id: number, request: DepenseCaisseRattachementPaieRequest): Observable<DepenseCaisseResponse> {
    return this.http.patch<DepenseCaisseResponse>(`${this.apiUrl}/${id}/rattachement-paie`, request);
  }

  annuler(id: number, commentaire?: string): Observable<DepenseCaisseResponse> {
    const params = commentaire ? new HttpParams().set('commentaire', commentaire) : undefined;
    return this.http.post<DepenseCaisseResponse>(`${this.apiUrl}/${id}/annuler`, {}, params ? { params } : {});
  }
}