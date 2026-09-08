import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { UsernameService } from '../../../core/services/username.service';
import { Subject, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';

/**
 * Composant pour afficher et permettre à l'utilisateur de changer son username
 * Accessible depuis le profil utilisateur ou le dashboard
 */
@Component({
  selector: 'app-change-username',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="username-card">
      <div class="card-header">
        <h3>👤 Identifiant (Username)</h3>
        <p class="subtitle">Votre nom d'utilisateur unique pour vous connecter</p>
      </div>

      <div class="card-content">
        <!-- Affichage du username actuel -->
        <div class="current-username" *ngIf="!isEditing">
          <div class="username-display">
            <label>Votre username actuel:</label>
            <div class="username-value">{{ currentUsername }}</div>
          </div>
          <button (click)="toggleEdit()" class="btn-edit">
            ✏️ Modifier
          </button>
        </div>

        <!-- Formulaire d'édition -->
        <form [formGroup]="usernameForm" (ngSubmit)="onSubmit()" *ngIf="isEditing" class="edit-form">
          <div class="form-group">
            <label for="newUsername">Nouveau username:</label>
            <input
              type="text"
              id="newUsername"
              formControlName="newUsername"
              placeholder="Ex: john.doe"
              class="form-control"
              [class.error]="isFieldInvalid('newUsername')"
              (blur)="markFieldAsTouched('newUsername')"
            />
            
            <!-- Validation messages -->
            <div *ngIf="isFieldInvalid('newUsername')" class="error-message">
              <span *ngIf="usernameForm.get('newUsername')?.hasError('required')">
                ❌ Le username est obligatoire
              </span>
              <span *ngIf="usernameForm.get('newUsername')?.hasError('minlength')">
                ❌ Le username doit avoir au moins 3 caractères
              </span>
              <span *ngIf="usernameForm.get('newUsername')?.hasError('maxlength')">
                ❌ Le username ne peut pas dépasser 50 caractères
              </span>
              <span *ngIf="usernameForm.get('newUsername')?.hasError('pattern')">
                ❌ Le username ne peut contenir que des lettres, chiffres, points, tirets et underscores
              </span>
              <span *ngIf="usernameForm.get('newUsername')?.hasError('usernameTaken')">
                ❌ Ce username est déjà utilisé. Veuillez en choisir un autre.
              </span>
            </div>

            <!-- Suggestion de disponibilité -->
            <div *ngIf="usernameForm.get('newUsername')?.valid && !usernameForm.get('newUsername')?.pending" class="success-message">
              ✅ Ce username est disponible!
            </div>
            <div *ngIf="usernameForm.get('newUsername')?.pending" class="checking-message">
              ⏳ Vérification de la disponibilité...
            </div>
          </div>

          <div class="form-actions">
            <button type="submit" class="btn-save" [disabled]="!usernameForm.valid || isLoading">
              <span *ngIf="!isLoading">💾 Confirmer</span>
              <span *ngIf="isLoading">⏳ Changement en cours...</span>
            </button>
            <button type="button" (click)="toggleEdit()" class="btn-cancel">
              ✖️ Annuler
            </button>
          </div>
        </form>

        <!-- Messages de succès/erreur -->
        <div *ngIf="successMessage" class="alert alert-success">
          {{ successMessage }}
        </div>
        <div *ngIf="errorMessage" class="alert alert-error">
          {{ errorMessage }}
        </div>
      </div>

      <div class="info-box">
        <p>
          <strong>ℹ️ Information:</strong><br>
          Votre username est votre identifiant unique pour vous connecter à l'application.
          Il doit être unique - deux utilisateurs ne peuvent pas avoir le même username.
          Vous pouvez le changer à tout moment.
        </p>
      </div>
    </div>
  `,
  styles: [`
    .username-card {
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      padding: 24px;
      max-width: 500px;
      margin: 20px auto;
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    }

    .card-header {
      text-align: center;
      margin-bottom: 24px;
      padding-bottom: 16px;
      border-bottom: 2px solid #667eea;
    }

    .card-header h3 {
      margin: 0 0 8px 0;
      color: #333;
      font-size: 20px;
    }

    .subtitle {
      margin: 0;
      color: #666;
      font-size: 14px;
    }

    .card-content {
      margin-bottom: 24px;
    }

    .current-username {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      background: #f5f5f5;
      border-radius: 8px;
      margin-bottom: 16px;
    }

    .username-display {
      flex: 1;
    }

    .username-display label {
      display: block;
      color: #666;
      font-size: 12px;
      margin-bottom: 4px;
      font-weight: 600;
    }

    .username-value {
      font-size: 18px;
      font-weight: 600;
      color: #333;
      font-family: 'Courier New', monospace;
      background: white;
      padding: 8px 12px;
      border-radius: 4px;
      border: 1px solid #ddd;
    }

    .btn-edit {
      padding: 8px 16px;
      background: #667eea;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 13px;
      font-weight: 500;
      margin-left: 12px;
      transition: all 0.2s ease;
    }

    .btn-edit:hover {
      background: #5568d3;
      transform: translateY(-2px);
    }

    .edit-form {
      padding: 16px;
      background: #f9f9f9;
      border-radius: 8px;
      border: 1px solid #e0e0e0;
    }

    .form-group {
      margin-bottom: 16px;
    }

    .form-group label {
      display: block;
      font-weight: 600;
      color: #333;
      margin-bottom: 8px;
      font-size: 13px;
    }

    .form-control {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 13px;
      font-family: 'Courier New', monospace;
      transition: all 0.2s ease;
      box-sizing: border-box;
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

    .error-message {
      color: #e74c3c;
      font-size: 11px;
      margin-top: 6px;
      line-height: 1.4;
    }

    .error-message span {
      display: block;
    }

    .success-message {
      color: #27ae60;
      font-size: 11px;
      margin-top: 6px;
      font-weight: 500;
    }

    .checking-message {
      color: #f39c12;
      font-size: 11px;
      margin-top: 6px;
      font-weight: 500;
    }

    .form-actions {
      display: flex;
      gap: 12px;
      margin-top: 16px;
    }

    .btn-save,
    .btn-cancel {
      flex: 1;
      padding: 10px 16px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 13px;
      font-weight: 500;
      transition: all 0.2s ease;
    }

    .btn-save {
      background: #27ae60;
      color: white;
    }

    .btn-save:hover:not(:disabled) {
      background: #229954;
      transform: translateY(-2px);
    }

    .btn-save:disabled {
      background: #95a5a6;
      cursor: not-allowed;
      opacity: 0.7;
    }

    .btn-cancel {
      background: #e8e8e8;
      color: #333;
    }

    .btn-cancel:hover {
      background: #d5d5d5;
    }

    .alert {
      padding: 12px 16px;
      border-radius: 4px;
      font-size: 13px;
      margin: 16px 0;
      line-height: 1.4;
    }

    .alert-success {
      background-color: #e8f8f5;
      color: #27ae60;
      border: 1px solid #a9dfbf;
    }

    .alert-error {
      background-color: #fff5f5;
      color: #e74c3c;
      border: 1px solid #f5c6cb;
    }

    .info-box {
      background: #ecf0f1;
      border-left: 4px solid #667eea;
      padding: 12px 16px;
      border-radius: 4px;
      font-size: 12px;
      color: #333;
      line-height: 1.5;
    }

    .info-box p {
      margin: 0;
    }

    @media (max-width: 480px) {
      .username-card {
        padding: 16px;
      }

      .current-username {
        flex-direction: column;
        align-items: flex-start;
      }

      .btn-edit {
        margin-left: 0;
        margin-top: 12px;
        width: 100%;
      }

      .form-actions {
        flex-direction: column;
      }
    }
  `]
})
export class ChangeUsernameComponent implements OnInit, OnDestroy {
  usernameForm: FormGroup;
  isEditing = false;
  isLoading = false;
  currentUsername = '';
  successMessage = '';
  errorMessage = '';

  private destroy$ = new Subject<void>();
  private usernameCheckSubject$ = new Subject<string>();

  private authService = inject(AuthService);
  private usernameService = inject(UsernameService);
  private fb = inject(FormBuilder);

  ngOnInit() {
    // Initialiser le formulaire
    this.usernameForm = this.fb.group({
      newUsername: [
        '',
        [
          Validators.required,
          Validators.minLength(3),
          Validators.maxLength(50),
          Validators.pattern(/^[a-z0-9._-]+$/)
        ],
        [this.usernameAvailabilityValidator.bind(this)]
      ]
    });

    // Obtenir l'utilisateur actuel
    this.authService.currentUser$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(user => {
      if (user) {
        this.currentUsername = user.username;
      }
    });

    // Valider la disponibilité en temps réel lors de la saisie
    this.usernameCheckSubject$.pipe(
      debounceTime(500),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(username => {
      // La validation est gérée par le validateur asynchrone
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Validateur asynchrone pour vérifier la disponibilité du username
   */
  private usernameAvailabilityValidator(control: AbstractControl) {
    if (!control.value) {
      return null; // Ne pas valider si vide
    }

    return new Promise<ValidationErrors | null>(resolve => {
      this.usernameService.checkUsernameAvailability(control.value)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (response) => {
            if (response.available || response.username === this.currentUsername) {
              resolve(null); // Username disponible
            } else {
              resolve({ usernameTaken: true }); // Username déjà utilisé
            }
          },
          error: () => {
            resolve(null); // En cas d'erreur, laisser passer
          }
        });
    });
  }

  toggleEdit() {
    this.isEditing = !this.isEditing;
    this.successMessage = '';
    this.errorMessage = '';
    
    if (!this.isEditing) {
      this.usernameForm.reset();
    }
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.usernameForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  markFieldAsTouched(fieldName: string) {
    const field = this.usernameForm.get(fieldName);
    if (field) {
      field.markAsTouched();
    }
  }

  onSubmit() {
    if (!this.usernameForm.valid) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const userId = this.authService.getCurrentUser()?.id;
    if (!userId) {
      this.errorMessage = 'Erreur: Utilisateur non trouvé';
      this.isLoading = false;
      return;
    }

    const request = {
      newUsername: this.usernameForm.get('newUsername')?.value
    };

    this.usernameService.changeUsername(userId, request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.isLoading = false;
          this.successMessage = `✅ Username changé avec succès! Votre nouveau username est "${response.username}"`;
          this.currentUsername = response.username;
          
          // Rafraîchir les données utilisateur dans le service d'authentification
          this.authService.getCurrentUser()?.username;
          
          // Fermer le formulaire après succès
          setTimeout(() => {
            this.isEditing = false;
            this.usernameForm.reset();
          }, 1500);
        },
        error: (err) => {
          this.isLoading = false;
          this.errorMessage = err?.error?.message || 'Erreur lors du changement de username';
          console.error('Error changing username:', err);
        }
      });
  }
}
