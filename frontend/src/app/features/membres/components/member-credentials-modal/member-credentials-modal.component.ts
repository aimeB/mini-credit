import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AuthService } from '../../../../core/services/auth.service';
import { AppConfigService } from '../../../../core/services/app-config.service';
import { ConfigAccessUrlComponent } from '../../../../shared/components/config-access-url/config-access-url.component';

@Component({
  selector: 'app-member-credentials-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="credentials-modal">
      <div class="modal-header">
        <h2>Compte cree avec succes</h2>
        <p class="message">{{ data?.message }}</p>
      </div>

      <div class="credentials-box">
        <div class="credential-row">
          <label>Nom d'utilisateur</label>
          <input type="text" readonly [value]="data?.credentials?.username || data?.membre?.nomComplet || ''" class="value-input" />
        </div>

        <div class="credential-row">
          <label>Telephone</label>
          <input type="text" readonly [value]="data?.membre?.telephonePrincipal || ''" class="value-input" />
        </div>

        <div class="credential-row code-highlight">
          <label>Code d'activation</label>
          <div class="value-row">
            <input type="text" readonly [value]="activationCode" class="value-input code-input" [class.copied]="codeCopied" />
            <button type="button" (click)="copyCode(activationCode, 'code')" class="btn-copy" [disabled]="!activationCode">
              {{ codeCopied ? 'Copie' : 'Copier' }}
            </button>
          </div>
        </div>

        <div class="credential-row link-highlight">
          <label>Lien d'activation</label>
          <div class="value-row">
            <input type="text" readonly [value]="activationLink || 'URL publique frontend non configuree'" class="value-input link-input" [class.copied]="linkCopied" />
            <button type="button" (click)="copyCode(activationLink, 'link')" class="btn-copy" [disabled]="!activationLink">
              {{ linkCopied ? 'Copie' : 'Copier' }}
            </button>
          </div>
          <small class="hint-text">Le membre peut ouvrir ce lien ou saisir manuellement le code sur /activate.</small>
          <div *ngIf="linkGenerationError" class="warning-message">{{ linkGenerationError }}</div>
        </div>

        <div class="network-box">
          <p><strong>URL actuelle utilisee:</strong> {{ publicUrlUsed }}</p>
          <p><strong>Environnement:</strong> {{ environmentLabel }}</p>
        </div>

        <div class="credential-row qr-highlight" *ngIf="activationLink">
          <label>QR code</label>
          <div class="qr-container">
            <img [src]="qrCodeDataUrl" alt="QR code d'activation" class="qr-code" />
            <button type="button" (click)="downloadQR()" class="btn-secondary">Telecharger QR</button>
          </div>
        </div>

        <div class="timer-section" [class.expiring]="timeRemaining <= 10">
          Fermeture automatique dans <strong>{{ formatDuration(timeRemaining) }}</strong>
        </div>
      </div>

      <div class="instructions-box">
        <p><strong>Important</strong></p>
        <ul>
          <li>Si le lien ne s'ouvre pas, communiquez simplement le code au membre.</li>
          <li>Le code reste valable 48 heures.</li>
          <li>Aucun mot de passe n'est expose dans cette fenetre.</li>
        </ul>
      </div>

      <div class="modal-actions">
        <button type="button" (click)="openConfigDialog()" class="btn-secondary" *ngIf="canConfigureFrontendUrl">
          Configurer URL publique frontend
        </button>
        <button type="button" (click)="close()" class="btn-primary">Fermer</button>
      </div>
    </div>
  `,
  styles: [`
    .credentials-modal {
      padding: 20px;
      max-width: 640px;
      max-height: 90vh;
      overflow-y: auto;
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    }

    .modal-header {
      margin-bottom: 20px;
      padding-bottom: 12px;
      border-bottom: 2px solid #16a34a;
    }

    .modal-header h2 {
      margin: 0 0 6px;
      color: #166534;
      font-size: 24px;
    }

    .message {
      margin: 0;
      color: #475569;
    }

    .credentials-box,
    .instructions-box,
    .network-box {
      border-radius: 12px;
      border: 1px solid #cbd5e1;
      background: #f8fafc;
    }

    .credentials-box {
      padding: 18px;
      display: grid;
      gap: 16px;
    }

    .credential-row {
      display: grid;
      gap: 8px;
    }

    .credential-row label {
      font-weight: 700;
      color: #0f172a;
    }

    .value-row {
      display: grid;
      grid-template-columns: 1fr auto;
      gap: 10px;
      align-items: center;
    }

    .value-input {
      width: 100%;
      padding: 12px 14px;
      border-radius: 10px;
      border: 1px solid #94a3b8;
      background: #fff;
      font-size: 14px;
    }

    .value-input.copied {
      border-color: #16a34a;
      background: #f0fdf4;
    }

    .code-highlight .value-input {
      font-weight: 700;
      letter-spacing: 0.04em;
    }

    .hint-text {
      color: #64748b;
      line-height: 1.4;
    }

    .warning-message {
      padding: 12px 14px;
      border-radius: 10px;
      background: #fff7ed;
      border: 1px solid #fdba74;
      color: #9a3412;
      line-height: 1.5;
    }

    .network-box {
      padding: 14px 16px;
      background: #eef2ff;
      border-color: #c7d2fe;
    }

    .network-box p {
      margin: 0 0 8px;
      color: #312e81;
    }

    .network-box p:last-child {
      margin-bottom: 0;
    }

    .qr-container {
      display: grid;
      justify-items: start;
      gap: 12px;
    }

    .qr-code {
      width: 180px;
      height: 180px;
      border-radius: 12px;
      background: #fff;
      border: 1px solid #cbd5e1;
    }

    .timer-section {
      color: #334155;
      font-size: 14px;
    }

    .timer-section.expiring {
      color: #b91c1c;
    }

    .instructions-box {
      margin-top: 18px;
      padding: 16px 18px;
    }

    .instructions-box p {
      margin: 0 0 10px;
      color: #0f172a;
    }

    .instructions-box ul {
      margin: 0;
      padding-left: 18px;
      color: #334155;
      line-height: 1.5;
    }

    .modal-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 18px;
    }

    .btn-copy,
    .btn-primary,
    .btn-secondary {
      border: none;
      border-radius: 10px;
      padding: 11px 16px;
      font-weight: 600;
      cursor: pointer;
    }

    .btn-copy,
    .btn-secondary {
      background: #e2e8f0;
      color: #0f172a;
    }

    .btn-primary {
      background: #2563eb;
      color: #fff;
    }

    .btn-copy:disabled,
    .btn-primary:disabled,
    .btn-secondary:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    @media (max-width: 640px) {
      .credentials-modal {
        padding: 14px;
      }

      .value-row {
        grid-template-columns: 1fr;
      }

      .modal-actions {
        flex-direction: column;
      }

      .btn-copy,
      .btn-primary,
      .btn-secondary {
        width: 100%;
      }
    }
  `]
})
export class MemberCredentialsModalComponent implements OnInit, OnDestroy {
  timeRemaining = 300;
  codeCopied = false;
  linkCopied = false;
  activationCode = '';
  activationLink = '';
  qrCodeDataUrl = '';
  linkGenerationError = '';
  publicUrlUsed = 'Non configuree';
  environmentLabel: 'DEV' | 'PROD' = 'DEV';
  canConfigureFrontendUrl = false;
  private timerInterval: ReturnType<typeof setInterval> | null = null;

  private dialogRef = inject(MatDialogRef<MemberCredentialsModalComponent>);
  private dialog = inject(MatDialog);
  private authService = inject(AuthService);
  private appConfig = inject(AppConfigService);
  data = inject(MAT_DIALOG_DATA) as any;

  ngOnInit(): void {
    this.activationCode =
      this.data?.activationCode?.code ||
      this.data?.credentials?.code ||
      this.data?.credentials?.temporaryPassword ||
      '';
    this.environmentLabel = this.appConfig.getEnvironmentLabel();
    this.canConfigureFrontendUrl = this.authService.getCurrentUser()?.role === 'ADMIN';
    this.refreshActivationLink();
    this.startTimer();
  }

  ngOnDestroy(): void {
    this.stopTimer();
  }

  formatDuration(seconds: number): string {
    if (seconds < 60) {
      return `${seconds}s`;
    }

    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}m ${remainingSeconds}s`;
  }

  copyCode(value: string, type: 'code' | 'link'): void {
    if (!value) {
      return;
    }

    navigator.clipboard.writeText(value).then(() => {
      if (type === 'code') {
        this.codeCopied = true;
        setTimeout(() => {
          this.codeCopied = false;
        }, 2000);
        return;
      }

      this.linkCopied = true;
      setTimeout(() => {
        this.linkCopied = false;
      }, 2000);
    });
  }

  downloadQR(): void {
    if (!this.qrCodeDataUrl) {
      return;
    }

    const link = document.createElement('a');
    link.href = this.qrCodeDataUrl;
    link.download = `activation-qr-${this.activationCode}.png`;
    link.click();
  }

  openConfigDialog(): void {
    if (!this.canConfigureFrontendUrl) {
      return;
    }

    this.dialog.open(ConfigAccessUrlComponent, {
      width: '700px',
      disableClose: false
    }).afterClosed().subscribe(() => {
      this.refreshActivationLink();
    });
  }

  close(): void {
    this.stopTimer();
    this.dialogRef.close();
  }

  private startTimer(): void {
    this.timerInterval = setInterval(() => {
      this.timeRemaining -= 1;
      if (this.timeRemaining <= 0) {
        this.stopTimer();
        this.close();
      }
    }, 1000);
  }

  private stopTimer(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  private refreshActivationLink(): void {
    const backendLink = (this.data?.activationCode?.activationLink || '').trim();

    this.linkGenerationError = '';

    if (backendLink) {
      this.activationLink = backendLink;
    } else {
      try {
        this.activationLink = this.appConfig.buildActivationLink(this.activationCode);
      } catch (error) {
        this.activationLink = '';
        this.linkGenerationError = error instanceof Error
          ? error.message
          : 'URL publique frontend non configuree.';
      }
    }

    this.publicUrlUsed = this.resolvePublicUrlUsed();
    this.generateQRCode();
  }

  private resolvePublicUrlUsed(): string {
    if (this.activationLink) {
      try {
        const parsedUrl = new URL(this.activationLink);
        const basePath = parsedUrl.pathname.replace(/\/activate$/, '');
        return `${parsedUrl.protocol}//${parsedUrl.host}${basePath}`;
      } catch {
        return this.activationLink;
      }
    }

    return this.appConfig.getConfiguredAccessUrl() || 'Non configuree';
  }

  private generateQRCode(): void {
    if (!this.activationLink) {
      this.qrCodeDataUrl = '';
      return;
    }

    this.qrCodeDataUrl = `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(this.activationLink)}`;
  }
}