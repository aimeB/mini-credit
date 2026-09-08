import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { MesActionsComponent } from './mes-actions.component';
import { WorkflowTaskService } from '../../../../shared/services/workflow-task.service';
import { AuthService } from '../../../../core/services/auth.service';

describe('MesActionsComponent', () => {
  let component: MesActionsComponent;
  let fixture: ComponentFixture<MesActionsComponent>;
  let router: Router;

  const workflowTaskServiceStub = {
    getMyActions: jasmine.createSpy('getMyActions').and.returnValue(of([
      {
        id: 1,
        typeAction: 'CONTROLE_A_FAIRE',
        module: 'CAISSE',
        referenceMetier: 'CAI-SESSION-1',
        entityType: 'SESSION_CAISSE',
        entityId: 1,
        titre: 'Session caisse à contrôler',
        priorite: 'HAUTE',
        statut: 'A_FAIRE',
        roleDestinataire: 'CAISSIER',
        roleAttendu: 'CAISSIER',
        dateCreation: '2026-07-01T08:00:00'
      }
    ])),
    getSupervisionActions: jasmine.createSpy('getSupervisionActions').and.returnValue(of([])),
    markAsViewed: jasmine.createSpy('markAsViewed').and.returnValue(of({})),
    complete: jasmine.createSpy('complete').and.returnValue(of({}))
  };

  const authServiceStub = {
    getCurrentUser: jasmine.createSpy('getCurrentUser').and.returnValue({
      id: 9,
      role: 'CAISSIER',
      permissions: ['TASK_READ_OWN', 'CREDIT_DISBURSE']
    })
  };

  beforeEach(async () => {
    authServiceStub.getCurrentUser.and.returnValue({
      id: 9,
      role: 'CAISSIER',
      permissions: ['TASK_READ_OWN', 'CREDIT_DISBURSE']
    });

    await TestBed.configureTestingModule({
      imports: [MesActionsComponent],
      providers: [
        provideRouter([]),
        { provide: WorkflowTaskService, useValue: workflowTaskServiceStub },
        { provide: AuthService, useValue: authServiceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MesActionsComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('charge les actions au démarrage', () => {
    expect(workflowTaskServiceStub.getMyActions).toHaveBeenCalledWith('A_FAIRE');
    expect(component.filteredActions.length).toBe(1);
  });

  it('redirige vers le détail session caisse contrôleur pour voir dossier', () => {
    const navigateSpy = spyOn(router, 'navigateByUrl');
    const action = component.filteredActions[0];

    component.voirDossier(action);

    expect(navigateSpy).toHaveBeenCalledWith('/caisses/sessions/1');
  });

  it('redirige vers la route fournie par l action', () => {
    const navigateSpy = spyOn(router, 'navigateByUrl');

    component.voirDossier({
      id: 6,
      typeAction: 'DEPENSE_CAISSE_VALIDATE',
      module: 'CAISSE',
      referenceMetier: 'DEPENSE-6',
      entityType: 'DEPENSE_CAISSE',
      entityId: 6,
      titre: 'Dépense à valider',
      priorite: 'HAUTE',
      statut: 'A_FAIRE',
      route: '/caisses/depenses',
      dateCreation: '2026-07-01T08:00:00'
    });

    expect(navigateSpy).toHaveBeenCalledWith('/caisses/depenses');
  });

  it('redirige vers le détail retrait épargne pour voir dossier', () => {
    const navigateSpy = spyOn(router, 'navigateByUrl');

    component.voirDossier({
      id: 2,
      typeAction: 'PAYER_RETRAIT_EPARGNE',
      module: 'EPARGNE',
      referenceMetier: 'RETRAIT-2',
      entityType: 'RETRAIT_EPARGNE',
      entityId: 2,
      titre: 'Retrait épargne à payer',
      priorite: 'MOYENNE',
      statut: 'A_FAIRE',
      dateCreation: '2026-07-01T08:00:00'
    });

    expect(navigateSpy).toHaveBeenCalledWith('/epargne/demandes-retrait/2');
  });

  it('redirige vers la validation recette pour voir dossier', () => {
    const navigateSpy = spyOn(router, 'navigateByUrl');

    component.voirDossier({
      id: 3,
      typeAction: 'CONTROLER_RECETTE_TERRAIN',
      module: 'RECETTE',
      referenceMetier: 'RECETTE-3',
      entityType: 'RECETTE_TERRAIN',
      entityId: 3,
      titre: 'Recette journalière à contrôler',
      priorite: 'MOYENNE',
      statut: 'A_FAIRE',
      dateCreation: '2026-07-01T08:00:00'
    });

    expect(navigateSpy).toHaveBeenCalledWith('/recettes/3/valider');
  });

  it('redirige vers le dossier demande crédit pour voir dossier', () => {
    const navigateSpy = spyOn(router, 'navigateByUrl');

    component.voirDossier({
      id: 4,
      typeAction: 'APPROUVER_DEMANDE_CREDIT',
      module: 'CREDIT',
      referenceMetier: 'DCR-4',
      entityType: 'DEMANDE_CREDIT',
      entityId: 4,
      titre: 'Demande crédit à approuver',
      priorite: 'HAUTE',
      statut: 'A_FAIRE',
      dateCreation: '2026-07-01T08:00:00'
    });

    expect(navigateSpy).toHaveBeenCalledWith('/credits/demandes?demandeId=4');
  });

  it('redirige le décaissement crédit vers le vrai décaissement', () => {
    const navigateSpy = spyOn(router, 'navigateByUrl');

    component.voirDossier({
      id: 5,
      typeAction: 'DECAISSER_CREDIT',
      module: 'CREDIT',
      referenceMetier: 'CR-5',
      entityType: 'CREDIT',
      entityId: 5,
      titre: 'Crédit à décaisser',
      priorite: 'HAUTE',
      statut: 'A_FAIRE',
      dateCreation: '2026-07-01T08:00:00'
    });

    expect(navigateSpy).toHaveBeenCalledWith('/credits/5/decaissement');
  });

  it('ignore une route obsolète du DTO pour le décaissement crédit', () => {
    const navigateSpy = spyOn(router, 'navigateByUrl');

    component.voirDossier({
      id: 7,
      typeAction: 'DECAISSER_CREDIT',
      module: 'CREDIT',
      referenceMetier: 'CR-2026-1',
      entityType: 'CREDIT',
      entityId: 5,
      titre: 'Crédit à décaisser',
      priorite: 'HAUTE',
      statut: 'A_FAIRE',
      dateCreation: '2026-07-01T08:00:00',
      route: '/credits/5/contrat'
    });

    expect(navigateSpy).toHaveBeenCalledWith('/credits/5/decaissement');
  });

  it('redirige le contrat crédit hors décaissement vers le contrat', () => {
    const navigateSpy = spyOn(router, 'navigateByUrl');

    component.voirDossier({
      id: 6,
      typeAction: 'GENERER_CONTRAT',
      module: 'CREDIT',
      referenceMetier: 'CR-6',
      entityType: 'CREDIT',
      entityId: 6,
      titre: 'Contrat crédit à générer',
      priorite: 'MOYENNE',
      statut: 'A_FAIRE',
      dateCreation: '2026-07-01T08:00:00'
    });

    expect(navigateSpy).toHaveBeenCalledWith('/credits/6/contrat');
  });

  it('affiche les rôles sous libellés métier', () => {
    const text = fixture.nativeElement.textContent || '';
    expect(text).toContain('Mes actions');
  });

  it('affiche la vue supervision pour les superviseurs autorises', () => {
    authServiceStub.getCurrentUser.and.returnValue({
      id: 20,
      role: 'RCI',
      permissions: ['TASK_SUPERVISE']
    });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent || '';
    expect(text).toContain('Mes actions à traiter');
    expect(text).toContain('Supervision des actions');
  });

  it('n affiche pas Terminer en supervision meme si une action existe', () => {
    authServiceStub.getCurrentUser.and.returnValue({
      id: 21,
      role: 'RCI',
      permissions: ['TASK_SUPERVISE']
    });
    component.supervisionActions = [{
      id: 9,
      typeAction: 'APPROUVER_DEMANDE_CREDIT',
      module: 'CREDIT',
      referenceMetier: 'DCR-9',
      entityType: 'DEMANDE_CREDIT',
      entityId: 9,
      titre: 'Demande crédit à superviser',
      priorite: 'HAUTE',
      statut: 'A_FAIRE',
      roleDestinataire: 'CHEF_BUREAU',
      roleAttendu: 'CHEF_BUREAU',
      dateCreation: '2026-07-01T08:00:00'
    }];
    component.activeView = 'supervision';
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent || '';
    expect(text).toContain('Lecture supervision uniquement');
    expect(text).toContain('Consulter dossier');
    expect(text).not.toContain('Terminer');
  });

  it('affiche Terminer seulement pour une action personnelle du role destinataire', () => {
    authServiceStub.getCurrentUser.and.returnValue({
      id: 22,
      role: 'CAISSIER',
      permissions: ['TASK_READ_OWN']
    });
    component.actions = [{
      id: 10,
      typeAction: 'DECAISSER_CREDIT',
      module: 'CREDIT',
      referenceMetier: 'CR-10',
      entityType: 'CREDIT',
      entityId: 10,
      titre: 'Crédit à décaisser',
      priorite: 'HAUTE',
      statut: 'A_FAIRE',
      roleDestinataire: 'CAISSIER',
      roleAttendu: 'CAISSIER',
      dateCreation: '2026-07-01T08:00:00'
    }];
    component.activeView = 'mine';
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent || '';
    expect(text).toContain('Voir dossier');
    expect(text).toContain('Terminer');
  });
});
