import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter, Router } from '@angular/router';

import { MesCollectesComponent } from './mes-collectes.component';
import { CollecteTerrainService } from '../../services/collecte-terrain.service';

describe('MesCollectesComponent', () => {
  let component: MesCollectesComponent;
  let fixture: ComponentFixture<MesCollectesComponent>;
  let collecteServiceSpy: jasmine.SpyObj<CollecteTerrainService>;
  let router: Router;

  beforeEach(async () => {
    collecteServiceSpy = jasmine.createSpyObj<CollecteTerrainService>('CollecteTerrainService', ['list', 'soumettre']);
    collecteServiceSpy.list.and.returnValue(of({
      content: [
        {
          id: 1,
          agentTerrainId: 11,
          dateCollecte: '2026-06-16',
          antenneId: 1,
          siteId: 2,
          siteNom: 'Site A',
          statut: 'SOUMISE',
          especesRemises: 10000,
          totalEpargneCalcule: 5000,
          totalRemboursementsCalcule: 3000,
          totalFraisCalcule: 2000,
          totalCarnetsCalcule: 1,
          totalGeneralCalcule: 10000,
          ecartTresorerie: 0,
          lignes: []
        },
        {
          id: 2,
          agentTerrainId: 11,
          dateCollecte: '2026-06-15',
          antenneId: 1,
          siteId: 2,
          siteNom: 'Site A',
          statut: 'BROUILLON',
          especesRemises: 1000,
          totalEpargneCalcule: 0,
          totalRemboursementsCalcule: 0,
          totalFraisCalcule: 1000,
          totalCarnetsCalcule: 0,
          totalGeneralCalcule: 1000,
          ecartTresorerie: -1000,
          lignes: []
        },
        {
          id: 3,
          agentTerrainId: 11,
          dateCollecte: '2026-06-14',
          antenneId: 1,
          siteId: 2,
          siteNom: 'Site A',
          statut: 'VALIDEE',
          especesRemises: 1500,
          totalEpargneCalcule: 1500,
          totalRemboursementsCalcule: 0,
          totalFraisCalcule: 0,
          totalCarnetsCalcule: 0,
          totalGeneralCalcule: 1500,
          ecartTresorerie: 0,
          lignes: []
        }
      ],
      totalElements: 3,
      totalPages: 1,
      currentPage: 0,
      pageSize: 20,
      hasNext: false,
      hasPrevious: false,
    } as any));
    collecteServiceSpy.soumettre.and.returnValue(of({ id: 2, statut: 'SOUMISE' } as any));

    await TestBed.configureTestingModule({
      imports: [MesCollectesComponent],
      providers: [
        provideRouter([]),
        { provide: CollecteTerrainService, useValue: collecteServiceSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(MesCollectesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('charge les collectes de l agent', () => {
    expect(collecteServiceSpy.list).toHaveBeenCalled();
    expect(component.collectes.length).toBe(3);
    expect(fixture.nativeElement.textContent).toContain('Mes collectes');
    expect(fixture.nativeElement.textContent).toContain('Voir détail');
  });

  it('affiche Reprendre pour BROUILLON', () => {
    expect(component.canReprendre(component.collectes[1] as any)).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Reprendre / Modifier');
  });

  it('n affiche pas Reprendre pour SOUMISE', () => {
    expect(component.canReprendre(component.collectes[0] as any)).toBeFalse();
  });

  it('n affiche pas Reprendre pour VALIDEE', () => {
    expect(component.canReprendre(component.collectes[2] as any)).toBeFalse();
  });

  it('clic Reprendre ouvre la route edition', () => {
    const navigateSpy = spyOn(router, 'navigate');

    component.reprendre(component.collectes[1] as any);

    expect(navigateSpy).toHaveBeenCalledWith(['/collectes', 2, 'edition']);
  });
});
