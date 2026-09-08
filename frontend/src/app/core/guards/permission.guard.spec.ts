import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';

import { PermissionGuard } from './permission.guard';
import { AuthService } from '../services/auth.service';

describe('PermissionGuard', () => {
  let guard: PermissionGuard;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const state = { url: '/mes-actions' } as RouterStateSnapshot;

  function buildRoute(data: Record<string, unknown>): ActivatedRouteSnapshot {
    return { data } as ActivatedRouteSnapshot;
  }

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', [
      'isAuthenticated',
      'hasAnyRole',
      'hasAnyPermission'
    ]);

    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        PermissionGuard,
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });

    guard = TestBed.inject(PermissionGuard);
  });

  it('autorise un utilisateur avec TASK_READ_OWN', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.hasAnyRole.and.returnValue(false);
    authServiceSpy.hasAnyPermission.and.callFake((permissions: string[]) => permissions.includes('TASK_READ_OWN'));

    const route = buildRoute({
      permissions: ['TASK_READ_OWN', 'TASK_READ_ANTENNE', 'TASK_SUPERVISE', 'TASK_AUDIT'],
      permissionBypassRoles: ['ADMIN']
    });

    const result = guard.canActivate(route, state);

    expect(result).toBeTrue();
    expect(authServiceSpy.hasAnyPermission).toHaveBeenCalledWith([
      'TASK_READ_OWN', 'TASK_READ_ANTENNE', 'TASK_SUPERVISE', 'TASK_AUDIT'
    ]);
  });

  it('autorise un utilisateur avec TASK_AUDIT', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.hasAnyRole.and.returnValue(false);
    authServiceSpy.hasAnyPermission.and.callFake((permissions: string[]) => permissions.includes('TASK_AUDIT'));

    const route = buildRoute({
      permissions: ['TASK_READ_OWN', 'TASK_READ_ANTENNE', 'TASK_SUPERVISE', 'TASK_AUDIT'],
      permissionBypassRoles: ['ADMIN']
    });

    const result = guard.canActivate(route, state);

    expect(result).toBeTrue();
  });

  it('refuse un utilisateur sans permission TASK', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.hasAnyRole.and.returnValue(false);
    authServiceSpy.hasAnyPermission.and.returnValue(false);

    const route = buildRoute({
      permissions: ['TASK_READ_OWN', 'TASK_READ_ANTENNE', 'TASK_SUPERVISE', 'TASK_AUDIT'],
      permissionBypassRoles: ['ADMIN']
    });

    const result = guard.canActivate(route, state);

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/access-denied']);
  });
});
