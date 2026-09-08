import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { EmployeListComponent } from './list.component';
import { EmployeService } from '../../services/employe.service';
import { PaiementSalaireService } from '../../services/paiement-salaire.service';
import { EmployeResponse } from '../../models/employe-response';
import { FonctionEmploye } from '../../models/fonction-employe';

describe('EmployeListComponent — affichage du site par fonction', () => {
  let fixture: ComponentFixture<EmployeListComponent>;
  let employeServiceMock: { getAll: jasmine.Spy; delete: jasmine.Spy };

  beforeEach(async () => {
    employeServiceMock = {
      getAll: jasmine.createSpy('getAll').and.returnValue(of([])),
      delete: jasmine.createSpy('delete').and.returnValue(of(void 0))
    };

    await TestBed.configureTestingModule({
      imports: [EmployeListComponent, RouterTestingModule],
      providers: [
        { provide: EmployeService, useValue: employeServiceMock },
        { provide: PaiementSalaireService, useValue: { create: jasmine.createSpy('create').and.returnValue(of({})) } }
      ]
    }).compileComponents();
  });

  function createEmploye(fonction: FonctionEmploye, nomSite: string | null = 'Maternité'): EmployeResponse {
    return {
      id: 1,
      matricule: `EMP-${fonction}`,
      nom: 'Falck',
      prenom: 'Jean',
      nomComplet: 'Jean Falck',
      telephone: '+243812345678',
      fonction,
      dateEmbauche: '2026-01-15',
      salaireBase: 250000,
      primeFixe: 0,
      bonusVariable: 0,
      totalRemuneration: 250000,
      actif: true,
      agenceId: 1,
      nomAgence: 'Agence Centre',
      siteId: nomSite ? 10 : null,
      nomSite,
      dateCreation: '2026-01-15T00:00:00',
      dateModification: '2026-01-15T00:00:00'
    };
  }

  function renderEmployes(employes: EmployeResponse[]): HTMLElement {
    employeServiceMock.getAll.and.returnValue(of(employes));
    fixture = TestBed.createComponent(EmployeListComponent);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  function getText(root: HTMLElement): string {
    return root.textContent || '';
  }

  function getAffectationCell(root: HTMLElement): HTMLTableCellElement {
    return root.querySelector('tbody tr td:nth-child(5)') as HTMLTableCellElement;
  }

  function expectAgenceSansMentionSite(fonction: FonctionEmploye): void {
    const root = renderEmployes([createEmploye(fonction)]);
    const cell = getAffectationCell(root);
    const cellText = cell.textContent || '';

    expect(cellText).toContain('Agence Centre');
    expect(cellText).not.toContain('Site');
    expect(cellText).not.toContain('—');
    expect(cellText).not.toContain('Maternité');
    expect(cell.title).toBe('Agence Centre');
  }

  it('listeAgentTerrainAfficheAgenceEtSite — AGENT_TERRAIN affiche agence et site', () => {
    const root = renderEmployes([createEmploye('AGENT_TERRAIN', 'Maternité')]);
    const cell = getAffectationCell(root);

    expect(cell.textContent).toContain('Agence Centre');
    expect(cell.textContent).toContain('Site : Maternité');
    expect(cell.title).toContain('Site : Maternité');
  });

  it('motSitePresentPourAgentTerrain — le mot Site est présent pour AGENT_TERRAIN', () => {
    const root = renderEmployes([createEmploye('AGENT_TERRAIN', 'Maternité')]);

    expect(getAffectationCell(root).textContent).toContain('Site');
  });

  it('listeGestionnaireAfficheAgenceSansMentionSite — GESTIONNAIRE affiche seulement agence', () => {
    expectAgenceSansMentionSite('GESTIONNAIRE');
  });

  it('listeCooAfficheToutesLesAgences — COO affiche une affectation transverse', () => {
    const root = renderEmployes([createEmploye('COO')]);
    const cell = getAffectationCell(root);

    expect(cell.textContent).toContain('Toutes les agences');
    expect(cell.title).toBe('Toutes les agences / supervision transverse');
  });

  it('listeRciAfficheToutesLesAgences — RCI affiche une affectation transverse', () => {
    const root = renderEmployes([createEmploye('RCI')]);
    const cell = getAffectationCell(root);

    expect(cell.textContent).toContain('Toutes les agences');
    expect(cell.title).toBe('Toutes les agences / supervision transverse');
  });

  it('listeCooNaffichePasDelvaux — COO ne montre pas son agence technique', () => {
    const root = renderEmployes([{ ...createEmploye('COO'), nomAgence: 'Delvaux' }]);

    expect(getAffectationCell(root).textContent).not.toContain('Delvaux');
  });

  it('listeRciNaffichePasDelvaux — RCI ne montre pas son agence technique', () => {
    const root = renderEmployes([{ ...createEmploye('RCI'), nomAgence: 'Delvaux' }]);

    expect(getAffectationCell(root).textContent).not.toContain('Delvaux');
  });

  it('listeCaissierAfficheAgenceSansMentionSite — CAISSIER affiche seulement agence', () => {
    expectAgenceSansMentionSite('CAISSIER');
  });

  it('listeControleurAfficheAgenceSansMentionSite — CONTROLEUR affiche seulement agence', () => {
    expectAgenceSansMentionSite('CONTROLEUR');
  });

  it('listeChefBureauAfficheAgenceSansMentionSite — CHEF_BUREAU affiche seulement agence', () => {
    expectAgenceSansMentionSite('CHEF_BUREAU');
  });

  it('ancienNomSiteDansDtoNonTerrainNestPasAffiche — un ancien nomSite non terrain est ignoré', () => {
    const root = renderEmployes([createEmploye('CONTROLEUR', 'Maternité')]);

    expect(getAffectationCell(root).textContent).not.toContain('Maternité');
  });

  it('motSiteAbsentPourNonTerrain — le mot Site est absent dans les lignes non terrain', () => {
    const root = renderEmployes([
      createEmploye('GESTIONNAIRE'),
      { ...createEmploye('CAISSIER'), id: 2, matricule: 'EMP-CAISSIER-2' },
      { ...createEmploye('CONTROLEUR'), id: 3, matricule: 'EMP-CONTROLEUR-3' },
      { ...createEmploye('CHEF_BUREAU'), id: 4, matricule: 'EMP-CHEF_BUREAU-4' }
    ]);

    expect(getText(root)).not.toContain('Site :');
  });

  it('clicPrincipalOuvreLaFicheDetail — le matricule et le nom pointent vers la consultation', () => {
    const root = renderEmployes([createEmploye('GESTIONNAIRE')]);
    const links = Array.from(root.querySelectorAll('tbody tr a')) as HTMLAnchorElement[];

    expect(links[0].getAttribute('href')).toBe('/employes/1');
    expect(links[1].getAttribute('href')).toBe('/employes/1');
  });
});