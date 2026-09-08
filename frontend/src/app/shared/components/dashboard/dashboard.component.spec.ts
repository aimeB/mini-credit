import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { DashboardComponent } from './dashboard.component';
import { AuthService } from '../../../core/services/auth.service';
import { WorkflowMessageService } from '../../services/workflow-message.service';
import { WorkflowTaskService } from '../../services/workflow-task.service';
import { DemandeCreditService } from '../../../features/credit/services/demande-credit.service';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let workflowSpy: jasmine.SpyObj<WorkflowMessageService>;
  let workflowTaskSpy: jasmine.SpyObj<WorkflowTaskService>;
  let demandeCreditSpy: jasmine.SpyObj<DemandeCreditService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['logout'], {
      currentUser$: of({
        id: 1,
        username: 'admin',
        email: 'admin@test.com',
        nomComplet: 'Admin',
        role: 'ADMIN',
        permissions: ['ANY']
      })
    });

    workflowSpy = jasmine.createSpyObj<WorkflowMessageService>('WorkflowMessageService', ['getGuidance']);
    workflowSpy.getGuidance.and.returnValue({
      title: 'Guidance dashboard',
      message: 'Message test',
      currentStep: 'Pilotage',
      severity: 'info',
      canCurrentUserAct: true
    });

    workflowTaskSpy = jasmine.createSpyObj<WorkflowTaskService>('WorkflowTaskService', ['getDashboard']);
    workflowTaskSpy.getDashboard.and.returnValue(of({
      totalAFaire: 2,
      totalEnCours: 1,
      totalTerminee: 4,
      urgentCount: 1,
      overdueCount: 0
    }));

    demandeCreditSpy = jasmine.createSpyObj<DemandeCreditService>('DemandeCreditService', ['getFraisCreditAEncaisser']);
    demandeCreditSpy.getFraisCreditAEncaisser.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: WorkflowMessageService, useValue: workflowSpy },
        { provide: WorkflowTaskService, useValue: workflowTaskSpy },
        { provide: DemandeCreditService, useValue: demandeCreditSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('charge une guidance dashboard 3N pour le rôle courant', () => {
    expect(component).toBeTruthy();
    expect(workflowSpy.getGuidance).toHaveBeenCalledWith(jasmine.objectContaining({
      module: 'DASHBOARD_ROLE',
      status: 'GERANT_GENERAL',
      currentRole: 'ADMIN'
    }));
  });

  it('charge les indicateurs mes actions pour roles autorises', () => {
    expect(component.canShowTaskWidget()).toBeTrue();
    expect(workflowTaskSpy.getDashboard).toHaveBeenCalled();
    expect(component.taskDashboard?.totalAFaire).toBe(2);
  });

  it('affiche le widget mes actions pour GESTIONNAIRE', async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['logout'], {
      currentUser$: of({
        id: 2,
        username: 'gestionnaire',
        email: 'gestionnaire@test.com',
        nomComplet: 'Gestionnaire',
        role: 'GESTIONNAIRE',
        permissions: ['TASK_READ_ANTENNE']
      })
    });

    await TestBed.resetTestingModule().configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: WorkflowMessageService, useValue: workflowSpy },
        { provide: WorkflowTaskService, useValue: workflowTaskSpy },
        { provide: DemandeCreditService, useValue: demandeCreditSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.canShowTaskWidget()).toBeTrue();
    expect(workflowTaskSpy.getDashboard).toHaveBeenCalled();
  });

  it('affiche A FAIRE = 1 pour un GESTIONNAIRE avec une pré-analyse crédit à traiter', async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['logout'], {
      currentUser$: of({
        id: 2,
        username: 'gestionnaire',
        email: 'gestionnaire@test.com',
        nomComplet: 'Gestionnaire',
        role: 'GESTIONNAIRE',
        permissions: ['TASK_READ_ANTENNE']
      })
    });
    workflowTaskSpy.getDashboard.and.returnValue(of({
      totalAFaire: 1,
      totalEnCours: 0,
      totalTerminee: 0,
      urgentCount: 0,
      overdueCount: 0,
      tasks: [{
        id: 9010,
        typeAction: 'TRAITER_DEMANDE_CREDIT',
        module: 'CREDIT',
        referenceMetier: 'DCR-901',
        entityType: 'DEMANDE_CREDIT',
        entityId: 901,
        titre: 'Pré-analyse crédit',
        description: 'Demande de crédit à pré-analyser',
        roleDestinataire: 'GESTIONNAIRE',
        antenneId: 7,
        siteId: 3,
        priorite: 'NORMALE',
        statut: 'A_FAIRE',
        dateCreation: '2026-07-09T10:00:00'
      }]
    }));

    await TestBed.resetTestingModule().configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: WorkflowMessageService, useValue: workflowSpy },
        { provide: WorkflowTaskService, useValue: workflowTaskSpy },
        { provide: DemandeCreditService, useValue: demandeCreditSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.taskDashboard?.totalAFaire).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('A faire');
    expect(fixture.nativeElement.textContent).toContain('1');
  });

  it('affiche A FAIRE = 1 avec fallback countAFaire quand totalAFaire est absent', async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['logout'], {
      currentUser$: of({
        id: 2,
        username: 'gestionnaire',
        email: 'gestionnaire@test.com',
        nomComplet: 'Gestionnaire',
        role: 'GESTIONNAIRE',
        permissions: ['TASK_READ_ANTENNE']
      })
    });
    workflowTaskSpy.getDashboard.and.returnValue(of({
      countAFaire: 1,
      totalEnCours: 0,
      totalTerminee: 0,
      urgentCount: 0,
      overdueCount: 0
    }));

    await TestBed.resetTestingModule().configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: WorkflowMessageService, useValue: workflowSpy },
        { provide: WorkflowTaskService, useValue: workflowTaskSpy },
        { provide: DemandeCreditService, useValue: demandeCreditSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.getTotalAFaire()).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('1');
  });

  it('redirige le bouton mes actions du GESTIONNAIRE vers Mes actions', async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['logout'], {
      currentUser$: of({
        id: 2,
        username: 'gestionnaire',
        email: 'gestionnaire@test.com',
        nomComplet: 'Gestionnaire',
        role: 'GESTIONNAIRE',
        permissions: ['TASK_READ_ANTENNE']
      })
    });

    await TestBed.resetTestingModule().configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: WorkflowMessageService, useValue: workflowSpy },
        { provide: WorkflowTaskService, useValue: workflowTaskSpy },
        { provide: DemandeCreditService, useValue: demandeCreditSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.getActionsRoute()).toBe('/mes-actions');
  });

  it('laisse AGENT_TERRAIN ouvrir le dashboard sans appeler Mes actions', async () => {
    workflowTaskSpy.getDashboard.calls.reset();
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['logout'], {
      currentUser$: of({
        id: 3,
        username: 'agent',
        email: 'agent@test.com',
        nomComplet: 'Agent Terrain',
        role: 'ROLE_AGENT_TERRAIN',
        permissions: []
      })
    });

    await TestBed.resetTestingModule().configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: WorkflowMessageService, useValue: workflowSpy },
        { provide: WorkflowTaskService, useValue: workflowTaskSpy },
        { provide: DemandeCreditService, useValue: demandeCreditSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.currentUser?.role).toBe('ROLE_AGENT_TERRAIN');
    expect(component.canShowTaskWidget()).toBeFalse();
    expect(workflowTaskSpy.getDashboard).not.toHaveBeenCalled();
    expect(component.roleGuidance).toBeTruthy();
  });

  it('route les rôles officiels 3N vers la page Mes actions', async () => {
    const roles = ['GESTIONNAIRE', 'CONTROLEUR', 'CAISSIER', 'CHEF_BUREAU', 'COO', 'RCI', 'GERANT_GENERAL'];

    for (const role of roles) {
      authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['logout'], {
        currentUser$: of({
          id: 10,
          username: role.toLowerCase(),
          email: `${role.toLowerCase()}@test.com`,
          nomComplet: role,
          role,
          permissions: ['TASK_READ_ANTENNE']
        })
      });

      await TestBed.resetTestingModule().configureTestingModule({
        imports: [DashboardComponent],
        providers: [
          provideRouter([]),
          { provide: AuthService, useValue: authServiceSpy },
          { provide: WorkflowMessageService, useValue: workflowSpy },
          { provide: WorkflowTaskService, useValue: workflowTaskSpy },
          { provide: DemandeCreditService, useValue: demandeCreditSpy }
        ]
      }).compileComponents();

      fixture = TestBed.createComponent(DashboardComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      expect(component.canShowTaskWidget()).withContext(role).toBeTrue();
      expect(component.getActionsRoute()).withContext(role).toBe('/mes-actions');
      expect(component.getActionsRoute()).withContext(role).not.toBe('/access-denied');
    }
  });

  it('ne contient plus le libellé vertical slice caisse', () => {
    expect(fixture.nativeElement.textContent).toContain('Actions à traiter selon votre rôle');
    expect(fixture.nativeElement.textContent).not.toContain('vertical slice caisse');
  });

  it('n’affiche pas d’action de validation financière dans le widget Gestionnaire', async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['logout'], {
      currentUser$: of({
        id: 2,
        username: 'gestionnaire',
        email: 'gestionnaire@test.com',
        nomComplet: 'Gestionnaire',
        role: 'GESTIONNAIRE',
        permissions: ['TASK_READ_ANTENNE']
      })
    });
    workflowTaskSpy.getDashboard.and.returnValue(of({
      totalAFaire: 1,
      totalEnCours: 0,
      totalTerminee: 0,
      urgentCount: 0,
      overdueCount: 0,
      tasks: [{
        id: 9030,
        typeAction: 'APPROUVER_DEMANDE_CREDIT',
        module: 'CREDIT',
        referenceMetier: 'DCR-903',
        entityType: 'DEMANDE_CREDIT',
        entityId: 903,
        titre: 'Demande crédit à approuver',
        roleDestinataire: 'CHEF_BUREAU',
        priorite: 'HAUTE',
        statut: 'A_FAIRE',
        dateCreation: '2026-07-09T10:00:00'
      }]
    }));

    await TestBed.resetTestingModule().configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: WorkflowMessageService, useValue: workflowSpy },
        { provide: WorkflowTaskService, useValue: workflowTaskSpy },
        { provide: DemandeCreditService, useValue: demandeCreditSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Demande crédit à approuver');
    expect(fixture.nativeElement.textContent).not.toContain('APPROUVER_DEMANDE_CREDIT');
  });
});
