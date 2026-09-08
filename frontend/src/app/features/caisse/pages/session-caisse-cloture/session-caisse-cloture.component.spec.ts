import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/services/auth.service';
import { SessionCaisseService } from '../../services/session-caisse.service';
import { SessionCaisseClotureComponent } from './session-caisse-cloture.component';

describe('SessionCaisseClotureComponent', () => {
  let component: SessionCaisseClotureComponent;
  let fixture: ComponentFixture<SessionCaisseClotureComponent>;
  let sessionServiceSpy: jasmine.SpyObj<SessionCaisseService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    sessionServiceSpy = jasmine.createSpyObj<SessionCaisseService>('SessionCaisseService', ['getById', 'cloturer']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);
    sessionServiceSpy.getById.and.returnValue(of({
      id: 1,
      caisseId: 1,
      caisseCode: 'CAI001',
      utilisateurId: 2,
      utilisateurNom: 'Caissier A',
      siteNom: 'Site 1',
      caissierResponsableNom: 'Resp Caisse',
      dateOuverture: '2026-06-16T08:00:00',
      soldeOuverture: 1000,
      totalEntrees: 500,
      totalSorties: 200,
      soldeTheorique: 1300,
      statut: 'OUVERTE',
      createdAt: '',
      updatedAt: ''
    } as any));
    sessionServiceSpy.cloturer.and.returnValue(of({ id: 1 } as any));

    authServiceSpy.getCurrentUser.and.returnValue({
      id: 10,
      username: 'cashier',
      nomComplet: 'Caissier Connecté',
      email: 'cash@x.com',
      role: 'CAISSIER'
    } as any);

    await TestBed.configureTestingModule({
      imports: [SessionCaisseClotureComponent],
      providers: [
        provideRouter([]),
        { provide: SessionCaisseService, useValue: sessionServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }) } }
        }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(SessionCaisseClotureComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('affiche le solde théorique calculé', () => {
    expect(component.soldeTheoriqueCalcule).toBe(1300);
  });

  it('calcule l\'écart automatiquement', () => {
    component.form.patchValue({ soldePhysique: 1200 });
    expect(component.ecartCalcule).toBe(-100);
  });

  it('exige observation si écart', () => {
    component.form.patchValue({ soldePhysique: 1200, observation: '' });
    component.cloturerSession();
    expect(component.form.get('observation')?.hasError('requiredOnEcart')).toBeTrue();
  });

  it('désactive clôturer si formulaire invalide', () => {
    component.form.patchValue({ soldePhysique: null });
    fixture.detectChanges();
    const button: HTMLButtonElement | null = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(button?.disabled).toBeTrue();
  });

  it('redirige vers détail session après clôture', () => {
    component.form.patchValue({ soldePhysique: 1300, observation: '' });
    component.cloturerSession();
    expect(router.navigate).toHaveBeenCalledWith(
      ['/caisses/sessions', 1],
      { state: { successMessage: 'Session transmise au contrôle du Contrôleur.' } }
    );
  });

  it('affiche une guidance explicite de transmission au contrôle', () => {
    const content = fixture.nativeElement.textContent as string;
    expect(content).toContain('Soumettre la session au contrôle');
    expect(content).toContain('Cette action ne clôture pas définitivement la session');
    expect(content).toContain('Soumettre au contrôle');
    expect(content).not.toContain('Clôturer session');
  });

  it('gère erreur de clôture', () => {
    sessionServiceSpy.cloturer.and.returnValue(throwError(() => ({ error: { message: 'Erreur clôture' } })));
    component.form.patchValue({ soldePhysique: 1300, observation: '' });
    component.cloturerSession();
    expect(component.error).toContain('Erreur clôture');
  });
});
