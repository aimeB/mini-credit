import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';

export interface ChangePasswordRequest {
  oldPassword?: string;
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ResetPasswordRequest {
  newPassword: string;
  confirmPassword: string;
}

export interface ChangePasswordResponse {
  message: string;
  success: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class PasswordChangeService {
  private readonly usersApiUrl = `${API_BASE_URL}/utilisateurs`;
  private readonly authApiUrl = `${API_BASE_URL}/auth`;

  constructor(private http: HttpClient) {}

  /**
   * Changer le mot de passe de l'utilisateur actuel
   * Demande l'ancien mot de passe pour sécurité
   */
  changePassword(userId: number, request: ChangePasswordRequest): Observable<ChangePasswordResponse> {
    console.log('🔐 Tentative de changement de mot de passe pour utilisateur:', userId);
    return this.http.post<ChangePasswordResponse>(`${this.authApiUrl}/change-password`, {
      oldPassword: request.oldPassword ?? request.currentPassword,
      currentPassword: request.currentPassword,
      newPassword: request.newPassword,
      confirmPassword: request.confirmPassword
    });
  }

  /**
   * Réinitialiser le mot de passe sans l'ancien
   * Utilisé quand l'utilisateur a oublié son mot de passe
   * Seulement possible pour son propre compte (authentification JWT requise)
   */
  resetPasswordSelf(userId: number, request: ResetPasswordRequest): Observable<{ message: string; success: string }> {
    console.log('🔄 Tentative de réinitialisation de mot de passe pour utilisateur:', userId);
    return this.http.post<{ message: string; success: string }>(
      `${this.usersApiUrl}/${userId}/reset-password-self`,
      request
    );
  }
}

