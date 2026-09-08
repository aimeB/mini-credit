import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse, HttpRequest } from '@angular/common/http';
import { throwError } from 'rxjs';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

describe('authInterceptor 403 UX', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getToken', 'getCurrentUser']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate'], { url: '/credits' });

    authServiceSpy.getToken.and.returnValue('token');
    authServiceSpy.getCurrentUser.and.returnValue({
      id: 1,
      username: 'agent',
      email: 'agent@x.com',
      nomComplet: 'Agent Terrain',
      role: 'AGENT_TERRAIN'
    } as any);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
  });

  it('affiche un message 403 avec guidance collecte pour AGENT_TERRAIN', (done) => {
    const req = new HttpRequest('POST', '/api/operations-epargne', null);

    TestBed.runInInjectionContext(() => {
      authInterceptor(req, () =>
        throwError(() => new HttpErrorResponse({ status: 403, url: '/api/operations-epargne' }))
      ).subscribe({
        next: () => fail('should error'),
        error: () => {
          expect(routerSpy.navigate).toHaveBeenCalled();
          const navigateArgs = routerSpy.navigate.calls.mostRecent().args;
          const queryParams = navigateArgs[1]?.queryParams;
          expect(navigateArgs[0]).toEqual(['/access-denied']);
          expect(queryParams?.['title']).toContain('Vous ne pouvez pas effectuer cette action');
          expect(queryParams?.['detail']).toContain('Ma collecte du jour');
          done();
        }
      });
    });
  });

  it('affiche un message 403 generic pour role non terrain', (done) => {
    authServiceSpy.getCurrentUser.and.returnValue({
      id: 2,
      username: 'admin',
      email: 'admin@x.com',
      nomComplet: 'Admin',
      role: 'ADMIN'
    } as any);

    const req = new HttpRequest('POST', '/api/caisses', null);

    TestBed.runInInjectionContext(() => {
      authInterceptor(req, () =>
        throwError(() => new HttpErrorResponse({ status: 403, url: '/api/caisses' }))
      ).subscribe({
        next: () => fail('should error'),
        error: () => {
          const queryParams = routerSpy.navigate.calls.mostRecent().args[1]?.queryParams;
          expect(queryParams?.['title']).toContain('Vous ne pouvez pas effectuer cette action');
          expect((queryParams?.['detail'] as string) || '').toContain('Caissier');
          done();
        }
      });
    });
  });

  it('ne redirige pas pour une erreur non 403', (done) => {
    const req = new HttpRequest('GET', '/api/membres');

    TestBed.runInInjectionContext(() => {
      authInterceptor(req, () =>
        throwError(() => new HttpErrorResponse({ status: 500, url: '/api/membres' }))
      ).subscribe({
        next: () => fail('should error'),
        error: (err: HttpErrorResponse) => {
          expect(err.status).toBe(500);
          expect(routerSpy.navigate).not.toHaveBeenCalled();
          done();
        }
      });
    });
  });
});
