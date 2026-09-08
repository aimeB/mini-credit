import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthService } from '../../../../core/services/auth.service';
import { SessionCaisseService } from '../../services/session-caisse.service';
import { CaisseControleComponent } from './caisse-controle.component';

describe('CaisseControleComponent', () => {
  let component: CaisseControleComponent;
  let fixture: ComponentFixture<CaisseControleComponent>;
  let sessionServiceSpy: jasmine.SpyObj<SessionCaisseService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  const sessionsMock = [
    {
      id: 1,
      caisseCode: 'CAI001',
      siteNom: 'Site A',
      utilisateurNom: 'Caissier A',
      dateOuverture: '2026-06-16T08:00:00',
      dateCloture: '2026-06-16T17:00:00',
      soldeTheorique: 1300,
      soldePhysique: 1290,
      ecartCaisse: -10,
      statut: 'CLOTUREE'
    }
  ];

  beforeEach(async () => {
    sessionServiceSpy = jasmine.createSpyObj<SessionCaisseService>('SessionCaisseService', ['getAll', 'validerControle']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['hasAnyRole']);

    sessionServiceSpy.getAll.and.returnValue(of(sessionsMock as any));
    sessionServiceSpy.validerControle.and.returnValue(of({ id: 1 } as any));
    authServiceSpy.hasAnyRole.and.returnValue(true);

    await TestBed.configureTestingModule({
      imports: [CaisseControleComponent],
      providers: [
        provideRouter([]),
        { provide: SessionCaisseService, useValue: sessionServiceSpy },
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CaisseControleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('affiche les sessions à contrôler', () => {
    expect(component.sessionsFermees.length).toBe(1);
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('CAI001');
  });

  it('bouton valider visible pour CONTROLEUR/ADMIN', () => {
    authServiceSpy.hasAnyRole.and.returnValue(true);
    fixture.detectChanges();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Valider contrôle');
  });

  it('bouton valider absent pour CAISSIER', () => {
    authServiceSpy.hasAnyRole.and.returnValue(false);
    fixture.detectChanges();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).not.toContain('Valider contrôle');
  });

  it('formatte les montants en CDF', () => {
    expect(component.formatCdf(1000)).toContain('CDF');
  });
});
