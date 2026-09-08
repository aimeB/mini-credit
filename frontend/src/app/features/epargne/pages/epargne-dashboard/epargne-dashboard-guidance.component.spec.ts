import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { EpargneDashboardComponent } from './epargne-dashboard.component';
import { CompteEpargneService } from '../../services/compte-epargne.service';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';

describe('EpargneDashboardComponent Guidance', () => {
  let component: EpargneDashboardComponent;
  let fixture: ComponentFixture<EpargneDashboardComponent>;
  let compteServiceSpy: jasmine.SpyObj<CompteEpargneService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let workflowServiceSpy: jasmine.SpyObj<WorkflowMessageService>;

  const comptes = [
    {
      id: 1,
      membreId: 10,
      membreNomComplet: 'Marie Test',
      numeroCompte: 'CEP001',
      typeCompte: 'EPARGNE_VOLONTAIRE',
      soldeDisponible: 1000,
      soldeBloque: 100,
      statut: 'ACTIF',
      dateOuverture: '2026-06-15',
      createdAt: '2026-06-15T00:00:00',
      updatedAt: '2026-06-15T00:00:00'
    }
  ] as any;

  beforeEach(async () => {
    compteServiceSpy = jasmine.createSpyObj<CompteEpargneService>('CompteEpargneService', ['getAll', 'getMesMembresComptes', 'getMesComptes', 'getByMembre']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['hasRole', 'hasAnyRole', 'hasAnyPermission']);
    workflowServiceSpy = jasmine.createSpyObj<WorkflowMessageService>('WorkflowMessageService', ['getGuidance']);

    compteServiceSpy.getAll.and.returnValue(of(comptes));
    compteServiceSpy.getMesMembresComptes.and.returnValue(of(comptes));
    compteServiceSpy.getMesComptes.and.returnValue(of(comptes));
    compteServiceSpy.getByMembre.and.returnValue(of(comptes));
    workflowServiceSpy.getGuidance.and.returnValue({
      title: 'Guidance',
      message: 'Message test',
      currentStep: 'Etape',
      severity: 'info',
      canCurrentUserAct: false
    });

    await TestBed.configureTestingModule({
      imports: [EpargneDashboardComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => null
              }
            }
          }
        },
        { provide: CompteEpargneService, useValue: compteServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: WorkflowMessageService, useValue: workflowServiceSpy }
      ]
    }).compileComponents();
  });

  it('utilise OPERATION_BLOQUEE pour un AGENT_TERRAIN en mode consultation', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'AGENT_TERRAIN');
    authServiceSpy.hasAnyRole.and.returnValue(false);
    authServiceSpy.hasAnyPermission.and.returnValue(false);

    fixture = TestBed.createComponent(EpargneDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const args = workflowServiceSpy.getGuidance.calls.mostRecent().args[0] as any;
    expect(args.module).toBe('EPARGNE_OPERATION');
    expect(args.status).toBe('OPERATION_BLOQUEE');
    expect(args.currentRole).toBe('AGENT_TERRAIN');
  });

  it('utilise CONSULTATION avec rôle courant pour un rôle habilité sans compte sélectionné', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'ADMIN');
    authServiceSpy.hasAnyRole.and.returnValue(true);
    authServiceSpy.hasAnyPermission.and.returnValue(true);

    fixture = TestBed.createComponent(EpargneDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const args = workflowServiceSpy.getGuidance.calls.mostRecent().args[0] as any;
    expect(args.module).toBe('EPARGNE_OPERATION');
    expect(args.status).toBe('CONSULTATION');
    expect(args.currentRole).toBe('ADMIN');
    expect(args.expectedRole).toBe('ADMIN');
    expect(args.metadata.compteSelectionne).toBeFalse();
  });

  it('utilise OPERATION_AUTORISEE quand un compte est sélectionné et rôle habilité', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'ADMIN');
    authServiceSpy.hasAnyRole.and.returnValue(true);
    authServiceSpy.hasAnyPermission.and.returnValue(true);

    fixture = TestBed.createComponent(EpargneDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.onCompteSelected(comptes[0]);

    const args = workflowServiceSpy.getGuidance.calls.mostRecent().args[0] as any;
    expect(args.module).toBe('EPARGNE_OPERATION');
    expect(args.status).toBe('OPERATION_AUTORISEE');
    expect(args.currentRole).toBe('ADMIN');
    expect(args.expectedRole).toBe('ADMIN');
    expect(args.metadata.compteSelectionne).toBeTrue();
  });
});
