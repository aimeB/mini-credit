import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';

import { SiteListComponent } from './site-list.component';
import { SiteService } from '../../../membres/services/site.service';
import { AgenceService } from '../../../employes/services/agence.service';
import { AdminAgentTerrainService } from '../../../../admin/agent-terrain/services/admin-agent-terrain.service';

describe('SiteListComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SiteListComponent, RouterTestingModule],
      providers: [
        { provide: SiteService, useValue: { getAll: () => of([]) } },
        { provide: AgenceService, useValue: { getAll: () => of([]) } },
        { provide: AdminAgentTerrainService, useValue: { getAll: () => of([]) } }
      ]
    }).compileComponents();
  });

  it('affiche la guidance lot 7 pour les sites', () => {
    const fixture = TestBed.createComponent(SiteListComponent);
    fixture.detectChanges();
    expect((fixture.nativeElement.textContent as string)).toContain('Gestion des sites');
  });
});
