import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { API_BASE_URL } from './api.config';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  id: number;
  username: string;
  email: string;
  nomComplet: string;
  role: string;
  posteEmploye?: string;
  siteId?: number;
  siteNom?: string;
  permissions?: string[];
  passwordChangeRequired?: boolean;
}

export interface User {
  id: number;
  username: string;
  email: string;
  nomComplet: string;
  role: string;
  posteEmploye?: string;
  siteId?: number;
  siteNom?: string;
  permissions?: string[];
  passwordChangeRequired?: boolean;
}

interface ApiErrorBody {
  code?: string;
  message?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly USER_KEY = 'auth_user';

  private currentUserSubject = new BehaviorSubject<User | null>(this.getUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  private normalizeRole(role: string | null | undefined): string {
    let normalized = (role ?? '').trim().toUpperCase();

    if (normalized.startsWith('ROLE_')) {
      normalized = normalized.slice(5);
    }

    return normalized;
  }

  private normalizeExpectedRole(role: string | null | undefined): string {
    let normalized = (role ?? '').trim().toUpperCase();
    if (normalized.startsWith('ROLE_')) {
      normalized = normalized.slice(5);
    }
    return normalized;
  }

  private normalizeUser(user: User | null): User | null {
    if (!user) {
      return null;
    }

    return {
      ...user,
      role: this.normalizeRole(user.role)
    };
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    console.log('🔐 LOGIN ATTEMPT:', {
      username: credentials.username,
      passwordLength: credentials.password.length,
      timestamp: new Date().toISOString(),
      apiUrl: `${API_BASE_URL}/auth/login`
    });

    return this.http.post<AuthResponse>(`${API_BASE_URL}/auth/login`, credentials)
      .pipe(
        tap(response => {
          console.log('✅ LOGIN SUCCESS:', {
            username: response.username,
            email: response.email,
            role: response.role,
            tokenPresent: !!response.token,
            tokenLength: response.token?.length || 0
          });
          this.setSession(response);
          // Utiliser directement la réponse au lieu de parser le token
          const user: User = {
            id: response.id,
            username: response.username,
            email: response.email,
            nomComplet: response.nomComplet,
            role: this.normalizeRole(response.role),
            posteEmploye: response.posteEmploye,
            siteId: response.siteId,
            siteNom: response.siteNom,
            permissions: response.permissions,
            passwordChangeRequired: !!response.passwordChangeRequired
          };
          this.currentUserSubject.next(user);
        }),
        catchError(error => {
          console.error('❌ LOGIN FAILED:', {
            status: error.status,
            statusText: error.statusText,
            message: error.error?.message || error.message,
            fullError: error.error
          });
          return this.handleError(error);
        })
      );
  }

  register(userData: any): Observable<any> {
    return this.http.post(`${API_BASE_URL}/auth/register`, userData)
      .pipe(catchError(this.handleError));
  }

  activate(activationCode: string, newPassword: string, confirmPassword: string): Observable<AuthResponse> {
    const request = {
      activationCode,
      newPassword,
      confirmPassword
    };

    console.log('🔓 ACTIVATION ATTEMPT:', {
      activationCode,
      newPasswordLength: newPassword.length,
      confirmPasswordMatch: newPassword === confirmPassword,
      timestamp: new Date().toISOString(),
      apiUrl: `${API_BASE_URL}/auth/activate`
    });

    return this.http.post<AuthResponse>(`${API_BASE_URL}/auth/activate`, request)
      .pipe(
        tap(response => {
          console.log('✅ ACTIVATION SUCCESS:', {
            username: response.username,
            email: response.email,
            role: response.role,
            tokenPresent: !!response.token,
            tokenLength: response.token?.length || 0
          });
          this.setSession(response);
          // Utiliser directement la réponse au lieu de parser le token
          const user: User = {
            id: response.id,
            username: response.username,
            email: response.email,
            nomComplet: response.nomComplet,
            role: this.normalizeRole(response.role),
            posteEmploye: response.posteEmploye,
            siteId: response.siteId,
            siteNom: response.siteNom,
            permissions: response.permissions,
            passwordChangeRequired: !!response.passwordChangeRequired
          };
          this.currentUserSubject.next(user);
        }),
        catchError(error => {
          console.error('❌ ACTIVATION FAILED:', {
            status: error.status,
            statusText: error.statusText,
            message: error.error?.message || error.message,
            fullError: error.error
          });
          return this.handleError(error);
        })
      );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUserSubject.next(null);
    this.router.navigate(['/auth/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  mustChangePassword(): boolean {
    return !!this.getCurrentUser()?.passwordChangeRequired;
  }

  clearPasswordChangeRequired(): void {
    const current = this.getCurrentUser();
    if (!current) {
      return;
    }
    const updated = { ...current, passwordChangeRequired: false };
    this.currentUserSubject.next(updated);
    localStorage.setItem(this.USER_KEY, JSON.stringify(updated));
  }

  isAuthenticated(): boolean {
    const token = this.getToken();

    if (!token || this.isTokenExpired(token)) {
      this.logout();
      return false;
    }

    return !!this.getCurrentUser();
  }

  hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    return user ? this.normalizeRole(user.role) === this.normalizeExpectedRole(role) : false;
  }

  private isTokenExpired(token: string): boolean {
    const payload = this.getTokenPayload(token);
    if (!payload) {
      return true;
    }

    return typeof payload.exp === 'number' ? (Date.now() / 1000) > payload.exp : false;
  }

  private getTokenPayload(token: string): any | null {
    try {
      return JSON.parse(atob(token.split('.')[1]));
    } catch (error) {
      console.error('Error parsing JWT payload', error);
      return null;
    }
  }

  hasAnyRole(roles: string[]): boolean {
    const user = this.getCurrentUser();
    return user ? roles.map(role => this.normalizeExpectedRole(role)).includes(this.normalizeRole(user.role)) : false;
  }

  hasPermission(permission: string): boolean {
    const user = this.getCurrentUser();
    if (!user || !permission) {
      return false;
    }

    const permissions = user.permissions ?? [];
    return permissions.includes(permission);
  }

  hasAnyPermission(permissions: string[]): boolean {
    const user = this.getCurrentUser();
    if (!user || !permissions || permissions.length === 0) {
      return false;
    }

    const userPermissions = user.permissions ?? [];
    if (userPermissions.length === 0) {
      return false;
    }

    return permissions.some(permission => userPermissions.includes(permission));
  }

  /**
   * Check if user can access a resource based on role hierarchy
   * ADMIN > GERANT_GENERAL > RCI > COO > CHEF_BUREAU > CONTROLEUR > CAISSIER > GESTIONNAIRE > AGENT_TERRAIN > MEMBER
   */
  hasMinimumRole(minimumRole: string): boolean {
    const user = this.getCurrentUser();
    if (!user) return false;

    const roleHierarchy: { [key: string]: number } = {
      'ADMIN': 9,
      'GERANT_GENERAL': 8,
      'RCI': 7,
      'COO': 6,
      'CHEF_BUREAU': 5,
      'CONTROLEUR': 4,
      'CAISSIER': 3,
      'GESTIONNAIRE': 2,
      'AGENT_TERRAIN': 1,
      'MEMBER': 0
    };

    const userRoleLevel = roleHierarchy[this.normalizeRole(user.role)] || 0;
    const minimumLevel = roleHierarchy[this.normalizeExpectedRole(minimumRole)] || 0;
    return userRoleLevel >= minimumLevel;
  }

  private setSession(authResult: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, authResult.token);
    
    // Utilisez directement les données de la réponse au lieu d'extraire du token
    const user: User = {
      id: authResult.id,
      username: authResult.username,
      email: authResult.email,
      nomComplet: authResult.nomComplet,
      role: this.normalizeRole(authResult.role),
      posteEmploye: authResult.posteEmploye,
      siteId: authResult.siteId,
      siteNom: authResult.siteNom,
      permissions: authResult.permissions,
      passwordChangeRequired: !!authResult.passwordChangeRequired
    };

    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  private getUserFromStorage(): User | null {
    const userStr = localStorage.getItem(this.USER_KEY);
    return userStr ? this.normalizeUser(JSON.parse(userStr)) : null;
  }

  private getUserFromToken(token: string): User | null {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return {
        id: payload.userId ?? payload.id ?? 0,
        username: payload.sub ?? payload.username ?? '',
        email: payload.email ?? payload.emailAddress ?? '',
        nomComplet: payload.nomComplet ?? payload.fullName ?? payload.name ?? '',
        role: this.normalizeRole(payload.role ?? (Array.isArray(payload.roles) ? payload.roles[0]?.replace('ROLE_', '') : 'MEMBER')),
        posteEmploye: payload.posteEmploye ?? payload.poste ?? null,
        permissions: Array.isArray(payload.permissions)
          ? payload.permissions
          : (Array.isArray(payload.authorities)
            ? payload.authorities.filter((item: unknown) => typeof item === 'string' && !String(item).startsWith('ROLE_')) as string[]
            : undefined)
      };
    } catch (error) {
      console.error('Error parsing JWT token', error);
      return null;
    }
  }

  private handleError = (error: HttpErrorResponse) => {
    let errorMessage = 'Une erreur inconnue est survenue';
    const errorBody = (error.error ?? {}) as ApiErrorBody;
    const errorCode = (errorBody.code ?? '').toUpperCase();

    if (error.error instanceof ErrorEvent) {
      errorMessage = `Erreur: ${error.error.message}`;
    } else {
      if (errorCode) {
        switch (errorCode) {
          case 'USER_NOT_FOUND':
            errorMessage = 'Utilisateur introuvable';
            break;
          case 'BAD_PASSWORD':
            errorMessage = 'Mot de passe incorrect';
            break;
          case 'ACCOUNT_DISABLED':
            errorMessage = 'Compte désactivé. Contactez l\'administrateur';
            break;
          case 'ACCOUNT_LOCKED':
            errorMessage = 'Compte verrouillé. Contactez l\'administrateur';
            break;
          case 'PASSWORD_RESET_PENDING':
            errorMessage = 'Mot de passe temporaire requis. Connectez-vous avec le temporaire puis changez-le.';
            break;
          default:
            break;
        }
      }

      // First, check if there's a specific error message from the backend
      if (errorMessage === 'Une erreur inconnue est survenue' && errorBody.message) {
        errorMessage = errorBody.message;
      } else {
        // Fall back to generic status-based messages
        if (errorMessage === 'Une erreur inconnue est survenue') {
          switch (error.status) {
            case 401:
              errorMessage = 'Nom d\'utilisateur ou mot de passe incorrect';
              break;
            case 403:
              errorMessage = 'Accès refusé';
              break;
            case 404:
              errorMessage = 'Service non trouvé';
              break;
            case 500:
              errorMessage = 'Erreur interne du serveur';
              break;
            default:
              errorMessage = `Code d'erreur: ${error.status}`;
          }
        }
      }
    }

    console.error('AuthService Error:', error);
    return throwError(() => new Error(errorMessage));
  };
}