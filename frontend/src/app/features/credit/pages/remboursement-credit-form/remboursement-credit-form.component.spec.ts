import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { RemboursementCreditFormComponent } from './remboursement-credit-form.component';
import { CreditService } from '../../services/credit.service';
import { AuthService } from '../../../../core/services/auth.service';

describe('RemboursementCreditFormComponent', () => {
  let fixture: ComponentFixture<RemboursementCreditFormComponent>;

  const creditServiceMock = {
    getById: jasmine.createSpy('getById').and.returnValue(of({
      id: 1,
      numeroCredit: 'CR-RET-001',
      membreId: 99,
      membreNomComplet: 'Membre Retard',
      statut: 'EN_RETARD',
      principalTotal: 1000,
      interetTotal: 100,
      penaliteTotal: 50,
      totalARembourser: 1150,
      encoursPrincipal: 500
    })),
    appliquerPenalites: jasmine.createSpy('appliquerPenalites').and.returnValue(of({})),
    getEcheancesByCreditId: jasmine.createSpy('getEcheancesByCreditId').and.returnValue(of([
      {
        id: 1,
        numeroEcheance: 1,
        dateEcheance: '2026-07-01',
        statut: 'EN_RETARD',
        principalRestant: 400,
        interetRestant: 50,
        penaliteRestante: 25,
        resteAPayer: 475
      }
    ])),
    enregistrerRemboursement: jasmine.createSpy('enregistrerRemboursement').and.returnValue(of({}))
  };

  const authServiceMock = {
    getCurrentUser: jasmine.createSpy('getCurrentUser').and.returnValue({ role: 'GESTIONNAIRE', permissions: [] }),
    hasAnyRole: jasmine.createSpy('hasAnyRole').and.returnValue(true)
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RemboursementCreditFormComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ creditId: '1' }) } } },
        { provide: CreditService, useValue: creditServiceMock },
        { provide: AuthService, useValue: authServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RemboursementCreditFormComponent);
    fixture.detectChanges();
  });

  it('affiche la guidance de retard et la pénalité métier', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Suivi du dossier crédit');
    expect(text).toContain('Retard de remboursement');
    expect(text).toContain('Pénalités totales');
    expect(text).toContain('50 CDF');
  });
});
