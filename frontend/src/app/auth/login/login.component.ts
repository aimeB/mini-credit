import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { AuthService, LoginRequest } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  loginForm: FormGroup;
  loading = false;
  error: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.loading = true;
      this.error = null;

      const credentials: LoginRequest = this.loginForm.value;

      this.authService.login(credentials)
        .pipe(finalize(() => this.loading = false))
        .subscribe({
          next: (response) => {
            if (response.passwordChangeRequired) {
              this.router.navigate(['/auth/change-password']);
              return;
            }

            // Redirection selon le rôle
            if (response.role === 'ADMIN' || response.role === 'CHEF_BUREAU' || response.role === 'GESTIONNAIRE') {
              this.router.navigate(['/dashboard']);
            } else if (response.role === 'CAISSIER') {
              this.router.navigate(['/caisses']);
            } else if (response.role === 'AGENT_TERRAIN') {
              this.router.navigate(['/membres']);
            } else {
              this.router.navigate(['/dashboard']);
            }
          },
          error: (error) => {
            this.error = error.message;
          }
        });
    } else {
      this.markFormGroupTouched();
    }
  }

  private markFormGroupTouched(): void {
    Object.keys(this.loginForm.controls).forEach(key => {
      const control = this.loginForm.get(key);
      control?.markAsTouched();
    });
  }

  getFieldError(fieldName: string): string {
    const control = this.loginForm.get(fieldName);
    if (control?.errors && control.touched) {
      if (control.errors['required']) {
        return `${fieldName.charAt(0).toUpperCase() + fieldName.slice(1)} est requis`;
      }
    }
    return '';
  }
}
