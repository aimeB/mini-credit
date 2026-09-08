import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {

  private readonly validRoles = new Set([
    'ADMIN',
    'AGENT_TERRAIN',
    'GESTIONNAIRE',
    'CONTROLEUR',
    'CAISSIER',
    'CHEF_BUREAU',
    'COO',
    'RCI',
    'GERANT_GENERAL',
    'MEMBER'
  ]);

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/auth/login']);
      return false;
    }

    const requiredPermissions = route.data['permissions'] as string[] | undefined;
    if (requiredPermissions && requiredPermissions.length > 0) {
      const currentUser = this.authService.getCurrentUser();
      const userPermissions = currentUser?.permissions ?? [];

      if (userPermissions.length > 0) {
        if (!this.authService.hasAnyPermission(requiredPermissions)) {
          this.router.navigate(['/access-denied']);
          return false;
        }
      }
    }

    const requiredRoles = route.data['roles'] as string[] | undefined;
    if (!requiredRoles || requiredRoles.length === 0) {
      return true;
    }

    const currentRole = this.normalizeRole(this.authService.getCurrentUser()?.role);
    if (currentRole && !this.validRoles.has(currentRole)) {
      console.warn(`RoleGuard: role inconnu recu: ${currentRole}`);
    }

    if (this.authService.hasAnyRole(requiredRoles)) {
      return true;
    }

    this.router.navigate(['/access-denied']);
    return false;
  }

  private normalizeRole(role: string | null | undefined): string {
    const normalized = (role ?? '').trim().toUpperCase();
    return normalized.startsWith('ROLE_') ? normalized.slice(5) : normalized;
  }
}