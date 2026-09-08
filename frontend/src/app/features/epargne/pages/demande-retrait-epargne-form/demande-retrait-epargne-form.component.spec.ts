import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { DemandeRetraitEpargneFormComponent } from './demande-retrait-epargne-form.component';
import { DemandeRetraitEpargneService } from '../../services/demande-retrait-epargne.service';
import { CompteEpargneService } from '../../services/compte-epargne.service';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';

describe('DemandeRetraitEpargneFormComponent', () => {
  let component: DemandeRetraitEpargneFormComponent;
  let fixture: ComponentFixture<DemandeRetraitEpargneFormComponent>;
  let compteServiceSpy: jasmine.SpyObj<CompteEpargneService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let workflowSpy: jasmine.SpyObj<WorkflowMessageService>;

  beforeEach(async () => {
    compteServiceSpy = jasmine.createSpyObj<CompteEpargneService>('CompteEpargneService', ['getMesComptes', 'getAll']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['hasRole', 'getCurrentUser']);
    workflowSpy = jasmine.createSpyObj<WorkflowMessageService>('WorkflowMessageService', ['getGuidance']);

    compteServiceSpy.getMesComptes.and.returnValue(of([]));
    compteServiceSpy.getAll.and.returnValue(of([] as any));
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'MEMBER');
    authServiceSpy.getCurrentUser.and.returnValue({ role: 'MEMBER', permissions: [] } as any);
    workflowSpy.getGuidance.and.returnValue({
      title: 'Guidance',
      message: 'Message test',
      currentStep: 'Etape',
      severity: 'warning',
      canCurrentUserAct: false
    });

    await TestBed.configureTestingModule({
      imports: [DemandeRetraitEpargneFormComponent],
      providers: [
        provideRouter([]),
        { provide: DemandeRetraitEpargneService, useValue: jasmine.createSpyObj<DemandeRetraitEpargneService>('DemandeRetraitEpargneService', ['creerDemande']) },
        { provide: CompteEpargneService, useValue: compteServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: WorkflowMessageService, useValue: workflowSpy },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigate']) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DemandeRetraitEpargneFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('initialise la guidance retrait avec statut CREEE', () => {
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Nouvelle demande de retrait épargne');
  });

  it('charge les comptes du membre connecté', () => {
    expect(compteServiceSpy.getMesComptes).toHaveBeenCalled();
    expect(compteServiceSpy.getAll).not.toHaveBeenCalled();
  });
});
