import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { CompteEpargneCreateRequest } from '../models/compte-epargne-create-request';
import { CompteEpargneResponse } from '../models/compte-epargne-response';
import { Page } from '../../../shared/models/page.model';


@Injectable({
  providedIn: 'root'
})
export class CompteEpargneService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/comptes-epargne`;

  getAll(): Observable<CompteEpargneResponse[] | Page<CompteEpargneResponse>> {
    return this.http.get<CompteEpargneResponse[] | Page<CompteEpargneResponse>>(this.apiUrl);
  }

  getComptesActifsPourRetraitGuichet(): Observable<CompteEpargneResponse[]> {
    return this.http.get<CompteEpargneResponse[]>(`${this.apiUrl}/guichet/retrait`);
  }

  create(request: CompteEpargneCreateRequest): Observable<CompteEpargneResponse> {
    return this.http.post<CompteEpargneResponse>(this.apiUrl, request);
  }

  getById(id: number): Observable<CompteEpargneResponse> {
    return this.http.get<CompteEpargneResponse>(`${this.apiUrl}/${id}`);
  }

  getByMembre(membreId: number): Observable<CompteEpargneResponse[]> {
    return this.http.get<CompteEpargneResponse[]>(`${this.apiUrl}/membre/${membreId}`);
  }

  getMesMembresComptes(): Observable<CompteEpargneResponse[]> {
    return this.http.get<CompteEpargneResponse[]>(`${this.apiUrl}/mes-membres`);
  }

  getMesComptes(): Observable<CompteEpargneResponse[]> {
    return this.http.get<CompteEpargneResponse[]>(`${this.apiUrl}/me`);
  }

  getActiveByMembre(membreId: number): Observable<CompteEpargneResponse> {
    return this.http.get<CompteEpargneResponse>(`${this.apiUrl}/membre/${membreId}/actif`);
  }

  repairMissingForMember(membreId: number): Observable<CompteEpargneResponse> {
    return this.http.post<CompteEpargneResponse>(`${this.apiUrl}/membre/${membreId}/repair`, {});
  }
}