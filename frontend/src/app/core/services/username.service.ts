import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';

export interface ChangeUsernameRequest {
  newUsername: string;
}

export interface UsernameCheckResponse {
  available: boolean;
  username: string;
}

@Injectable({
  providedIn: 'root'
})
export class UsernameService {
  private readonly apiUrl = `${API_BASE_URL}/utilisateurs`;

  constructor(private http: HttpClient) {}

  /**
   * Changer le username de l'utilisateur actuel
   */
  changeUsername(userId: number, request: ChangeUsernameRequest): Observable<any> {
    console.log('🔄 Tentative de changement de username:', request.newUsername);
    return this.http.post(`${this.apiUrl}/${userId}/change-username`, request);
  }

  /**
   * Vérifier si un username est disponible (validation en temps réel)
   */
  checkUsernameAvailability(username: string): Observable<UsernameCheckResponse> {
    return this.http.get<UsernameCheckResponse>(
      `${this.apiUrl}/check-username/${username.toLowerCase()}`
    );
  }
}
