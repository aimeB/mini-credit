import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from './auth.service';

describe('AuthService role hierarchy', () => {
  let service: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    });

    service = TestBed.inject(AuthService);
  });

  it('reconnait CHEF_BUREAU dans la hierarchie des roles', () => {
    (service as any).currentUserSubject.next({
      id: 1,
      username: 'chef',
      email: 'chef@test.local',
      nomComplet: 'Chef Bureau',
      role: 'CHEF_BUREAU'
    });

    expect(service.hasRole('CHEF_BUREAU')).toBeTrue();
    expect(service.hasAnyRole(['ADMIN', 'CHEF_BUREAU'])).toBeTrue();
  });

  it('normalise ROLE_CHEF_BUREAU en CHEF_BUREAU quand le rôle vient des authorities JWT', () => {
    (service as any).currentUserSubject.next({
      id: 7,
      username: 'chef.jwt',
      email: 'chef.jwt@test.local',
      nomComplet: 'Chef Bureau JWT',
      role: 'ROLE_CHEF_BUREAU',
      permissions: ['SESSION_CAISSE_FINAL_CLOSE']
    });

    expect(service.hasRole('CHEF_BUREAU')).toBeTrue();
    expect(service.hasAnyRole(['ADMIN', 'CHEF_BUREAU'])).toBeTrue();
    expect(service.hasPermission('SESSION_CAISSE_FINAL_CLOSE')).toBeTrue();
  });

  it('normalise ROLE_AGENT_TERRAIN en AGENT_TERRAIN pour hasAnyRole', () => {
    (service as any).currentUserSubject.next({
      id: 8,
      username: 'agent.jwt',
      email: 'agent.jwt@test.local',
      nomComplet: 'Agent Terrain JWT',
      role: 'ROLE_AGENT_TERRAIN'
    });

    expect(service.hasRole('AGENT_TERRAIN')).toBeTrue();
    expect(service.hasAnyRole(['AGENT_TERRAIN'])).toBeTrue();
  });

  it('reconnait CHEF_BUREAU comme rôle officiel', () => {
    (service as any).currentUserSubject.next({
      id: 2,
      username: 'chef',
      email: 'chef@test.local',
      nomComplet: 'Chef Bureau',
      role: 'CHEF_BUREAU'
    });

    expect(service.hasRole('CHEF_BUREAU')).toBeTrue();
    expect(service.hasMinimumRole('GESTIONNAIRE')).toBeTrue();
  });

  it('reconnait GERANT_GENERAL comme rôle supérieur métier', () => {
    (service as any).currentUserSubject.next({
      id: 3,
      username: 'gg',
      email: 'gg@test.local',
      nomComplet: 'Gerant General',
      role: 'GERANT_GENERAL'
    });

    expect(service.hasMinimumRole('RCI')).toBeTrue();
    expect(service.hasMinimumRole('COO')).toBeTrue();
    expect(service.hasMinimumRole('CHEF_BUREAU')).toBeTrue();
  });

  it('inclut COO dans la hiérarchie technique', () => {
    (service as any).currentUserSubject.next({
      id: 4,
      username: 'coo',
      email: 'coo@test.local',
      nomComplet: 'COO',
      role: 'COO'
    });

    expect(service.hasMinimumRole('CHEF_BUREAU')).toBeTrue();
    expect(service.hasMinimumRole('CONTROLEUR')).toBeTrue();
    expect(service.hasMinimumRole('RCI')).toBeFalse();
  });

  it('reconnait GESTIONNAIRE comme rôle officiel', () => {
    (service as any).currentUserSubject.next({
      id: 5,
      username: 'gestionnaire.gb',
      email: 'gestionnaire.gb@test.local',
      nomComplet: 'Gestionnaire Bureau',
      role: 'GESTIONNAIRE'
    });

    expect(service.hasRole('GESTIONNAIRE')).toBeTrue();
    expect(service.hasMinimumRole('AGENT_TERRAIN')).toBeTrue();
    expect(service.hasMinimumRole('CAISSIER')).toBeFalse();
  });

  it('reconnait RCI comme rôle dédié audit/contrôle interne', () => {
    (service as any).currentUserSubject.next({
      id: 6,
      username: 'rci',
      email: 'rci@test.local',
      nomComplet: 'RCI',
      role: 'RCI'
    });

    expect(service.hasRole('RCI')).toBeTrue();
    expect(service.hasMinimumRole('CHEF_BUREAU')).toBeTrue();
    expect(service.hasMinimumRole('GERANT_GENERAL')).toBeFalse();
  });
});
