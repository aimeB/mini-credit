import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AppConfigService {
  private readonly CONFIG_KEY = 'app_config_public_frontend_url';

  private accessUrlSubject = new BehaviorSubject<string>(this.resolveFrontendPublicUrl());
  public accessUrl$ = this.accessUrlSubject.asObservable();

  getAccessUrl(): string {
    return this.resolveFrontendPublicUrl();
  }

  getConfiguredAccessUrl(): string {
    return this.normalizeUrl(localStorage.getItem(this.CONFIG_KEY) || '');
  }

  getRuntimeOrigin(): string {
    return `${window.location.protocol}//${window.location.host}`.replace(/\/$/, '');
  }

  getEnvironmentLabel(): 'DEV' | 'PROD' {
    return this.isLocalhost() ? 'DEV' : 'PROD';
  }

  setAccessUrl(url: string): void {
    const cleanUrl = this.normalizeUrl(url);
    if (!cleanUrl) {
      throw new Error('URL publique frontend invalide');
    }

    localStorage.setItem(this.CONFIG_KEY, cleanUrl);
    this.accessUrlSubject.next(cleanUrl);
  }

  buildActivationLink(code: string, baseUrl?: string): string {
    const activationCode = code.trim();
    if (!activationCode) {
      throw new Error('Code d\'activation introuvable');
    }

    const resolvedBaseUrl = this.normalizeUrl(baseUrl || this.resolveFrontendPublicUrl());
    if (!resolvedBaseUrl) {
      throw new Error(
        'URL publique frontend non configurée. Configurez app.frontend-public-url côté backend ou utilisez l’option ADMIN « Configurer URL publique frontend ».'
      );
    }

    return `${resolvedBaseUrl}/activate?code=${encodeURIComponent(activationCode)}`;
  }

  isLocalhost(): boolean {
    return window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
  }

  isConfigured(): boolean {
    return !!this.getConfiguredAccessUrl();
  }

  resetToDefault(): void {
    localStorage.removeItem(this.CONFIG_KEY);
    this.accessUrlSubject.next(this.resolveFrontendPublicUrl());
  }

  private resolveFrontendPublicUrl(): string {
    const configuredUrl = this.getConfiguredAccessUrl();
    if (configuredUrl) {
      return configuredUrl;
    }

    return this.isLocalhost() ? '' : this.getRuntimeOrigin();
  }

  private normalizeUrl(url: string): string {
    const trimmedUrl = url.trim();
    if (!trimmedUrl) {
      return '';
    }

    try {
      const parsedUrl = new URL(trimmedUrl);
      if (parsedUrl.protocol !== 'http:' && parsedUrl.protocol !== 'https:') {
        return '';
      }

      parsedUrl.hash = '';
      parsedUrl.search = '';
      const pathName = parsedUrl.pathname && parsedUrl.pathname !== '/'
        ? parsedUrl.pathname.replace(/\/+$/, '')
        : '';

      return `${parsedUrl.protocol}//${parsedUrl.host}${pathName}`;
    } catch {
      return '';
    }
  }
}

