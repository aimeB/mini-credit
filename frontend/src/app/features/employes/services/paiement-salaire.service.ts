import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { PaiementSalaireResponse } from '../models/paiement-salaire-response';
import { CreatePaiementSalaireRequest } from '../models/paiement-salaire-create-request';

@Injectable({
  providedIn: 'root'
})
export class PaiementSalaireService {
  private readonly apiUrl = `${API_BASE_URL}/paiements-salaire`;

  constructor(private http: HttpClient) {}

  create(request: CreatePaiementSalaireRequest): Observable<PaiementSalaireResponse> {
    return this.http.post<PaiementSalaireResponse>(this.apiUrl, request);
  }

  getById(id: number): Observable<PaiementSalaireResponse> {
    return this.http.get<PaiementSalaireResponse>(`${this.apiUrl}/${id}`);
  }

  getByEmployeId(employeId: number): Observable<PaiementSalaireResponse[]> {
    return this.http.get<PaiementSalaireResponse[]>(`${this.apiUrl}/employe/${employeId}`);
  }

  getAll(): Observable<PaiementSalaireResponse[]> {
    return this.http.get<PaiementSalaireResponse[]>(this.apiUrl);
  }
}
