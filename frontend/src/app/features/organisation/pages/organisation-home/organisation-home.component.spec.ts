import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';

import { OrganisationHomeComponent } from './organisation-home.component';
import { AgenceService } from '../../../employes/services/agence.service';
import { SiteService } from '../../../membres/services/site.service';

describe('OrganisationHomeComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrganisationHomeComponent, RouterTestingModule],
      providers: [
        { provide: AgenceService, useValue: { getAll: () => of([]) } },
        { provide: SiteService, useValue: { getAll: () => of([]) } }
      ]
    }).compileComponents();
  });

  it('affiche la guidance referentiel administratif', () => {
    const fixture = TestBed.createComponent(OrganisationHomeComponent);
    fixture.detectChanges();
    expect((fixture.nativeElement.textContent as string)).toContain('Referentiel administratif');
  });
});
