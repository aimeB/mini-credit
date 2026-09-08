import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { DemandeRetraitEpargneDetailComponent } from './demande-retrait-epargne-detail.component';
import { DemandeRetraitEpargneService } from '../../services/demande-retrait-epargne.service';
import { CompteEpargneService } from '../../services/compte-epargne.service';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';
import { TicketRecuService } from '../../services/ticket-recu.service';

describe('DemandeRetraitEpargneDetailComponent', () => {
  let component: DemandeRetraitEpargneDetailComponent;
  let fixture: ComponentFixture<DemandeRetraitEpargneDetailComponent>;
  let demandeServiceSpy: jasmine.SpyObj<DemandeRetraitEpargneService>;
  let compteServiceSpy: jasmine.SpyObj<CompteEpargneService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let workflowSpy: jasmine.SpyObj<WorkflowMessageService>;
  let ticketServiceSpy: jasmine.SpyObj<TicketRecuService>;

  const demande = {
    id: 1,
    compteEpargneId: 9,
    compteEpargneNumero: 'CEP001',
    membreId: 101,
    membreNom: 'Membre Test',
    montantDemande: 15000,
    statut: 'CREEE',
    dateDemande: new Date('2026-07-01T08:00:00'),
    motifRejet: null
  } as any;

  beforeEach(async () => {
    demandeServiceSpy = jasmine.createSpyObj<DemandeRetraitEpargneService>('DemandeRetraitEpargneService', [
      'getById',
      'validerDemande',
      'rejeterDemande',
      'decaisserRetrait',
      'annulerDemande'
    ]);
    compteServiceSpy = jasmine.createSpyObj<CompteEpargneService>('CompteEpargneService', ['getById']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser', 'hasPermission']);
    workflowSpy = jasmine.createSpyObj<WorkflowMessageService>('WorkflowMessageService', ['getGuidance']);
    ticketServiceSpy = jasmine.createSpyObj<TicketRecuService>('TicketRecuService', [
      'getByDemandeRetrait',
      'marquerImpression',
      'genererDuplicata',
      'getPrintableUrl'
    ]);

    demandeServiceSpy.getById.and.returnValue(of(demande));
    compteServiceSpy.getById.and.returnValue(of({ soldeDisponible: 45000, soldeBloque: 5000 } as any));
    authServiceSpy.getCurrentUser.and.returnValue({
      role: 'CONTROLEUR',
      permissions: ['RETRAIT_VALIDATE']
    } as any);
    authServiceSpy.hasPermission.and.returnValue(false);
    ticketServiceSpy.getByDemandeRetrait.and.returnValue(of([]));
    workflowSpy.getGuidance.and.returnValue({
      title: 'Guidance',
      message: 'Message test',
      currentStep: 'Controle',
      severity: 'warning',
      canCurrentUserAct: true
    });

    await TestBed.configureTestingModule({
      imports: [DemandeRetraitEpargneDetailComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'id' ? '1' : null)
              }
            }
          }
        },
        { provide: DemandeRetraitEpargneService, useValue: demandeServiceSpy },
        { provide: CompteEpargneService, useValue: compteServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: WorkflowMessageService, useValue: workflowSpy },
        { provide: TicketRecuService, useValue: ticketServiceSpy },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigate']) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DemandeRetraitEpargneDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(demandeServiceSpy.getById).toHaveBeenCalledWith(1);
  });

  it('détecte correctement les rôles pour un contrôleur', () => {
    expect(component.isControleur).toBeTrue();
    expect(component.isCaissier).toBeFalse();
    expect(component.isMembre).toBeFalse();
  });

  it('calcule la guidance contextuelle avec statut, rôle et metadata', () => {
    expect(workflowSpy.getGuidance).toHaveBeenCalled();

    const lastCallArgs = workflowSpy.getGuidance.calls.mostRecent().args[0] as any;
    expect(lastCallArgs.module).toBe('RETRAIT_EPARGNE');
    expect(lastCallArgs.status).toBe('CREEE');
    expect(lastCallArgs.currentRole).toBe('CONTROLEUR');
    expect(lastCallArgs.permissions).toEqual(['RETRAIT_VALIDATE']);
    expect(lastCallArgs.metadata.montantDemande).toBe(15000);
    expect(lastCallArgs.metadata.soldeDisponible).toBe(45000);
    expect(lastCallArgs.metadata.soldeBloque).toBe(5000);
  });

  it('charge le ticket reçu pour une demande décaissée', () => {
    demandeServiceSpy.getById.and.returnValue(of({ ...demande, statut: 'DECAISSEE' } as any));
    ticketServiceSpy.getByDemandeRetrait.and.returnValue(of([
      { id: 77, numeroTicket: 'TIC-001', typeTicket: 'RETRAIT_EPARGNE', statut: 'GENERE', membreId: 101, compteEpargneId: 9, montantPrincipal: 15000, ancienSolde: 45000, nouveauSolde: 30000, codeVerification: 'ABCD-2345' } as any
    ]));

    component.loadDemande(1);

    expect(ticketServiceSpy.getByDemandeRetrait).toHaveBeenCalledWith(1);
    expect(component.ticketRecu?.numeroTicket).toBe('TIC-001');
  });
});
