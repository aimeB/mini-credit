import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { RecetteDashboardComponent } from './recette-dashboard.component';
import { RecetteTerrainService } from '../../services/recette-terrain.service';
import { CollecteTerrainService } from '../../services/collecte-terrain.service';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';

describe('RecetteDashboardComponent', () => {
  let component: RecetteDashboardComponent;
  let fixture: ComponentFixture<RecetteDashboardComponent>;
  let recetteServiceSpy: jasmine.SpyObj<RecetteTerrainService>;
  let collecteServiceSpy: jasmine.SpyObj<CollecteTerrainService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let workflowSpy: jasmine.SpyObj<WorkflowMessageService>;

  beforeEach(async () => {
    recetteServiceSpy = jasmine.createSpyObj<RecetteTerrainService>('RecetteTerrainService', ['searchAndFilterPaginated']);
    collecteServiceSpy = jasmine.createSpyObj<CollecteTerrainService>('CollecteTerrainService', ['list']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);
    workflowSpy = jasmine.createSpyObj<WorkflowMessageService>('WorkflowMessageService', ['getGuidance']);

    recetteServiceSpy.searchAndFilterPaginated.and.returnValue(of({
      content: [{
        statut: 'VALIDEE',
        epargneCollectee: 100,
        remboursementsCreditCollectes: 50,
        fraisCollectes: 10,
        totalCollecte: 160,
        especesRemises: 160,
        excedent: 0,
        manquant: 0,
        membresVisites: 2,
        carnetDistribues: 1,
        demandesCreditRecueillies: 1
      }],
      totalElements: 1,
      totalPages: 1,
      currentPage: 0,
      pageSize: 10000,
      last: true,
      first: true,
      empty: false
    } as any));

    collecteServiceSpy.list.and.returnValue(of({
      content: [{
        statut: 'SOUMISE',
        totalEpargneCalcule: 200,
        totalRemboursementsCalcule: 80,
        totalFraisCalcule: 20,
        totalGeneralCalcule: 300,
        especesRemises: 280,
        ecartTresorerie: -20,
        lignes: [
          { membreId: 1, typeLigne: 'EPARGNE', quantite: 1 },
          { membreId: 1, typeLigne: 'CARNET', quantite: 2 },
          { membreId: 2, typeLigne: 'DEMANDE_CREDIT', quantite: 1 }
        ]
      }],
      totalElements: 1,
      totalPages: 1,
      currentPage: 0,
      pageSize: 10000,
      hasNext: false,
      hasPrevious: false,
    } as any));
    authServiceSpy.getCurrentUser.and.returnValue({ role: 'CONTROLEUR' } as any);
    workflowSpy.getGuidance.and.returnValue({
      title: 'Guidance',
      message: 'Message test',
      currentStep: 'Etape',
      severity: 'warning',
      canCurrentUserAct: true
    });

    await TestBed.configureTestingModule({
      imports: [RecetteDashboardComponent],
      providers: [
        { provide: RecetteTerrainService, useValue: recetteServiceSpy },
        { provide: CollecteTerrainService, useValue: collecteServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: WorkflowMessageService, useValue: workflowSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RecetteDashboardComponent);
    component = fixture.componentInstance;
  });

  it('calcule les KPI depuis Collecte en mode actif', () => {
    component.mode = 'COLLECTE';
    fixture.detectChanges();

    expect(collecteServiceSpy.list).toHaveBeenCalled();
    expect(component.stats.totalRecettes).toBe(1);
    expect(component.stats.totalGeneral).toBe(300);
    expect(component.stats.totalMembresVisites).toBe(2);
    expect(component.stats.totalCarnets).toBe(2);
    expect(component.stats.totalDemandesCredit).toBe(1);
  });

  it('garde le mode legacy disponible', () => {
    component.mode = 'LEGACY';
    fixture.detectChanges();

    expect(recetteServiceSpy.searchAndFilterPaginated).toHaveBeenCalled();
    expect(component.stats.totalRecettes).toBe(1);
    expect(component.stats.totalGeneral).toBe(160);
    expect(component.stats.totalEpargne).toBe(100);
  });

  it('charge une guidance role-aware sur le dashboard recettes', () => {
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Supervision des collectes terrain');
    expect(text).toContain('Contrôleur');
    expect(text).toContain('Contrôler le billetage');
  });
});
