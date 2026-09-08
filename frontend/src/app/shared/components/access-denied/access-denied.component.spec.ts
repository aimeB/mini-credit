import { ComponentFixture, TestBed } from '@angular/core/testing';
import { convertToParamMap } from '@angular/router';

import { AccessDeniedComponent } from './access-denied.component';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';

describe('AccessDeniedComponent', () => {
  let component: AccessDeniedComponent;
  let fixture: ComponentFixture<AccessDeniedComponent>;
  let routerSpy: jasmine.SpyObj<Router>;
  let locationSpy: jasmine.SpyObj<Location>;

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);
    locationSpy = jasmine.createSpyObj<Location>('Location', ['back']);

    await TestBed.configureTestingModule({
      imports: [AccessDeniedComponent],
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: Location, useValue: locationSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({
                title: "Vous n'êtes pas autorisé à effectuer cette action.",
                detail: 'Cette opération doit être enregistrée depuis Ma collecte du jour.'
              })
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AccessDeniedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('affiche un message utilisateur clair sur 403', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('403 - Accès refusé');
    expect(text).toContain("Vous n'êtes pas autorisé à effectuer cette action.");
    expect(text).toContain('Ma collecte du jour');
  });

  it('propose retour accueil et retour precedent', () => {
    const buttons = fixture.nativeElement.querySelectorAll('button');
    expect(buttons.length).toBeGreaterThanOrEqual(2);

    buttons[0].click();
    expect(locationSpy.back).toHaveBeenCalled();

    buttons[1].click();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});
