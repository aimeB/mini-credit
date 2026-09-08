import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { ActivationRequest, ActivationResponse } from '../../../../core/models/activation.model';

/**
 * Page d'activation publique (sans authentification)
 * Le membre entre son code d'activation + choisit son password
 */
@Component({
  selector: 'app-member-activation',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatCardModule
  ],
  template: `
    <div class="activation-container">
      <mat-card class="activation-card">
        <mat-card-header>
          <h1>🔐 Activation Compte Membre</h1>
          <p class="subtitle">Activez votre compte avec le code reçu de l'agent</p>
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="submitActivation()">
            
            <!-- Code d'activation -->
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Code d'Activation</mat-label>
              <input matInput 
                     formControlName="activationCode"
                     placeholder="Ex: MBR-12345-260412"
                     uppercase
              />
              <mat-icon matSuffix>vpn_key</mat-icon>
              <mat-error *ngIf="form.get('activationCode')?.hasError('required')">
                Code d'activation requis
              </mat-error>
            </mat-form-field>

            <!-- Nouveau Password -->
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Nouveau Mot de Passe</mat-label>
              <input matInput 
                     [type]="showPassword ? 'text' : 'password'"
                     formControlName="newPassword"
                     placeholder="Minimum 8 caractères"
              />
              <button mat-icon-button matSuffix type="button" (click)="togglePasswordVisibility()">
                <mat-icon>{{ showPassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              <mat-error *ngIf="form.get('newPassword')?.hasError('required')">
                Mot de passe requis
              </mat-error>
              <mat-error *ngIf="form.get('newPassword')?.hasError('minlength')">
                Minimum 8 caractères
              </mat-error>
            </mat-form-field>

            <!-- Confirmation Password -->
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Confirmer Mot de Passe</mat-label>
              <input matInput 
                     [type]="showPassword ? 'text' : 'password'"
                     formControlName="confirmPassword"
                     placeholder="Confirmer le mot de passe"
              />
              <mat-error *ngIf="form.get('confirmPassword')?.hasError('required')">
                Confirmation requise
              </mat-error>
            </mat-form-field>

            <!-- Error/Success Messages -->
            <div class="message-box" *ngIf="error" class="error-message">
              <mat-icon>error</mat-icon>
              <span>{{ error }}</span>
            </div>

            <div class="message-box" *ngIf="success" class="success-message">
              <mat-icon>check_circle</mat-icon>
              <span>{{ success }}</span>
            </div>

            <!-- Submit Button -->
            <button mat-raised-button color="primary" class="full-width" 
                    [disabled]="form.invalid || loading">
              <mat-icon *ngIf="!loading">done</mat-icon>
              <mat-icon *ngIf="loading" class="spinner">hourglass_empty</mat-icon>
              {{ loading ? 'Activation en cours...' : 'Activer Mon Compte' }}
            </button>
          </form>

          <!-- Help Text -->
          <div class="help-section">
            <p><strong>💡 Besoin d'aide?</strong></p>
            <ul>
              <li>📱 Code reçu par SMS ou papier de l'agent</li>
              <li>🔐 Choisissez un mot de passe sécurisé (min 8 caractères)</li>
              <li>⏱️ Code valide 24 heures, utilisable UNE FOIS</li>
              <li>✅ Après activation, connectez-vous avec vos identifiants</li>
            </ul>
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Footer Link -->
      <div class="footer-link">
        <p>Déjà activé? <a routerLink="/login">Se connecter</a></p>
      </div>
    </div>
  `,
  styles: [`
    .activation-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      padding: 20px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    .activation-card {
      width: 100%;
      max-width: 500px;
      border-radius: 8px;
      box-shadow: 0 10px 40px rgba(0,0,0,0.3);
    }

    mat-card-header {
      text-align: center;
      margin-bottom: 20px;
      padding-bottom: 20px;
      border-bottom: 2px solid #f5f5f5;
    }

    mat-card-header h1 {
      margin: 0 0 8px 0;
      color: #667eea;
      font-size: 24px;
      font-weight: 600;
    }

    .subtitle {
      margin: 0;
      color: #999;
      font-size: 14px;
    }

    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }

    .message-box {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      border-radius: 4px;
      margin-bottom: 16px;
      font-size: 14px;

      mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }
    }

    .error-message {
      background: #ffebee;
      color: #c62828;
      border-left: 4px solid #c62828;
    }

    .success-message {
      background: #e8f5e9;
      color: #2e7d32;
      border-left: 4px solid #2e7d32;
    }

    button[disabled] {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .spinner {
      animation: spin 2s linear infinite;
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }

    .help-section {
      background: #f5f5f5;
      padding: 16px;
      border-radius: 4px;
      margin-top: 20px;
    }

    .help-section p {
      margin: 0 0 12px 0;
      font-weight: 600;
      color: #333;
      font-size: 13px;
    }

    .help-section ul {
      margin: 0;
      padding-left: 20px;
      font-size: 13px;
      color: #666;
    }

    .help-section li {
      margin: 6px 0;
      line-height: 1.4;
    }

    .footer-link {
      text-align: center;
      margin-top: 20px;
    }

    .footer-link p {
      color: white;
      margin: 0;
    }

    .footer-link a {
      color: #ffd700;
      text-decoration: none;
      font-weight: 600;
    }

    .footer-link a:hover {
      text-decoration: underline;
    }
  `]
})
export class MemberActivationComponent implements OnInit {
  
  private fb = inject(FormBuilder);
  private router = inject(Router);
  // TODO: Inject AuthService to call activation endpoint

  showPassword = false;
  loading = false;
  error = '';
  success = '';

  form = this.fb.group({
    activationCode: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]]
  });

  ngOnInit() {
    // TODO: Check if user already logged in -> redirect to dashboard
  }

  submitActivation() {
    if (this.form.invalid) {
      this.error = 'Veuillez remplir tous les champs correctement';
      return;
    }

    // Valider que les passwords correspondent
    if (this.form.get('newPassword')?.value !== this.form.get('confirmPassword')?.value) {
      this.error = 'Les mots de passe ne correspondent pas';
      return;
    }

    this.loading = true;
    this.error = '';
    this.success = '';

    const request: ActivationRequest = {
      activationCode: this.form.get('activationCode')?.value || '',
      newPassword: this.form.get('newPassword')?.value || '',
      confirmPassword: this.form.get('confirmPassword')?.value || ''
    };

    // TODO: Call activationService.activate(request)
    // apiService.post('/api/auth/activate', request).subscribe({
    //   next: (response: ActivationResponse) => {
    //     this.success = response.message;
    //     // Store JWT token
    //     // Redirect to dashboard after 2 sec
    //     setTimeout(() => {
    //       this.router.navigate(['/dashboard']);
    //     }, 2000);
    //   },
    //   error: (err) => {
    //     this.loading = false;
    //     this.error = err.error?.message || 'Erreur lors de l\'activation';
    //   },
    //   complete: () => {
    //     this.loading = false;
    //   }
    // });
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }
}
