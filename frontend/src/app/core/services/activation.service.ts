import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ActivationRequest, ActivationResponse } from '../models/activation.model';

@Injectable({
  providedIn: 'root'
})
export class ActivationService {
  private apiUrl = '/api/auth';

  constructor(private http: HttpClient) {}

  /**
   * Activer un compte avec code d'activation + nouveau password
   * @param request - Contient le code, password et confirmation
   * @returns Observable avec AuthResponse (token + infos utilisateur)
   */
  activateAccount(request: ActivationRequest): Observable<ActivationResponse> {
    return this.http.post<ActivationResponse>(`${this.apiUrl}/activate`, request);
  }
}
