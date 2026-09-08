import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { AppConfigService } from '../../../core/services/app-config.service';

@Component({
  selector: 'app-config-access-url',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="config-modal">
      <div class="modal-header">
        <h2>Configurer URL publique frontend</h2>
        <p class="subtitle">Cette URL sert a generer le lien d'activation partage au membre.</p>
      </div>

      <div class="config-content">
        <div class="info-box">
          <p><strong>Environnement:</strong> {{ environmentLabel }}</p>
          <p><strong>URL actuelle de l'application:</strong> {{ runtimeOrigin }}</p>
        </div>

        <div class="form-group">
          <label for="accessUrl" class="form-label">URL publique frontend</label>
          <input
            id="accessUrl"
            type="text"
            [(ngModel)]="accessUrl"
            placeholder="https://app.example.com"
            class="form-input"
            [class.error]="!!errorMessage"
          />
          <small class="hint">Exemples: http://localhost:4200 en DEV, https://URL_OFFICIELLE_APPLICATION en PROD.</small>
          <div *ngIf="errorMessage" class="error-message">{{ errorMessage }}</div>
        </div>

        <div class="warning-box">
          <p class="warning-title">Regle</p>
          <p class="warning-text">
            N'entrez que l'URL publique finale du frontend. Aucun chemin /activate, aucun parametre, aucun mot de passe.
          </p>
        </div>
      </div>

      <div class="modal-actions">
        <button type="button" (click)="testConnection()" class="btn-secondary" [disabled]="testingUrl">
          {{ testingUrl ? 'Ouverture...' : 'Tester /activate' }}
        </button>
        <button type="button" (click)="saveConfig()" class="btn-primary">Enregistrer</button>
      </div>
    </div>
  `,
  styles: [`
    .config-modal {
      padding: 28px;
      max-width: 620px;
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    }

    .modal-header {
      margin-bottom: 24px;
      padding-bottom: 12px;
      border-bottom: 2px solid #0f766e;
    }

    .modal-header h2 {
      margin: 0 0 8px;
      color: #0f766e;
      font-size: 24px;
    }

    .subtitle {
      margin: 0;
      color: #475569;
      line-height: 1.5;
    }

    .config-content {
      display: grid;
      gap: 16px;
    }

    .info-box,
    .warning-box {
      padding: 16px;
      border-radius: 10px;
      background: #f8fafc;
      border: 1px solid #cbd5e1;
    }

    .info-box p,
    .warning-box p {
      margin: 0 0 8px;
    }

    .info-box p:last-child,
    .warning-box p:last-child {
      margin-bottom: 0;
    }

    .warning-title {
      font-weight: 700;
      color: #9a3412;
    }

    .warning-text {
      color: #7c2d12;
      line-height: 1.5;
    }

    .form-group {
      display: grid;
      gap: 8px;
    }

    .form-label {
      font-weight: 600;
      color: #0f172a;
    }

    .form-input {
      width: 100%;
      padding: 12px 14px;
      border-radius: 10px;
      border: 1px solid #94a3b8;
      font-size: 14px;
    }

    .form-input.error {
      border-color: #dc2626;
      background: #fef2f2;
    }

    .hint {
      color: #64748b;
      line-height: 1.4;
    }

    .error-message {
      color: #b91c1c;
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 8px;
      padding: 10px 12px;
    }

    .modal-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 24px;
    }

    .btn-primary,
    .btn-secondary {
      border: none;
      border-radius: 10px;
      padding: 12px 18px;
      font-weight: 600;
      cursor: pointer;
    }

    .btn-primary {
      background: #0f766e;
      color: #fff;
    }

    .btn-secondary {
      background: #e2e8f0;
      color: #0f172a;
    }

    .btn-primary:disabled,
    .btn-secondary:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  `]
})
export class ConfigAccessUrlComponent implements OnInit {
  accessUrl = '';
  runtimeOrigin = '';
  environmentLabel: 'DEV' | 'PROD' = 'DEV';
  errorMessage = '';
  testingUrl = false;

  private dialogRef = inject(MatDialogRef<ConfigAccessUrlComponent>);
  private appConfig = inject(AppConfigService);

  ngOnInit(): void {
    this.runtimeOrigin = this.appConfig.getRuntimeOrigin();
    this.environmentLabel = this.appConfig.getEnvironmentLabel();
    this.accessUrl = this.appConfig.getConfiguredAccessUrl() || this.runtimeOrigin;
  }

  testConnection(): void {
    const normalizedUrl = this.validateAndNormalizeUrl();
    if (!normalizedUrl) {
      return;
    }

    this.testingUrl = true;
    window.open(`${normalizedUrl}/activate`, '_blank');
    setTimeout(() => {
      this.testingUrl = false;
    }, 800);
  }

  saveConfig(): void {
    const normalizedUrl = this.validateAndNormalizeUrl();
    if (!normalizedUrl) {
      return;
    }

    this.appConfig.setAccessUrl(normalizedUrl);
    this.dialogRef.close(true);
  }

  private validateAndNormalizeUrl(): string {
    this.errorMessage = '';

    const rawUrl = this.accessUrl.trim();
    if (!rawUrl) {
      this.errorMessage = 'L\'URL publique frontend est obligatoire.';
      return '';
    }

    try {
      const parsedUrl = new URL(rawUrl);
      if (parsedUrl.protocol !== 'http:' && parsedUrl.protocol !== 'https:') {
        this.errorMessage = 'L\'URL doit commencer par http:// ou https://.';
        return '';
      }

      if (parsedUrl.search || parsedUrl.hash) {
        this.errorMessage = 'N\'ajoutez ni parametres ni fragment a l\'URL publique frontend.';
        return '';
      }

      const pathName = parsedUrl.pathname && parsedUrl.pathname !== '/'
        ? parsedUrl.pathname.replace(/\/+$/, '')
        : '';

      return `${parsedUrl.protocol}//${parsedUrl.host}${pathName}`;
    } catch {
      this.errorMessage = 'Format invalide. Exemple valide: https://app.example.com';
      return '';
    }
  }
}