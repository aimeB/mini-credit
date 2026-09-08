import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';

import { MembreFormComponent } from './membre-form.component';
import { MembreService } from '../../services/membre.service';
import { SiteService } from '../../services/site.service';
import { AgentTerrainService } from '../../services/agent-terrain.service';
import { AuthService } from '../../../../core/services/auth.service';

describe('MembreFormComponent', () => {
  let component: MembreFormComponent;
  let fixture: ComponentFixture<MembreFormComponent>;
  let membreServiceMock: { create: jasmine.Spy; update: jasmine.Spy; getById: jasmine.Spy };
  let agentTerrainServiceMock: { getAll: jasmine.Spy; getBySite: jasmine.Spy };

  beforeEach(async () => {
    membreServiceMock = {
      create: jasmine.createSpy('create').and.returnValue(of({ membre: { id: 1 } })),
      update: jasmine.createSpy('update').and.returnValue(of({ id: 1 })),
      getById: jasmine.createSpy('getById').and.returnValue(of({ id: 1 }))
    };
    agentTerrainServiceMock = {
      getAll: jasmine.createSpy('getAll').and.returnValue(of([])),
      getBySite: jasmine.createSpy('getBySite').and.returnValue(of([]))
    };

    await TestBed.configureTestingModule({
      imports: [MembreFormComponent],
      providers: [
        { provide: MembreService, useValue: membreServiceMock },
        { provide: SiteService, useValue: { getAll: () => of([{ id: 10, nomSite: 'SITE' }]) } },
        { provide: AgentTerrainService, useValue: agentTerrainServiceMock },
        { provide: AuthService, useValue: { getCurrentUser: () => ({ id: 1, username: 'admin', role: 'ADMIN', email: '', nomComplet: 'Admin' }) } },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(undefined) }) } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => null } } } },
        provideRouter([]),
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MembreFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('afficheErreurBackendTelephoneDoublon — message source backend conservé', () => {
    membreServiceMock.create.and.returnValue(throwError(() => ({
      error: { message: 'Ce numéro de téléphone est déjà utilisé.' }
    })));
    fillBaseForm();

    component.enregistrer();

    expect(component.error).toBe('Ce numéro de téléphone est déjà utilisé.');
  });

  it('afficheErreurBackendPrenomNomDoublon — message source backend conservé', () => {
    membreServiceMock.create.and.returnValue(throwError(() => ({
      error: { message: 'Une personne avec le même prénom et le même nom existe déjà.' }
    })));
    fillBaseForm();

    component.enregistrer();

    expect(component.error).toBe('Une personne avec le même prénom et le même nom existe déjà.');
  });

  it('memePrenomOuMemeNomSeulNonBloqueCoteFrontend — le submit reste envoyé au backend', () => {
    fillBaseForm();

    component.enregistrer();

    expect(membreServiceMock.create).toHaveBeenCalledTimes(1);
  });

  it('changementSiteRechargeAgentsTerrain — le site sélectionné appelle endpoint by-site', () => {
    agentTerrainServiceMock.getBySite.calls.reset();
    agentTerrainServiceMock.getBySite.and.returnValue(of([
      { id: 7, matricule: 'AT-001', siteId: 20, nomCompletUtilisateur: 'Agent Site 20' }
    ]));

    component.form.patchValue({ siteId: 20 });

    expect(agentTerrainServiceMock.getBySite).toHaveBeenCalledWith(20);
    expect(component.agents).toEqual([
      jasmine.objectContaining({ id: 7, nomAffichage: 'Agent Site 20' })
    ]);
  });

  it('changementSiteVideAgentSelectionne — la sélection agent est réinitialisée', () => {
    component.form.patchValue({ agentId: 7 }, { emitEvent: false });

    component.form.patchValue({ siteId: 20 });

    expect(component.form.get('agentId')?.value).toBeNull();
  });

  it('listeVideNonCacheeDefinitivement — un second changement de site recharge la liste', () => {
    agentTerrainServiceMock.getBySite.calls.reset();
    agentTerrainServiceMock.getBySite.and.returnValues(
      of([]),
      of([{ id: 8, matricule: 'AT-002', siteId: 30, username: 'agent30' }])
    );

    component.form.patchValue({ siteId: 20 });
    component.form.patchValue({ siteId: 30 });

    expect(agentTerrainServiceMock.getBySite.calls.allArgs()).toEqual([[20], [30]]);
    expect(component.agents).toEqual([
      jasmine.objectContaining({ id: 8, nomAffichage: 'agent30' })
    ]);
  });

  function fillBaseForm(): void {
    component.form.patchValue({
      nom: 'Doe',
      postnom: 'Alpha',
      prenom: 'Jane',
      sexe: 'F',
      dateNaissance: '1998-01-01',
      telephonePrincipal: '099000111',
      adresse: 'Adresse 1',
      ville: 'Kinshasa',
      commune: 'Gombe',
      quartier: 'Q1',
      email: 'jane@example.com',
      professionActivite: 'Commerce',
      lieuActivite: 'Marche',
      siteId: 10,
      dateAdhesion: '2026-01-15'
    });
  }
});
