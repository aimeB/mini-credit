import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { CaisseCreateRequest } from '../models/caisse-create-request';
import { CaisseResponse } from '../models/caisse-response';

@Injectable({
  providedIn: 'root'
})
export class CaisseService {
  private readonly apiUrl = `${API_BASE_URL}/caisses`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<CaisseResponse[]> {
    return this.http.get<CaisseResponse[]>(this.apiUrl);
  }

  getActives(): Observable<CaisseResponse[]> {
    return this.http.get<CaisseResponse[]>(`${this.apiUrl}/actives`);
  }

  getAccessibles(): Observable<CaisseResponse[]> {
    return this.http.get<CaisseResponse[]>(`${this.apiUrl}/accessibles`);
  }

  getById(id: number): Observable<CaisseResponse> {
    return this.http.get<CaisseResponse>(`${this.apiUrl}/${id}`);
  }

  create(request: CaisseCreateRequest): Observable<CaisseResponse> {
    return this.http.post<CaisseResponse>(this.apiUrl, request);
  }

  initialiserMaCaisse(): Observable<CaisseResponse> {
    return this.http.post<CaisseResponse>(`${this.apiUrl}/initialiser-ma-caisse`, {});
  }
}