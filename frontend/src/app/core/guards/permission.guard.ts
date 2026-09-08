import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class PermissionGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/auth/login']);
      return false;
    }

    const bypassRoles = (route.data['permissionBypassRoles'] as string[] | undefined) ?? ['ADMIN'];
    if (bypassRoles.length > 0 && this.authService.hasAnyRole(bypassRoles)) {
      return true;
    }

    const requiredPermissions = route.data['permissions'] as string[] | undefined;
    if (!requiredPermissions || requiredPermissions.length === 0) {
      return true;
    }

    if (this.authService.hasAnyPermission(requiredPermissions)) {
      return true;
    }

    this.router.navigate(['/access-denied']);
    return false;
  }
}
