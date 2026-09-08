import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ActivatedRoute, provideRouter } from '@angular/router';

import { EpargneDashboardComponent } from './epargne-dashboard.component';
import { CompteEpargneService } from '../../services/compte-epargne.service';
import { OperationEpargneService } from '../../services/operation-epargne.service';
import { SessionCaisseService } from '../../../caisse/services/session-caisse.service';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';
import { TicketRecuService } from '../../services/ticket-recu.service';

describe('EpargneDashboardComponent', () => {
  let component: EpargneDashboardComponent;
  let fixture: ComponentFixture<EpargneDashboardComponent>;
  let compteServiceSpy: jasmine.SpyObj<CompteEpargneService>;
  let operationEpargneServiceSpy: jasmine.SpyObj<OperationEpargneService>;
  let sessionCaisseServiceSpy: jasmine.SpyObj<SessionCaisseService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let workflowServiceSpy: jasmine.SpyObj<WorkflowMessageService>;
  let ticketRecuServiceSpy: jasmine.SpyObj<TicketRecuService>;

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
      updatedAt: '2026-06-15T00:00:00',
    },
    {
      id: 2,
      membreId: 20,
      membreNomComplet: 'Jean Alpha',
      numeroCompte: 'CEP002',
      typeCompte: 'COTISATION',
      soldeDisponible: 2000,
      soldeBloque: 0,
      statut: 'ACTIF',
      dateOuverture: '2026-06-15',
      createdAt: '2026-06-15T00:00:00',
      updatedAt: '2026-06-15T00:00:00',
    }
  ] as any;

  beforeEach(async () => {
    compteServiceSpy = jasmine.createSpyObj<CompteEpargneService>('CompteEpargneService', ['getAll', 'repairMissingForMember', 'getMesMembresComptes', 'getMesComptes', 'getByMembre']);
    operationEpargneServiceSpy = jasmine.createSpyObj<OperationEpargneService>('OperationEpargneService', ['enregistrer', 'getByCompte']);
    sessionCaisseServiceSpy = jasmine.createSpyObj<SessionCaisseService>('SessionCaisseService', ['getSessionActive']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['hasRole', 'hasAnyRole', 'hasAnyPermission']);
    workflowServiceSpy = jasmine.createSpyObj<WorkflowMessageService>('WorkflowMessageService', ['getGuidance']);
    ticketRecuServiceSpy = jasmine.createSpyObj<TicketRecuService>('TicketRecuService', ['getByOperationEpargne', 'marquerImpression', 'genererDuplicata', 'getPrintableUrl']);
    compteServiceSpy.getAll.and.returnValue(of(comptes));
    compteServiceSpy.getMesMembresComptes.and.returnValue(of(comptes));
    compteServiceSpy.getMesComptes.and.returnValue(of(comptes));
    compteServiceSpy.getByMembre.and.returnValue(of(comptes));
    compteServiceSpy.repairMissingForMember.and.returnValue(of(comptes[0]));
    operationEpargneServiceSpy.enregistrer.and.returnValue(of({} as any));
    operationEpargneServiceSpy.getByCompte.and.returnValue(of([]));
    sessionCaisseServiceSpy.getSessionActive.and.returnValue(of({ id: 1 } as any));
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'ADMIN');
    authServiceSpy.hasAnyRole.and.returnValue(true);
    authServiceSpy.hasAnyPermission.and.returnValue(true);
    workflowServiceSpy.getGuidance.and.returnValue({
      title: 'Guidance',
      message: 'Message test',
      currentStep: 'Etape',
      severity: 'info',
      canCurrentUserAct: true
    });
    ticketRecuServiceSpy.getByOperationEpargne.and.returnValue(of([]));
    ticketRecuServiceSpy.getPrintableUrl.and.returnValue('/tickets-recus/1/print');

    await TestBed.configureTestingModule({
      imports: [EpargneDashboardComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => null,
              },
            },
          },
        },
        { provide: CompteEpargneService, useValue: compteServiceSpy },
        { provide: OperationEpargneService, useValue: operationEpargneServiceSpy },
        { provide: SessionCaisseService, useValue: sessionCaisseServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: WorkflowMessageService, useValue: workflowServiceSpy },
        { provide: TicketRecuService, useValue: ticketRecuServiceSpy },
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EpargneDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(workflowServiceSpy.getGuidance).toHaveBeenCalled();
  });

  it('filtre la liste des comptes via la recherche membre/code', () => {
    component.searchTerm = 'marie';
    component.applySearch();

    expect(component.filteredComptes.length).toBe(1);
    expect(component.filteredComptes[0].numeroCompte).toBe('CEP001');
  });

  it('charge les comptes quand l API retourne une réponse paginée', () => {
    compteServiceSpy.getAll.and.returnValue(of({
      content: comptes,
      totalElements: 2,
      totalPages: 1,
      currentPage: 0,
      pageSize: 10,
      hasNext: false,
      hasPrevious: false,
    } as any));

    component.chargerTousLesComptes();

    expect(component.comptes.length).toBe(2);
    expect(component.filteredComptes.length).toBe(2);
  });

  it('masque l action de creation compte manquant pour AGENT_TERRAIN', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'AGENT_TERRAIN');
    authServiceSpy.hasAnyRole.and.returnValue(false);
    authServiceSpy.hasAnyPermission.and.returnValue(false);
    compteServiceSpy.getMesMembresComptes.and.returnValue(of(comptes));

    fixture = TestBed.createComponent(EpargneDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.canRepairMissingAccount).toBeFalse();
    expect(component.canCreateOperation).toBeFalse();
    expect(compteServiceSpy.getMesMembresComptes).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Mode consultation Agent Terrain');
    expect(fixture.nativeElement.textContent).not.toContain('Créer compte manquant');
    expect(fixture.nativeElement.textContent).not.toContain('Nouvelle Opération');
  });

  it('affiche un message explicite sur erreur 403', () => {
    compteServiceSpy.getAll.and.returnValue(throwError(() => ({ status: 403, error: { message: 'Forbidden' } })));

    component.chargerTousLesComptes();

    expect(component.errorMessage).toContain('Accès refusé');
  });

  it('CONTROLEUR charge les comptes en lecture via getAll', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'CONTROLEUR');
    authServiceSpy.hasAnyRole.and.returnValue(false);
    authServiceSpy.hasAnyPermission.and.returnValue(false);
    compteServiceSpy.getAll.calls.reset();

    fixture = TestBed.createComponent(EpargneDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(compteServiceSpy.getAll).toHaveBeenCalled();
    expect(component.canCreateOperation).toBeFalse();
  });

  it('masque Nouvelle Opération pour GESTIONNAIRE et affiche un message de consultation', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'GESTIONNAIRE');
    authServiceSpy.hasAnyPermission.and.returnValue(false);

    fixture = TestBed.createComponent(EpargneDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.onCompteSelected(comptes[0]);
    fixture.detectChanges();

    expect(component.canCreateOperation).toBeFalse();
    expect(fixture.nativeElement.textContent).not.toContain('Nouvelle Opération');
    expect(fixture.nativeElement.textContent).toContain('Vous pouvez consulter le compte');
  });
});
