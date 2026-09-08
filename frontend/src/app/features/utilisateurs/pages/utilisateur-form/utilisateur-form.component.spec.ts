import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { UtilisateurFormComponent } from './utilisateur-form.component';
import { UtilisateurService } from '../../services/utilisateur.service';
import { EmployeService } from '../../../employes/services/employe.service';
import { ROLES } from '../../models/role-enum';
import { EmployeResponse } from '../../../employes/models/employe-response';

class ActivatedRouteStub {
  snapshot = {
    paramMap: { get: () => null },
    queryParamMap: { get: () => null }
  };
}

describe('UtilisateurFormComponent — rôles globaux', () => {
  let fixture: ComponentFixture<UtilisateurFormComponent>;
  let component: UtilisateurFormComponent;
  let utilisateurServiceMock: { create: jasmine.Spy; update: jasmine.Spy; getById: jasmine.Spy };

  const employesDisponibles: EmployeResponse[] = [
    employe(1, 'AGENT_TERRAIN', 10, 'SITE 10'),
    employe(2, 'GESTIONNAIRE'),
    employe(3, 'CONTROLEUR'),
    employe(4, 'CAISSIER'),
    employe(5, 'CHEF_BUREAU')
  ];

  beforeEach(async () => {
    utilisateurServiceMock = {
      create: jasmine.createSpy('create').and.returnValue(of({})),
      update: jasmine.createSpy('update').and.returnValue(of({})),
      getById: jasmine.createSpy('getById').and.returnValue(of({}))
    };

    await TestBed.configureTestingModule({
      imports: [UtilisateurFormComponent, RouterTestingModule],
      providers: [
        { provide: UtilisateurService, useValue: utilisateurServiceMock },
        { provide: EmployeService, useValue: { getDisponibles: () => of(employesDisponibles), getById: () => of({}) } },
        { provide: ActivatedRoute, useClass: ActivatedRouteStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UtilisateurFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function employe(id: number, fonction: string, siteId?: number, nomSite?: string): EmployeResponse {
    return {
      id,
      matricule: `EMP-${id}`,
      nom: 'Nom',
      prenom: 'Prenom',
      nomComplet: `Employe ${id}`,
      telephone: '+243812345678',
      fonction: fonction as any,
      dateEmbauche: '2026-01-15',
      salaireBase: 1,
      primeFixe: 0,
      bonusVariable: 0,
      totalRemuneration: 1,
      actif: true,
      agenceId: 1,
      nomAgence: 'AGENCE',
      siteId: siteId as any,
      nomSite,
      dateCreation: '',
      dateModification: ''
    };
  }

  function selectRole(role: string): void {
    component.form.patchValue({
      username: 'global.user',
      password: 'secret123',
      confirmPassword: 'secret123',
      roles: [role],
      employeId: null
    });
    fixture.detectChanges();
  }

  it('siteNonObligatoirePourCOO — COO ne bloque pas sans employé lié', () => {
    selectRole(ROLES.COO);

    expect(component.isOperationalRole).toBeFalse();
    expect(component.isGlobalSupervisionRole).toBeTrue();
    expect(component.form.valid).toBeTrue();
  });

  it('siteFacultatifPourGestionnaire — GESTIONNAIRE ne vérifie pas le site de l’employé lié', () => {
    selectRole(ROLES.GESTIONNAIRE);
    component.form.patchValue({ employeId: 2 });
    component.onEmployeChange({ target: { value: '2' } } as any);

    component.enregistrer();

    expect(utilisateurServiceMock.create).toHaveBeenCalled();
  });

  it('siteFacultatifPourControleur — CONTROLEUR ne vérifie pas le site de l’employé lié', () => {
    selectRole(ROLES.CONTROLEUR);
    component.form.patchValue({ employeId: 3 });
    component.onEmployeChange({ target: { value: '3' } } as any);

    component.enregistrer();

    expect(utilisateurServiceMock.create).toHaveBeenCalled();
  });

  it('siteFacultatifPourCaissier — CAISSIER ne vérifie pas le site de l’employé lié', () => {
    selectRole(ROLES.CAISSIER);
    component.form.patchValue({ employeId: 4 });
    component.onEmployeChange({ target: { value: '4' } } as any);

    component.enregistrer();

    expect(utilisateurServiceMock.create).toHaveBeenCalled();
  });

  it('siteFacultatifPourChefBureau — CHEF_BUREAU ne vérifie pas le site de l’employé lié', () => {
    selectRole(ROLES.CHEF_BUREAU);
    component.form.patchValue({ employeId: 5 });
    component.onEmployeChange({ target: { value: '5' } } as any);

    component.enregistrer();

    expect(utilisateurServiceMock.create).toHaveBeenCalled();
  });

  it('siteNonObligatoirePourRCI — RCI ne bloque pas sans employé lié', () => {
    selectRole(ROLES.RCI);

    expect(component.isOperationalRole).toBeFalse();
    expect(component.isGlobalSupervisionRole).toBeTrue();
    expect(component.form.valid).toBeTrue();
  });

  it('siteNonObligatoirePourGerantGeneral — GERANT_GENERAL ne bloque pas sans employé lié', () => {
    selectRole(ROLES.GERANT_GENERAL);

    expect(component.isOperationalRole).toBeFalse();
    expect(component.isGlobalSupervisionRole).toBeTrue();
    expect(component.form.valid).toBeTrue();
  });

  it('siteObligatoirePourAgentTerrainSiRegleExistante — AGENT_TERRAIN reste opérationnel', () => {
    selectRole(ROLES.AGENT_TERRAIN);

    expect(component.isOperationalRole).toBeTrue();
    component.enregistrer();
    expect(component.error).toContain('employé est obligatoire');
  });

  it('siteObligatoirePourAgentTerrain — AGENT_TERRAIN avec employé sans site est bloqué', () => {
    component.employes = [employe(6, 'AGENT_TERRAIN')];
    selectRole(ROLES.AGENT_TERRAIN);
    component.form.patchValue({ employeId: 6 });
    component.onEmployeChange({ target: { value: '6' } } as any);

    component.enregistrer();

    expect(component.error).toContain('Le site est obligatoire pour un Agent Terrain.');
    expect(utilisateurServiceMock.create).not.toHaveBeenCalled();
  });

  it('messageAideRoleGlobalAffiche — le texte de rôle global est visible', () => {
    selectRole(ROLES.COO);

    expect((fixture.nativeElement.textContent as string)).toContain("Ce rôle est global et n'est pas rattaché obligatoirement à un site.");
  });

  it('messageSiteUniquementAgentTerrainAffiche — le texte site terrain est visible', () => {
    selectRole(ROLES.CAISSIER);

    expect((fixture.nativeElement.textContent as string)).toContain('Le site concerne uniquement les Agents Terrain.');
  });
});
