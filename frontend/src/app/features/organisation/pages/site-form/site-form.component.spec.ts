import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { SiteFormComponent } from './site-form.component';
import { SiteService } from '../../../membres/services/site.service';
import { AgenceService } from '../../../employes/services/agence.service';

describe('SiteFormComponent', () => {
  const siteServiceMock = {
    getById: jasmine.createSpy('getById').and.returnValue(of({ id: 1, nomSite: 'S1', codeSite: 'S1', agenceId: 1, zone: 'Zone', actif: true })),
    create: jasmine.createSpy('create').and.returnValue(of({})),
    update: jasmine.createSpy('update').and.returnValue(of({}))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SiteFormComponent, RouterTestingModule],
      providers: [
        { provide: SiteService, useValue: siteServiceMock },
        { provide: AgenceService, useValue: { getAll: () => of([{ id: 1, nomAgence: 'Agence', actif: true }]) } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({}) } }
        }
      ]
    }).compileComponents();

    siteServiceMock.create.calls.reset();
    siteServiceMock.update.calls.reset();
  });

  it('affiche la guidance lot 7 pour le formulaire site', () => {
    const fixture = TestBed.createComponent(SiteFormComponent);
    fixture.detectChanges();
    expect((fixture.nativeElement.textContent as string)).toContain('Gestion des sites');
  });

  it('payloadSiteEnMajuscules — création trim et transforme nomSite', () => {
    const fixture = TestBed.createComponent(SiteFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.setValue({
      agenceId: 1,
      codeSite: 'SAC01',
      nomSite: '  sacombi  ',
      zone: 'Zone test',
      actif: true
    });

    component.enregistrer();

    expect(siteServiceMock.create).toHaveBeenCalled();
    expect(siteServiceMock.create.calls.mostRecent().args[0].nomSite).toBe('SACOMBI');
  });
});
