import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/services/api.config';
import { UtilisateurResponse } from '../models/utilisateur-response';
import { UtilisateurCreateRequest } from '../models/utilisateur-create-request';
import { UtilisateurUpdateRequest } from '../models/utilisateur-update-request';

@Injectable({
  providedIn: 'root'
})
export class UtilisateurService {
  private http = inject(HttpClient);

  private readonly API_URL = `${API_BASE_URL}/utilisateurs`;

  getAll(): Observable<UtilisateurResponse[]> {
    return this.http.get<UtilisateurResponse[]>(this.API_URL);
  }

  getByRole(role: string): Observable<UtilisateurResponse[]> {
    return this.http.get<UtilisateurResponse[]>(`${this.API_URL}/par-role/${role}`);
  }

  getById(id: number): Observable<UtilisateurResponse> {
    return this.http.get<UtilisateurResponse>(`${this.API_URL}/${id}`);
  }

  create(request: UtilisateurCreateRequest): Observable<UtilisateurResponse> {
    return this.http.post<UtilisateurResponse>(this.API_URL, request);
  }

  update(id: number, request: UtilisateurUpdateRequest): Observable<UtilisateurResponse> {
    return this.http.put<UtilisateurResponse>(`${this.API_URL}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  changePassword(id: number, currentPassword: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.API_URL}/${id}/change-password`, {
      currentPassword,
      newPassword
    });
  }

  resetPassword(id: number, motif: string): Observable<{ username: string; temporaryPassword: string; passwordChangeRequired: boolean; advisoryMessage?: string }> {
    return this.http.post<{ username: string; temporaryPassword: string; passwordChangeRequired: boolean; advisoryMessage?: string }>(
      `${this.API_URL}/${id}/reset-password`,
      { motif }
    );
  }
}
