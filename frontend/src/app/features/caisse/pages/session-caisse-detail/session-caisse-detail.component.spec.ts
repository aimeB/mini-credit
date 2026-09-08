import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

import { SessionCaisseDetailComponent } from './session-caisse-detail.component';

describe('SessionCaisseDetailComponent', () => {
  let component: SessionCaisseDetailComponent;
  let fixture: ComponentFixture<SessionCaisseDetailComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['hasAnyRole', 'hasPermission', 'hasAnyPermission', 'getCurrentUser']);
    authServiceSpy.hasAnyRole.and.returnValue(true);
    authServiceSpy.hasPermission.and.returnValue(true);
    authServiceSpy.hasAnyPermission.and.returnValue(false);
    authServiceSpy.getCurrentUser.and.returnValue({ role: 'CONTROLEUR', permissions: [] } as any);

    await TestBed.configureTestingModule({
      imports: [SessionCaisseDetailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SessionCaisseDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('autorise la validation du contrôle pour le Contrôleur', () => {
    authServiceSpy.hasAnyRole.and.returnValue(true);

    expect(component.canValidateControl()).toBeTrue();
  });

  it('n\'affiche pas la saisie d\'opérations caisse pour un contrôleur', () => {
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CONTROLEUR'));
    component.session = {
      id: 1,
      statut: 'OUVERTE',
      dateComptable: new Date().toISOString().slice(0, 10)
    } as any;

    expect(component.canAddOperations()).toBeFalse();
  });

  it('n\'affiche pas la saisie d\'opérations caisse pour un caissier', () => {
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CAISSIER'));
    component.session = {
      id: 1,
      statut: 'OUVERTE',
      dateComptable: new Date().toISOString().slice(0, 10)
    } as any;

    expect(component.canAddOperations()).toBeFalse();
  });

  it('affiche la saisie d\'opérations caisse pour Chef Bureau ou RCI sur session ouverte du jour', () => {
    component.session = {
      id: 1,
      statut: 'OUVERTE',
      dateComptable: new Date().toISOString().slice(0, 10)
    } as any;

    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CHEF_BUREAU'));
    expect(component.canAddOperations()).toBeTrue();

    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('RCI'));
    expect(component.canAddOperations()).toBeTrue();
  });

  it('autorise la clôture finale seulement avec rôle Chef Bureau et permission dédiée', () => {
    component.session = { id: 1, statut: 'VALIDEE_CONTROLE' } as any;
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CHEF_BUREAU'));
    authServiceSpy.hasPermission.and.returnValue(true);

    expect(component.canFinalClose()).toBeTrue();

    authServiceSpy.hasPermission.and.returnValue(false);
    expect(component.canFinalClose()).toBeFalse();
  });
});
