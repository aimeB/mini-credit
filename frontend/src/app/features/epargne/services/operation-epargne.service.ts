import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { OperationEpargneRequest } from '../models/operation-epargne-request';
import { OperationEpargneResponse } from '../models/operation-epargne-response';
import { Page } from '../../../shared/models/page.model';


@Injectable({
  providedIn: 'root'
})
export class OperationEpargneService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/operations-epargne`;

  enregistrer(request: OperationEpargneRequest): Observable<OperationEpargneResponse> {
    return this.http.post<OperationEpargneResponse>(this.apiUrl, request);
  }

  // PHASE 3B: Paginated version
  getAllPaginated(page: number = 0, size: number = 10): Observable<Page<OperationEpargneResponse>> {
    return this.http.get<Page<OperationEpargneResponse>>(`${this.apiUrl}?page=${page}&size=${size}`);
  }

  // Backward compatibility: keep original method
  getAll(): Observable<OperationEpargneResponse[]> {
    return this.http.get<OperationEpargneResponse[]>(this.apiUrl);
  }

  getByCompte(compteId: number): Observable<OperationEpargneResponse[]> {
    return this.http.get<OperationEpargneResponse[]>(`${this.apiUrl}/compte/${compteId}`);
  }

  getByMembre(membreId: number): Observable<OperationEpargneResponse[]> {
    return this.http.get<OperationEpargneResponse[]>(`${this.apiUrl}/membre/${membreId}`);
  }
}