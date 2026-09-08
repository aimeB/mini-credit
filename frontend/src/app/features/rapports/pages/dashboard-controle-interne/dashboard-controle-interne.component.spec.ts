import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { DashboardControleInterneComponent } from './dashboard-controle-interne.component';
import { DashboardControleInterneService } from '../../services/dashboard-controle-interne.service';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';

describe('DashboardControleInterneComponent', () => {
  let component: DashboardControleInterneComponent;
  let fixture: ComponentFixture<DashboardControleInterneComponent>;
  let serviceSpy: jasmine.SpyObj<DashboardControleInterneService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let workflowSpy: jasmine.SpyObj<WorkflowMessageService>;

  beforeEach(async () => {
    serviceSpy = jasmine.createSpyObj<DashboardControleInterneService>('DashboardControleInterneService', ['getDashboard']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser', 'hasPermission']);
    workflowSpy = jasmine.createSpyObj<WorkflowMessageService>('WorkflowMessageService', ['getGuidance']);

    serviceSpy.getDashboard.and.returnValue(of({
      caisse: { sessionsOuvertes: 0, sessionsFermeesNonValidees: 0, ecartsDetectes: 0, ecartsEnInvestigation: 0, ecartsNonResolus: 0 },
      depenses: { brouillon: 0, enAttenteValidation: 0, valideesNonPayees: 0, payees: 0, rejetees: 0 },
      recettes: { brouillon: 0, soumises: 0, validees: 0, rejetees: 0 },
      retraits: { creees: 0, valides: 0, rejetees: 0, decaissees: 0 },
      credits: { approuves: 0, enCours: 0, enRetard: 0, contentieux: 0, clotures: 0 },
      audit: { totalEvenements: 0, infos: 0, warnings: 0, critiques: 0 },
      alertes: []
    } as any));

    authServiceSpy.getCurrentUser.and.returnValue({ role: 'RCI', permissions: ['DASHBOARD_CONTROLE_INTERNE_READ'] } as any);
    authServiceSpy.hasPermission.and.returnValue(true);

    workflowSpy.getGuidance.and.returnValue({
      title: 'Guidance',
      message: 'Message test',
      currentStep: 'Audit',
      severity: 'warning',
      canCurrentUserAct: true
    });

    await TestBed.configureTestingModule({
      imports: [DashboardControleInterneComponent],
      providers: [
        provideRouter([]),
        { provide: DashboardControleInterneService, useValue: serviceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: WorkflowMessageService, useValue: workflowSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardControleInterneComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('charge la guidance role-aware du contrôle interne', () => {
    expect(component).toBeTruthy();
    expect(serviceSpy.getDashboard).toHaveBeenCalled();
    expect(workflowSpy.getGuidance).toHaveBeenCalledWith(jasmine.objectContaining({
      module: 'DASHBOARD_ROLE',
      status: 'RCI'
    }));
  });
});
