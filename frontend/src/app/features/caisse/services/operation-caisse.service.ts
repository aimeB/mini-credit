import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { OperationCaisseRequest } from '../models/operation-caisse-request';
import { OperationCaisseResponse } from '../models/operation-caisse-response';
import { Page } from '../../../shared/models/page.model';
import { JournalCaisseResponse } from '../models/journal-caisse-response';
import { JournalCaisseFilter } from '../models/journal-caisse-filter';

@Injectable({
  providedIn: 'root'
})
export class OperationCaisseService {
  private readonly apiUrl = `${API_BASE_URL}/operations-caisse`;

  constructor(private http: HttpClient) {}

  enregistrer(request: OperationCaisseRequest): Observable<OperationCaisseResponse> {
    return this.http.post<OperationCaisseResponse>(this.apiUrl, request);
  }

  // PHASE 3B: Paginated version
  getAllPaginated(page: number = 0, size: number = 10): Observable<Page<OperationCaisseResponse>> {
    return this.http.get<Page<OperationCaisseResponse>>(`${this.apiUrl}?page=${page}&size=${size}`);
  }

  // Backward compatibility: keep original method
  getAll(): Observable<OperationCaisseResponse[]> {
    return this.http.get<Page<OperationCaisseResponse> | OperationCaisseResponse[]>(this.apiUrl).pipe(
      map((response) => Array.isArray(response) ? response : (response?.content ?? []))
    );
  }

  getBySession(sessionId: number): Observable<OperationCaisseResponse[]> {
    return this.http.get<OperationCaisseResponse[]>(`${this.apiUrl}/session/${sessionId}`);
  }

  getByCaisse(caisseId: number): Observable<OperationCaisseResponse[]> {
    return this.http.get<OperationCaisseResponse[]>(`${this.apiUrl}/caisse/${caisseId}`);
  }

  getJournalCaisse(
    filters: JournalCaisseFilter = {},
    page: number = 0,
    size: number = 20,
    sort: string = 'dateOperation,desc'
  ): Observable<Page<JournalCaisseResponse>> {
    let params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size))
      .set('sort', sort);

    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    });

    return this.http.get<Page<JournalCaisseResponse>>(`${this.apiUrl}/journal`, { params });
  }
}