import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';

import { MembreResponse } from '../models/membre-response';
import { MembreCreateRequest } from '../models/membre-create-request';
import { MembreUpdateRequest } from '../models/membre-update-request';
import { MembreActivationResponseDTO } from '../models/membre-creation-response';
import { Page } from '../../../shared/models/page.model';

@Injectable({
  providedIn: 'root'
})
export class MembreService {

  private http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/membres`;

  // PHASE 3B: Paginated version
  getAllPaginated(page: number = 0, size: number = 10): Observable<Page<MembreResponse>> {
    return this.http.get<Page<MembreResponse>>(`${this.baseUrl}?page=${page}&size=${size}`);
  }

  // Backward compatibility: keep original method
  getAll(): Observable<MembreResponse[]> {
    return this.http.get<MembreResponse[]>(this.baseUrl);
  }

  searchPaginated(q: string = '', siteId?: number, page: number = 0, size: number = 20): Observable<Page<MembreResponse>> {
    const params = new URLSearchParams();
    if (q && q.trim().length > 0) {
      params.set('q', q.trim());
    }
    if (siteId !== undefined && siteId !== null) {
      params.set('siteId', String(siteId));
    }
    params.set('page', String(page));
    params.set('size', String(size));
    return this.http.get<Page<MembreResponse>>(`${this.baseUrl}/search?${params.toString()}`);
  }

  getById(id: number): Observable<MembreResponse> {
    return this.http.get<MembreResponse>(`${this.baseUrl}/${id}`);
  }

  create(request: MembreCreateRequest): Observable<MembreActivationResponseDTO> {
    return this.http.post<MembreActivationResponseDTO>(this.baseUrl, request);
  }

  update(id: number, request: MembreUpdateRequest): Observable<MembreResponse> {
    return this.http.put<MembreResponse>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getCurrentMember(): Observable<MembreResponse> {
    return this.http.get<MembreResponse>(`${this.baseUrl}/me`);
  }
}