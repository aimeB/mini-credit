import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { RapportsDashboardComponent } from './rapports-dashboard.component';
import { RapportService } from '../../services/rapport.service';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';

describe('RapportsDashboardComponent', () => {
  let component: RapportsDashboardComponent;
  let fixture: ComponentFixture<RapportsDashboardComponent>;
  let rapportServiceSpy: jasmine.SpyObj<RapportService>;
  let workflowSpy: jasmine.SpyObj<WorkflowMessageService>;

  beforeEach(async () => {
    rapportServiceSpy = jasmine.createSpyObj<RapportService>('RapportService', ['getKPIDashboard', 'getRapportRisque']);
    workflowSpy = jasmine.createSpyObj<WorkflowMessageService>('WorkflowMessageService', ['getGuidance']);

    rapportServiceSpy.getKPIDashboard.and.returnValue(of({
      totalClients: 10,
      totalCreditActifs: 5,
      totalPortefeuilleActual: 1000,
      tauxDefautPortefeuille: 1,
      revenus30Jours: 100,
      depenses30Jours: 50,
      benefice30Jours: 50,
      epargneCollectee: 200
    } as any));
    rapportServiceSpy.getRapportRisque.and.returnValue(of([] as any));
    workflowSpy.getGuidance.and.returnValue({
      title: 'Guidance',
      message: 'Message test',
      currentStep: 'Pilotage',
      severity: 'info',
      canCurrentUserAct: true
    });

    await TestBed.configureTestingModule({
      imports: [RapportsDashboardComponent],
      providers: [
        provideRouter([]),
        { provide: RapportService, useValue: rapportServiceSpy },
        { provide: AuthService, useValue: jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']) },
        { provide: WorkflowMessageService, useValue: workflowSpy }
      ]
    }).compileComponents();

    const auth = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    auth.getCurrentUser.and.returnValue({ role: 'CHEF_BUREAU' } as any);

    fixture = TestBed.createComponent(RapportsDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('affiche une guidance role-aware', () => {
    expect(component).toBeTruthy();
    expect(workflowSpy.getGuidance).toHaveBeenCalledWith(jasmine.objectContaining({
      module: 'DASHBOARD_ROLE',
      status: 'CHEF_BUREAU'
    }));
  });
});
