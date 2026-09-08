import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { GarantieResponse } from '../models/garantie-response';
import { GarantieCreateRequest } from '../models/garantie-create-request';

@Injectable({
  providedIn: 'root'
})
export class GarantieService {
  private readonly apiUrl = `${API_BASE_URL}/garanties`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<GarantieResponse[]> {
    return this.http.get<GarantieResponse[]>(this.apiUrl);
  }

  getById(id: number): Observable<GarantieResponse> {
    return this.http.get<GarantieResponse>(`${this.apiUrl}/${id}`);
  }

  getByCreditId(creditId: number): Observable<GarantieResponse[]> {
    return this.http.get<GarantieResponse[]>(`${this.apiUrl}/credit/${creditId}`);
  }

  getByMembreId(membreId: number): Observable<GarantieResponse[]> {
    return this.http.get<GarantieResponse[]>(`${this.apiUrl}/membre/${membreId}`);
  }

  getByDemandeCreditId(demandeCreditId: number): Observable<GarantieResponse[]> {
    return this.http.get<GarantieResponse[]>(`${this.apiUrl}/demande/${demandeCreditId}`);
  }

  getByType(type: string): Observable<GarantieResponse[]> {
    return this.http.get<GarantieResponse[]>(`${this.apiUrl}/type/${type}`);
  }

  getByStatut(statut: string): Observable<GarantieResponse[]> {
    return this.http.get<GarantieResponse[]>(`${this.apiUrl}/statut/${statut}`);
  }

  getByDateRange(debut: string, fin: string): Observable<GarantieResponse[]> {
    const params = new HttpParams()
      .set('debut', debut)
      .set('fin', fin);
    return this.http.get<GarantieResponse[]>(`${this.apiUrl}/date-range`, { params });
  }

  create(request: GarantieCreateRequest): Observable<GarantieResponse> {
    return this.http.post<GarantieResponse>(this.apiUrl, request);
  }

  update(id: number, request: GarantieCreateRequest): Observable<GarantieResponse> {
    return this.http.put<GarantieResponse>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
