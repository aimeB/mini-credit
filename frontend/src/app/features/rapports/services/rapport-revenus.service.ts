import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { RapportRevenusFilters, RapportRevenusResponse } from '../models/rapport-revenus.model';
import { NatureFinancementApprovisionnement } from '../../caisse/models/nature-financement-approvisionnement';
import { DepenseCaisseBeneficiaireSalaire } from '../../caisse/models/depense-caisse-beneficiaire-salaire';
import { DepenseCaisseRattachementPaieRequest } from '../../caisse/models/depense-caisse-rattachement-paie-request';
import { DepenseCaisseRattachementTransportRequest } from '../../caisse/models/depense-caisse-rattachement-transport-request';
import { PaieEmployePreview } from '../../caisse/models/paie-employe-preview';

export interface RequalificationNatureFinancementRequest {
  natureFinancement: NatureFinancementApprovisionnement;
  commentaireCorrection: string;
}

@Injectable({ providedIn: 'root' })
export class RapportRevenusService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/rapports/revenus`;

  getRapport(filters: RapportRevenusFilters): Observable<RapportRevenusResponse> {
    let params = new HttpParams()
      .set('dateDebut', filters.dateDebut)
      .set('dateFin', filters.dateFin);

    if (filters.agenceId) params = params.set('agenceId', String(filters.agenceId));
    if (filters.categorie) params = params.set('categorie', filters.categorie);
    if (filters.source) params = params.set('source', filters.source);

    return this.http.get<RapportRevenusResponse>(this.apiUrl, { params });
  }

  requalifierNatureFinancement(operationId: number, request: RequalificationNatureFinancementRequest): Observable<unknown> {
    return this.http.patch(`${API_BASE_URL}/operations-caisse/${operationId}/nature-financement`, request);
  }

  getBeneficiairesSalaire(caisseId: number): Observable<DepenseCaisseBeneficiaireSalaire[]> {
    const params = new HttpParams().set('caisseId', String(caisseId));
    return this.http.get<DepenseCaisseBeneficiaireSalaire[]>(`${API_BASE_URL}/depenses-caisse/beneficiaires-salaire`, { params });
  }

  getPaiePreview(employeId: number, periodePaie: string): Observable<PaieEmployePreview> {
    const params = new HttpParams()
      .set('employeId', String(employeId))
      .set('periodePaie', periodePaie);
    return this.http.get<PaieEmployePreview>(`${API_BASE_URL}/depenses-caisse/paie-preview`, { params });
  }

  rattacherPaie(depenseId: number, request: DepenseCaisseRattachementPaieRequest): Observable<unknown> {
    return this.http.patch(`${API_BASE_URL}/depenses-caisse/${depenseId}/rattachement-paie`, request);
  }

  rattacherTransport(depenseId: number, request: DepenseCaisseRattachementTransportRequest): Observable<unknown> {
    return this.http.patch(`${API_BASE_URL}/depenses-caisse/${depenseId}/rattachement-transport`, request);
  }
}
