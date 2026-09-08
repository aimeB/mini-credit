import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthService } from '../../../../core/services/auth.service';
import { DepenseCaisseService } from '../../services/depense-caisse.service';
import { DepenseCaisseValidationComponent } from './depense-caisse-validation.component';

describe('DepenseCaisseValidationComponent', () => {
  let component: DepenseCaisseValidationComponent;
  let fixture: ComponentFixture<DepenseCaisseValidationComponent>;
  let depenseServiceSpy: jasmine.SpyObj<DepenseCaisseService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  let currentRole = 'CAISSIER';
  let currentStatus = 'VALIDEE';

  beforeEach(async () => {
    depenseServiceSpy = jasmine.createSpyObj<DepenseCaisseService>('DepenseCaisseService', [
      'getById',
      'valider',
      'rejeter',
      'payer',
      'annuler'
    ]);

    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['hasRole', 'hasAnyRole', 'getCurrentUser']);

    authServiceSpy.hasRole.and.callFake((role: string) => role === currentRole);
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes(currentRole));
    authServiceSpy.getCurrentUser.and.callFake(() => ({
      id: 1,
      username: 'user',
      email: 'user@example.com',
      nomComplet: 'User Test',
      role: currentRole
    } as any));

    depenseServiceSpy.getById.and.callFake(() => of({
      id: 11,
      caisseId: 1,
      categorie: 'AUTRE',
      montant: 50000,
      devise: 'CDF',
      motif: 'Achat consommables',
      statut: currentStatus,
      dateDemande: '2026-07-01T08:00:00Z',
      createdAt: '2026-07-01T08:00:00Z',
      updatedAt: '2026-07-01T08:00:00Z'
    } as any));

    depenseServiceSpy.valider.and.returnValue(of({} as any));
    depenseServiceSpy.rejeter.and.returnValue(of({} as any));
    depenseServiceSpy.payer.and.returnValue(of({} as any));
    depenseServiceSpy.annuler.and.returnValue(of({} as any));

    await TestBed.configureTestingModule({
      imports: [DepenseCaisseValidationComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: '11' })
            }
          }
        },
        { provide: DepenseCaisseService, useValue: depenseServiceSpy },
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DepenseCaisseValidationComponent);
    component = fixture.componentInstance;
  });

  it('affiche guidance DEPENSE_CAISSE validée et action Caissier', () => {
    currentRole = 'CAISSIER';
    currentStatus = 'VALIDEE';

    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Dépense validée');
    expect(text).toContain('Vous pouvez agir');
    expect(text).toContain('Payer la dépense et enregistrer la sortie de caisse');
  });

  it('affiche blocage non-Caissier quand statut VALIDEE', () => {
    currentRole = 'ADMIN';
    currentStatus = 'VALIDEE';

    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Vous ne pouvez pas agir à cette étape');
    expect(text).toContain('Le paiement appartient au Caissier après validation');
  });

  it('affiche blocage du Caissier quand statut EN_ATTENTE_VALIDATION', () => {
    currentRole = 'CAISSIER';
    currentStatus = 'EN_ATTENTE_VALIDATION';

    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Dépense en attente de validation');
    expect(text).toContain('Vous ne pouvez pas encore payer cette dépense');
  });
});
