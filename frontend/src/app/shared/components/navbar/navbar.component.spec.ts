import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { provideRouter } from '@angular/router';

import { NavbarComponent } from './navbar.component';
import { AuthService } from '../../../core/services/auth.service';
import { WorkflowTaskService } from '../../services/workflow-task.service';

describe('NavbarComponent', () => {
  const removedRoles = ['AGENT_BUREAU', 'RESPONSABLE', 'ROLE_AGENT_BUREAU', 'ROLE_RESPONSABLE'];
  let fixture: ComponentFixture<NavbarComponent>;
  const currentUser$ = new BehaviorSubject<any>({
    id: 1,
    username: 'agent',
    nomComplet: 'Agent Terrain',
    role: 'AGENT_TERRAIN'
  });

  const authServiceStub = {
    currentUser$,
    logout: jasmine.createSpy('logout'),
    hasAnyPermission: jasmine.createSpy('hasAnyPermission').and.returnValue(true)
  } as Partial<AuthService>;

  const workflowRefresh$ = new Subject<void>();
  const workflowTaskServiceStub = {
    refreshCount$: workflowRefresh$,
    getMyActionCount: jasmine.createSpy('getMyActionCount').and.returnValue(of({ totalAFaire: 3 })),
    getMyActions: jasmine.createSpy('getMyActions').and.returnValue(of([]))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NavbarComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceStub },
        { provide: WorkflowTaskService, useValue: workflowTaskServiceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NavbarComponent);
    fixture.detectChanges();
  });

  it('affiche Ma collecte et Mes collectes pour AGENT_TERRAIN', () => {
    currentUser$.next({
      id: 1,
      username: 'agent',
      nomComplet: 'Agent Terrain',
      role: 'AGENT_TERRAIN'
    });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;

    expect(text).toContain('Ma collecte');
    expect(text).toContain('Mes collectes');
    expect(text).not.toContain('Recettes Legacy');
    expect(text).not.toContain('Caisse');
  });

  it('reconnait CHEF_BUREAU dans les modules visibles', () => {
    currentUser$.next({
      id: 2,
      username: 'chef',
      nomComplet: 'Chef Bureau',
      role: 'CHEF_BUREAU'
    });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Ma collecte');
    expect(fixture.componentInstance.getRoleLabel('CHEF_BUREAU')).toBe('Chef de Bureau');
    expect(fixture.componentInstance.getModuleRoute({ route: '/collectes/ma-collecte', roleRoutes: { CHEF_BUREAU: '/collectes/suivi-terrain' } })).toBe('/collectes/suivi-terrain');
  });

  it('garde le libellé officiel CHEF_BUREAU', () => {
    const component = fixture.componentInstance;

    expect(component.getRoleLabel('CHEF_BUREAU')).toBe('Chef de Bureau');
    expect(component.getRoleLabel('ROLE_CHEF_BUREAU')).toBe('Chef de Bureau');
  });

  it('affiche le module Credits pour CONTROLEUR', () => {
    currentUser$.next({
      id: 3,
      username: 'controleur',
      nomComplet: 'Controleur 3N',
      role: 'CONTROLEUR'
    });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Crédits');
    expect(text).toContain('Caisse');
  });

  it('affiche le module Gestionnaire pour GESTIONNAIRE', () => {
    currentUser$.next({
      id: 4,
      username: 'gestionnaire-menu',
      nomComplet: 'Gestionnaire Menu',
      role: 'GESTIONNAIRE'
    });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(fixture.componentInstance.getRoleLabel('GESTIONNAIRE')).toBe('Gestionnaire');
    expect(text).toContain('Crédits');
    expect(text).toContain('Ma collecte');
  });

  it('affiche un menu Gestionnaire non vide sans actions caisse sensibles', () => {
    currentUser$.next({
      id: 6,
      username: 'gestionnaire',
      nomComplet: 'Gestionnaire Gest',
      role: 'GESTIONNAIRE'
    });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Accueil');
    expect(text).toContain('Ma collecte');
    expect(text).toContain('Pré-analyses');
    expect(text).toContain('Crédits');
    expect(text).toContain('Rapports');
    expect(text).not.toContain('Caisse');
    expect(text).not.toContain('Rapports caisse');
  });

  it('affiche le raccourci actions pour GESTIONNAIRE', () => {
    currentUser$.next({
      id: 5,
      username: 'gestionnaire',
      nomComplet: 'Gestionnaire 3N',
      role: 'GESTIONNAIRE'
    });
    fixture.detectChanges();

    expect(fixture.componentInstance.canShowTaskArea()).toBeTrue();
    expect(workflowTaskServiceStub.getMyActionCount).toHaveBeenCalled();
  });

  it('affiche le raccourci actions avec badge pour les roles caisse workflow', () => {
    workflowTaskServiceStub.getMyActionCount.and.returnValue(of({ totalAFaire: 3 }));

    currentUser$.next({
      id: 10,
      username: 'ctrl',
      nomComplet: 'Controleur',
      role: 'CONTROLEUR'
    });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Actions');
    expect(workflowTaskServiceStub.getMyActionCount).toHaveBeenCalled();
    expect(fixture.componentInstance.taskCount).toBe(3);
  });

  it('affiche le badge actions pour CHEF_BUREAU quand le compteur renvoie total', () => {
    workflowTaskServiceStub.getMyActionCount.and.returnValue(of({ total: 2 }));

    currentUser$.next({
      id: 11,
      username: 'chef',
      nomComplet: 'Chef Bureau',
      role: 'CHEF_BUREAU'
    });
    fixture.detectChanges();

    expect(fixture.componentInstance.canShowTaskArea()).toBeTrue();
    expect(fixture.componentInstance.taskCount).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('2');
  });

  it('affiche Frais credit au CAISSIER sans module credit complet', () => {
    currentUser$.next({
      id: 12,
      username: 'caissier',
      nomComplet: 'Caissier 3N',
      role: 'CAISSIER'
    });
    fixture.detectChanges();

    const component = fixture.componentInstance;
    const visibleLabels = component.visibleModules.map((module) => module.navLabel);
    const visibleRoutes = component.visibleModules.map((module) => component.getModuleRoute(module));

    expect(visibleLabels).toContain('Frais crédit');
    expect(visibleRoutes).toContain('/credits/frais-a-encaisser');
    expect(visibleLabels).not.toContain('Crédits');
    expect(visibleRoutes).not.toContain('/credits');
  });

  it('ne donne pas au RCI de raccourci caisse ou credit operationnel', () => {
    currentUser$.next({
      id: 13,
      username: 'rci',
      nomComplet: 'RCI 3N',
      role: 'RCI',
      permissions: ['DASHBOARD_CONTROLE_INTERNE_READ']
    });
    fixture.detectChanges();

    const component = fixture.componentInstance;
    const visibleRoutes = component.visibleModules.map((module) => component.getModuleRoute(module));

    expect(visibleRoutes).toContain('/caisses/rapports/journalier');
    expect(visibleRoutes).toContain('/dashboard/controle-interne');
    expect(visibleRoutes).not.toContain('/caisses/session/:sessionId/operations/nouveau');
    expect(visibleRoutes).not.toContain('/credits/frais-a-encaisser');
    expect(visibleRoutes).not.toContain('/credits');
  });

  it('ne reference aucun role supprime dans les modules de navigation', () => {
    const component = fixture.componentInstance;
    const roles = component.modules.flatMap((module: any) => module.roles || []);
    const roleRoutes = component.modules.flatMap((module: any) => Object.keys(module.roleRoutes || {}));

    for (const removedRole of removedRoles) {
      expect(roles).not.toContain(removedRole);
      expect(roleRoutes).not.toContain(removedRole);
    }
  });

  it('regroupe les modules administrateur excédentaires dans Plus sans les supprimer', () => {
    currentUser$.next({
      id: 20,
      username: 'admin',
      nomComplet: 'Administrateur 3N',
      role: 'ADMIN',
      permissions: ['DASHBOARD_CONTROLE_INTERNE_READ']
    });
    fixture.detectChanges();

    const component = fixture.componentInstance;
    const overflowLabels = component.overflowModules.map((module) => module.navLabel);

    expect(component.primaryModules.length).toBe(8);
    expect(overflowLabels).toEqual(jasmine.arrayContaining([
      'Organisation', 'Agents', 'Personnel', 'Garanties', 'Rapports',
      'Revenus', 'Audit', 'Supervision'
    ]));
    expect(component.visibleModules.length).toBe(component.primaryModules.length + component.overflowModules.length);
  });

  it('ferme les menus avec Echap', () => {
    const component = fixture.componentInstance;
    component.mobileMenuOpen = true;
    component.moreMenuOpen = true;

    component.closeMenusOnEscape();

    expect(component.mobileMenuOpen).toBeFalse();
    expect(component.moreMenuOpen).toBeFalse();
  });
});
