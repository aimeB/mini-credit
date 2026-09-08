import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { PasswordChangeService } from '../../core/services/password-change.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './change-password.component.html'
})
export class ChangePasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly passwordService = inject(PasswordChangeService);
  private readonly router = inject(Router);

  readonly form = this.fb.group({
    oldPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]]
  });

  loading = false;
  errorMessage = '';
  successMessage = '';

  submit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.id) {
      this.errorMessage = 'Session invalide. Veuillez vous reconnecter.';
      return;
    }

    const { oldPassword, newPassword, confirmPassword } = this.form.getRawValue();
    if (newPassword !== confirmPassword) {
      this.errorMessage = 'Les mots de passe ne correspondent pas.';
      return;
    }

    this.loading = true;

    this.passwordService.changePassword(currentUser.id, {
      oldPassword: oldPassword!,
      currentPassword: oldPassword!,
      newPassword: newPassword!,
      confirmPassword: confirmPassword!
    }).subscribe({
      next: () => {
        this.loading = false;
        this.authService.clearPasswordChangeRequired();
        this.successMessage = 'Mot de passe changé avec succès.';
        setTimeout(() => this.router.navigate(['/dashboard']), 500);
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error?.error?.message || error?.message || 'Erreur lors du changement de mot de passe.';
      }
    });
  }
}
