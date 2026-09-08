import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthService } from '../../../../core/services/auth.service';
import { EmployeResponse } from '../../models/employe-response';
import { EmployeService } from '../../services/employe.service';
import { EmployeDetailComponent } from './detail.component';

describe('EmployeDetailComponent', () => {
  let component: EmployeDetailComponent;
  let fixture: ComponentFixture<EmployeDetailComponent>;

  const employe: EmployeResponse = {
    id: 1,
    matricule: 'DEL1-AT-26-001',
    nom: 'Terrain',
    prenom: 'Agent',
    nomComplet: 'Agent Terrain',
    telephone: '+243812345678',
    dateEmbauche: '2026-01-15',
    salaireBase: 250000,
    primeFixe: 0,
    bonusVariable: 0,
    totalRemuneration: 250000,
    actif: true,
    agenceId: 1,
    nomAgence: 'Agence Centre',
    dateCreation: '2026-01-15T00:00:00',
    dateModification: '2026-01-15T00:00:00'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmployeDetailComponent],
      providers: [
        provideRouter([]),
        { provide: EmployeService, useValue: { getById: () => of(employe) } },
        { provide: AuthService, useValue: { hasAnyRole: () => true } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '1' } } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('affiche toujours un avatar avec les initiales de l employe', () => {
    const avatar = fixture.nativeElement.querySelector('[data-testid="employe-avatar"]') as HTMLElement;

    expect(avatar).not.toBeNull();
    expect(avatar.textContent?.trim()).toBe('AT');
    expect(component.getEmployeInitials(employe)).toBe('AT');
  });

  it('afficheLaPhotoEtRetourneAuxInitialesSiLeChargementEchoue', () => {
    component.employe = { ...employe, photoUrl: 'https://cdn.example/employe-1.jpg' };
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="employe-photo"]')).not.toBeNull();
    const photo = fixture.nativeElement.querySelector('[data-testid="employe-photo"]') as HTMLImageElement;
    photo.dispatchEvent(new Event('error'));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="employe-avatar"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('input[type="file"]')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('impossible à afficher');
  });

  it('ouvreLaPhotoDansUneLightboxConsultative', () => {
    component.employe = { ...employe, photoUrl: 'https://cdn.example/employe-1.jpg' };
    fixture.detectChanges();

    const photoButton = fixture.nativeElement.querySelector('[data-testid="employe-photo-button"]') as HTMLButtonElement;
    photoButton.click();
    fixture.detectChanges();

    expect(component.lightboxOpen).toBeTrue();
    expect(fixture.nativeElement.querySelector('[data-testid="employe-photo-large"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="employe-photo-large"]')?.alt)
      .toBe('Photo de Agent Terrain');
  });

  it('fermeLaLightboxParBoutonFondEtEscape', () => {
    component.employe = { ...employe, photoUrl: 'https://cdn.example/employe-1.jpg' };
    fixture.detectChanges();
    component.openPhotoLightbox();
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('[data-testid="close-photo-lightbox"]') as HTMLButtonElement).click();
    expect(component.lightboxOpen).toBeFalse();

    component.openPhotoLightbox();
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('[data-testid="employe-photo-lightbox"]') as HTMLElement).click();
    expect(component.lightboxOpen).toBeFalse();

    component.openPhotoLightbox();
    component.closePhotoLightboxOnEscape();
    expect(component.lightboxOpen).toBeFalse();
  });

  it('avatarSansPhotoNeOuvrePasDeLightbox', () => {
    expect(fixture.nativeElement.querySelector('[data-testid="employe-photo-button"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="employe-photo-lightbox"]')).toBeNull();

    component.openPhotoLightbox();

    expect(component.lightboxOpen).toBeFalse();
  });

  it('reste consultative sans upload ni bouton de modification photo', () => {
    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelector('input[type="file"]')).toBeNull();
    expect(root.textContent).not.toContain('Modifier la photo');
    expect(root.textContent).not.toContain('Téléverser');
  });
});
