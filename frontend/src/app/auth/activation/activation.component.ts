import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-activation',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="activation-container">
      <div class="activation-card">
        <div class="card-header">
          <h1>🔐 Activation du Compte</h1>
          <p class="subtitle" *ngIf="!codePreFilled">
            Complétez votre inscription avec votre code d'activation
          </p>
          <p class="subtitle" *ngIf="codePreFilled">
            ✅ Code d'activation détecté ! Choisissez votre mot de passe
          </p>
        </div>

        <form [formGroup]="activationForm" (ngSubmit)="onSubmit()" class="activation-form">
          <!-- Code d'activation -->
          <div class="form-group">
            <label for="activationCode" class="form-label">
              <span class="required">*</span> Code d'activation
              <span *ngIf="codePreFilled" class="auto-detected">✓ Auto-détecté</span>
            </label>
            <input
              type="text"
              id="activationCode"
              formControlName="activationCode"
              placeholder="Entrez le code reçu"
              class="form-control"
              [readonly]="codePreFilled"
              [class.error]="isFieldInvalid('activationCode')"
              [class.readonly]="codePreFilled"
              (blur)="markFieldAsTouched('activationCode')"
            />
            <div *ngIf="isFieldInvalid('activationCode')" class="error-message">
              Le code d'activation est obligatoire
            </div>
          </div>

          <!-- Nouveau mot de passe -->
          <div class="form-group">
            <label for="newPassword" class="form-label">
              <span class="required">*</span> Nouveau mot de passe
            </label>
            <div class="password-input-wrapper">
              <input
                [type]="showPassword ? 'text' : 'password'"
                id="newPassword"
                formControlName="newPassword"
                placeholder="Minimum 8 caractères"
                class="form-control"
                [class.error]="isFieldInvalid('newPassword')"
                (blur)="markFieldAsTouched('newPassword')"
              />
              <button
                type="button"
                class="btn-toggle-password"
                (click)="togglePasswordVisibility()"
                title="Afficher/Masquer le mot de passe"
              >
                {{ showPassword ? '👁️ Masquer' : '👁️ Voir' }}
              </button>
            </div>
            <div *ngIf="isFieldInvalid('newPassword')" class="error-message">
              {{ getPasswordError() }}
            </div>
          </div>

          <!-- Confirmation mot de passe -->
          <div class="form-group">
            <label for="confirmPassword" class="form-label">
              <span class="required">*</span> Confirmer le mot de passe
            </label>
            <div class="password-input-wrapper">
              <input
                [type]="showPassword ? 'text' : 'password'"
                id="confirmPassword"
                formControlName="confirmPassword"
                placeholder="Ressaisir votre mot de passe"
                class="form-control"
                [class.error]="isFieldInvalid('confirmPassword')"
                (blur)="markFieldAsTouched('confirmPassword')"
              />
              <button
                type="button"
                class="btn-toggle-password"
                (click)="togglePasswordVisibility()"
                title="Afficher/Masquer le mot de passe"
              >
                {{ showPassword ? '👁️ Masquer' : '👁️ Voir' }}
              </button>
            </div>
            <div *ngIf="isFieldInvalid('confirmPassword')" class="error-message">
              {{ getConfirmPasswordError() }}
            </div>
          </div>

          <!-- Messages d'erreur/succès -->
          <div *ngIf="errorMessage" class="alert alert-error">
            <span class="icon">❌</span>
            {{ errorMessage }}
          </div>

          <div *ngIf="successMessage" class="alert alert-success">
            <span class="icon">✅</span>
            {{ successMessage }}
          </div>

          <!-- Bouton submit -->
          <button
            type="submit"
            class="btn-submit"
            [disabled]="!activationForm.valid || isLoading"
          >
            <span *ngIf="!isLoading">🔓 Activer mon compte</span>
            <span *ngIf="isLoading">⏳ Activation en cours...</span>
          </button>
        </form>

        <!-- Lien vers login -->
        <div class="login-link">
          <p>Vous avez déjà un compte ? <a (click)="goToLogin()">Se connecter</a></p>
        </div>
      </div>

      <!-- Informations d'aide -->
      <div class="help-section">
        <h3>ℹ️ Besoin d'aide ?</h3>
        <ul>
          <li>✓ Le code d'activation a été envoyé par SMS ou communiqué par l'agent</li>
          <li>✓ Le code est valide pendant 48 heures</li>
          <li>✓ Votre mot de passe doit contenir au moins 8 caractères</li>
          <li>✓ Après activation, vous pourrez vous connecter normalement</li>
        </ul>
      </div>
    </div>
  `,
  styles: [`
    .activation-container {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      padding: 20px;
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    }

    .activation-card {
      background: white;
      border-radius: 12px;
      box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
      width: 100%;
      max-width: 420px;
      padding: 40px;
    }

    .card-header {
      text-align: center;
      margin-bottom: 30px;
    }

    .card-header h1 {
      margin: 0 0 10px 0;
      color: #333;
      font-size: 28px;
      font-weight: 600;
    }

    .subtitle {
      margin: 0;
      color: #666;
      font-size: 14px;
      line-height: 1.5;
    }

    .activation-form {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .form-label {
      font-weight: 600;
      color: #333;
      font-size: 14px;
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .required {
      color: #e74c3c;
      font-size: 16px;
    }

    .auto-detected {
      background: #e8f5e9;
      color: #27ae60;
      padding: 2px 8px;
      border-radius: 3px;
      font-size: 11px;
      font-weight: 600;
      margin-left: auto;
    }

    .form-control {
      padding: 12px 16px;
      border: 1px solid #ddd;
      border-radius: 8px;
      font-size: 14px;
      transition: all 0.3s ease;
      font-family: inherit;
    }

    .form-control:focus {
      outline: none;
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }

    .form-control.error {
      border-color: #e74c3c;
      background-color: #fff5f5;
    }

    .form-control.readonly {
      background-color: #f5f5f5;
      border-color: #27ae60;
      color: #27ae60;
      font-weight: 600;
      cursor: default;
    }

    .form-control.readonly:focus {
      border-color: #27ae60;
      box-shadow: 0 0 0 3px rgba(39, 174, 96, 0.1);
    }

    .password-input-wrapper {
      display: flex;
      gap: 8px;
      align-items: stretch;
    }

    .btn-toggle-password {
      padding: 8px 12px;
      background: #f5f5f5;
      color: #666;
      border: 1px solid #ddd;
      border-radius: 6px;
      cursor: pointer;
      font-size: 12px;
      font-weight: 500;
      white-space: nowrap;
      transition: all 0.2s ease;
      flex-shrink: 0;
    }

    .btn-toggle-password:hover {
      background: #e8e8e8;
      border-color: #ccc;
      color: #333;
    }

    .btn-toggle-password:active {
      background: #ddd;
    }

    .error-message {
      color: #e74c3c;
      font-size: 12px;
      margin-top: -4px;
    }

    .alert {
      padding: 12px 16px;
      border-radius: 8px;
      font-size: 14px;
      display: flex;
      align-items: center;
      gap: 8px;
      line-height: 1.4;
    }

    .alert-error {
      background-color: #fff5f5;
      color: #e74c3c;
      border: 1px solid #f5c6cb;
    }

    .alert-success {
      background-color: #f0f9ff;
      color: #27ae60;
      border: 1px solid #c3e6cb;
    }

    .icon {
      font-size: 16px;
    }

    .btn-submit {
      padding: 12px 24px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 15px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s ease;
      margin-top: 10px;
    }

    .btn-submit:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
    }

    .btn-submit:disabled {
      opacity: 0.6;
      cursor: not-allowed;
      transform: none;
    }

    .login-link {
      text-align: center;
      margin-top: 24px;
      padding-top: 20px;
      border-top: 1px solid #eee;
    }

    .login-link p {
      margin: 0;
      color: #666;
      font-size: 14px;
    }

    .login-link a {
      color: #667eea;
      text-decoration: none;
      font-weight: 600;
      cursor: pointer;
      transition: color 0.2s ease;
    }

    .login-link a:hover {
      color: #764ba2;
      text-decoration: underline;
    }

    .help-section {
      margin-top: 40px;
      padding: 20px;
      background: rgba(255, 255, 255, 0.9);
      border-radius: 8px;
      border-left: 4px solid #667eea;
    }

    .help-section h3 {
      margin: 0 0 12px 0;
      color: #333;
      font-size: 16px;
    }

    .help-section ul {
      list-style: none;
      padding: 0;
      margin: 0;
    }

    .help-section li {
      color: #666;
      font-size: 13px;
      margin: 6px 0;
      line-height: 1.5;
    }

    @media (max-width: 480px) {
      .activation-container {
        padding: 10px;
      }

      .activation-card {
        padding: 24px;
      }

      .card-header h1 {
        font-size: 24px;
      }
    }
  `]
})
export class ActivationComponent implements OnInit {
  activationForm: FormGroup;
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  showPassword = false;
  codePreFilled = false; // Pour savoir si le code a été pré-rempli

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.activationForm = this.formBuilder.group({
      activationCode: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    });
  }

  ngOnInit(): void {
    // Récupérer le code d'activation depuis les query params
    this.route.queryParams.subscribe(params => {
      if (params['code']) {
        this.activationForm.patchValue({
          activationCode: params['code']
        });
        this.codePreFilled = true;
        
        // Rendre le code en lecture seule s'il a été pré-rempli
        this.activationForm.get('activationCode')?.disable();
      }
    });
  }

  onSubmit(): void {
    if (!this.activationForm.valid) {
      return;
    }

    // Vérifier que les mots de passe correspondent
    const newPassword = this.activationForm.get('newPassword')?.value;
    const confirmPassword = this.activationForm.get('confirmPassword')?.value;

    if (newPassword !== confirmPassword) {
      this.errorMessage = 'Les mots de passe ne correspondent pas';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    // Utiliser getRawValue() pour inclure les champs disabled
    const { activationCode } = this.activationForm.getRawValue();

    this.authService.activate(activationCode, newPassword, confirmPassword).subscribe({
      next: () => {
        this.successMessage = '✅ Compte activé avec succès ! Redirection...';
        setTimeout(() => {
          // Rediriger selon le rôle
          const currentUser = this.authService.getCurrentUser();
          if (currentUser?.role === 'MEMBER') {
            this.router.navigate(['/member-profile']);
          } else {
            this.router.navigate(['/dashboard']);
          }
        }, 1500);
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = error.message || 'Erreur lors de l\'activation. Vérifiez votre code.';
        console.error('Activation error:', error);
      }
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.activationForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  markFieldAsTouched(fieldName: string): void {
    const field = this.activationForm.get(fieldName);
    if (field) {
      field.markAsTouched();
    }
  }

  getPasswordError(): string {
    const passwordControl = this.activationForm.get('newPassword');
    if (passwordControl?.hasError('required')) {
      return 'Le mot de passe est obligatoire';
    }
    if (passwordControl?.hasError('minlength')) {
      return 'Le mot de passe doit contenir au moins 8 caractères';
    }
    return '';
  }

  getConfirmPasswordError(): string {
    const confirmControl = this.activationForm.get('confirmPassword');
    if (confirmControl?.hasError('required')) {
      return 'La confirmation du mot de passe est obligatoire';
    }
    const newPassword = this.activationForm.get('newPassword')?.value;
    const confirmPassword = confirmControl?.value;
    if (newPassword && confirmPassword && newPassword !== confirmPassword) {
      return 'Les mots de passe ne correspondent pas';
    }
    return '';
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  goToLogin(): void {
    this.router.navigate(['/auth/login']);
  }
}
