import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { AgentFormComponent } from './agent-form.component';
import { AdminAgentTerrainService } from '../../services/admin-agent-terrain.service';
import { SiteService } from '../../../../features/membres/services/site.service';

describe('AgentFormComponent', () => {
  let component: AgentFormComponent;
  let fixture: ComponentFixture<AgentFormComponent>;

  beforeEach(async () => {
    const agentSpy = jasmine.createSpyObj<AdminAgentTerrainService>('AdminAgentTerrainService', [
      'getUtilisateursDisponibles',
      'getSites',
      'getGestionnaires',
      'getById',
      'update',
      'create'
    ]);
    agentSpy.getUtilisateursDisponibles.and.returnValue(of([]));
    agentSpy.getSites.and.returnValue(of([]));
    agentSpy.getGestionnaires.and.returnValue(of([]));
    agentSpy.getById.and.returnValue(of({ id: 1 } as any));
    agentSpy.update.and.returnValue(of({} as any));
    agentSpy.create.and.returnValue(of({ id: 1 } as any));

    const siteSpy = jasmine.createSpyObj<SiteService>('SiteService', ['getById']);
    siteSpy.getById.and.returnValue(of({ id: 1, nomSite: 'S1', codeSite: 'S1' } as any));

    await TestBed.configureTestingModule({
      imports: [AgentFormComponent],
      providers: [
        provideRouter([]),
        { provide: AdminAgentTerrainService, useValue: agentSpy },
        { provide: SiteService, useValue: siteSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: convertToParamMap({}) },
            paramMap: of(convertToParamMap({}))
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AgentFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('affiche la guidance de supervision', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Supervision des agents');
  });
});
