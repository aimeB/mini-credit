import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CreditDashboardComponent } from './credit-dashboard.component';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';

describe('CreditDashboardComponent', () => {
  let component: CreditDashboardComponent;
  let fixture: ComponentFixture<CreditDashboardComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let workflowSpy: jasmine.SpyObj<WorkflowMessageService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['hasRole', 'getCurrentUser']);
    workflowSpy = jasmine.createSpyObj<WorkflowMessageService>('WorkflowMessageService', ['getGuidance']);
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'AGENT_TERRAIN');
    authServiceSpy.getCurrentUser.and.returnValue({ role: 'AGENT_TERRAIN' } as any);
    workflowSpy.getGuidance.and.returnValue({
      title: 'Guidance',
      message: 'Message test',
      currentStep: 'Etape',
      severity: 'info',
      canCurrentUserAct: true
    });

    await TestBed.configureTestingModule({
      imports: [CreditDashboardComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: WorkflowMessageService, useValue: workflowSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreditDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('affiche le parcours collecte pour AGENT_TERRAIN', () => {
    const text = fixture.nativeElement.textContent;
    expect(component.isAgentTerrain).toBeTrue();
    expect(text).toContain('Parcours Agent Terrain');
    expect(text).toContain('Nouvelle Demande via Collecte');
    expect(text).toContain('Remboursement via Collecte');
    expect(text).not.toContain('Voir les demandes');
    expect(text).not.toContain('Créer une demande');
    expect(workflowSpy.getGuidance).toHaveBeenCalled();
  });
});
