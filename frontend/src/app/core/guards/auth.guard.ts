import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(): boolean {
    if (this.authService.isAuthenticated()) {
      const currentUrl = this.router.url || '';
      if (this.authService.mustChangePassword() && !currentUrl.startsWith('/auth/change-password')) {
        this.router.navigate(['/auth/change-password']);
        return false;
      }
      return true;
    } else {
      this.router.navigate(['/auth/login']);
      return false;
    }
  }
}