import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { DemandeRetraitEpargneListComponent } from './demande-retrait-epargne-list.component';
import { DemandeRetraitEpargneService } from '../../services/demande-retrait-epargne.service';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';

describe('DemandeRetraitEpargneListComponent', () => {
  let component: DemandeRetraitEpargneListComponent;
  let fixture: ComponentFixture<DemandeRetraitEpargneListComponent>;
  let serviceSpy: jasmine.SpyObj<DemandeRetraitEpargneService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let workflowSpy: jasmine.SpyObj<WorkflowMessageService>;

  const demandes = [
    {
      id: 1,
      compteEpargneId: 9,
      compteEpargneNumero: 'CEP001',
      membreNom: 'Membre A',
      montantDemande: 15000,
      statut: 'CREEE',
      dateDemande: '2026-07-01T08:00:00'
    },
    {
      id: 2,
      compteEpargneId: 10,
      compteEpargneNumero: 'CEP002',
      membreNom: 'Membre B',
      montantDemande: 30000,
      statut: 'VALIDEE',
      dateDemande: '2026-07-01T09:00:00'
    }
  ] as any;

  beforeEach(async () => {
    serviceSpy = jasmine.createSpyObj<DemandeRetraitEpargneService>('DemandeRetraitEpargneService', ['getAll', 'annulerDemande']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);
    workflowSpy = jasmine.createSpyObj<WorkflowMessageService>('WorkflowMessageService', ['getGuidance']);

    serviceSpy.getAll.and.returnValue(of(demandes));
    serviceSpy.annulerDemande.and.returnValue(of({} as any));
    authServiceSpy.getCurrentUser.and.returnValue({
      role: 'MEMBER',
      permissions: []
    } as any);
    workflowSpy.getGuidance.and.returnValue({
      title: 'Guidance',
      message: 'Message test',
      currentStep: 'Etape',
      severity: 'info',
      canCurrentUserAct: false
    });

    await TestBed.configureTestingModule({
      imports: [DemandeRetraitEpargneListComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: {
                get: () => null
              }
            }
          }
        },
        { provide: DemandeRetraitEpargneService, useValue: serviceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: WorkflowMessageService, useValue: workflowSpy },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigate']) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DemandeRetraitEpargneListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(serviceSpy.getAll).toHaveBeenCalled();
  });

  it('calcule une guidance par défaut sur le workflow retrait', () => {
    expect(workflowSpy.getGuidance).toHaveBeenCalledWith(jasmine.objectContaining({
      module: 'RETRAIT_EPARGNE',
      status: 'DEMANDE',
      currentRole: 'MEMBER'
    }));
  });

  it('utilise le statut filtré pour la guidance', () => {
    component.filterStatut = 'VALIDEE';
    component.applyFilters();

    expect(workflowSpy.getGuidance).toHaveBeenCalledWith(jasmine.objectContaining({
      module: 'RETRAIT_EPARGNE',
      status: 'VALIDEE'
    }));
  });
});
