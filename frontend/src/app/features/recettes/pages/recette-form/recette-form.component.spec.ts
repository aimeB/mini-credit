import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';

import { RecetteFormComponent } from './recette-form.component';
import { RecetteTerrainService } from '../../services/recette-terrain.service';
import { AgentTerrainService } from '../../../membres/services/agent-terrain.service';
import { SiteService } from '../../../membres/services/site.service';
import { AuthService } from '../../../../core/services/auth.service';

describe('RecetteFormComponent', () => {
  let component: RecetteFormComponent;
  let fixture: ComponentFixture<RecetteFormComponent>;

  const recetteServiceMock = {
    getById: jasmine.createSpy('getById').and.returnValue(of(undefined)),
    create: jasmine.createSpy('create').and.returnValue(of({ id: 5 })),
    update: jasmine.createSpy('update').and.returnValue(of({ id: 5 })),
    soumettre: jasmine.createSpy('soumettre').and.returnValue(of({ id: 5 }))
  };

  const agentServiceMock = {
    getAll: jasmine.createSpy('getAll').and.returnValue(
      of([
        { id: 100, utilisateurId: 1, siteId: 10, nomCompletUtilisateur: 'Agent Test' }
      ])
    )
  };

  const siteServiceMock = {
    getAll: jasmine.createSpy('getAll').and.returnValue(
      of([
        { id: 10, nomSite: 'Site A', actif: true },
        { id: 11, nomSite: 'Site B', actif: true }
      ])
    )
  };

  const authServiceMock = {
    getCurrentUser: jasmine.createSpy('getCurrentUser').and.returnValue({ id: 1, nomComplet: 'Agent Test' }),
    hasRole: jasmine.createSpy('hasRole').and.callFake((role: string) => role === 'AGENT_TERRAIN')
  };

  const routerMock = {
    navigate: jasmine.createSpy('navigate')
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecetteFormComponent],
      providers: [
        { provide: RecetteTerrainService, useValue: recetteServiceMock },
        { provide: AgentTerrainService, useValue: agentServiceMock },
        { provide: SiteService, useValue: siteServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
        { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RecetteFormComponent);
    component = fixture.componentInstance;
    spyOn(window, 'alert');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not render global epargne source field', () => {
    const html = fixture.nativeElement.textContent as string;
    expect(html).not.toContain('Source épargne');
  });

  it('should not duplicate site field for agent terrain', () => {
    const html = fixture.nativeElement.textContent as string;
    expect(html).toContain('Site affecté');
    expect(html).not.toContain('Site *');
  });

  it('should submit create payload without epargneSourceType', () => {
    component.form.patchValue({
      dateRecette: '2025-01-01',
      membresVisites: 2,
      nouveauxMembres: 1,
      carnetDistribues: 1,
      epargneCollectee: 100,
      remboursementsCreditCollectes: 50,
      fraisCollectes: 10,
      especesRemises: 160,
      especesEmises: 0
    });

    component.onSubmit('BROUILLON');

    expect(recetteServiceMock.create).toHaveBeenCalled();
    const payload = recetteServiceMock.create.calls.mostRecent().args[0];
    expect(payload.epargneSourceType).toBeUndefined();
  });
});
