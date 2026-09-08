import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, Subject, throwError } from 'rxjs';

import { EmployeFormComponent } from './form.component';
import { EmployeService } from '../../services/employe.service';
import { UtilisateurService } from '../../../utilisateurs/services/utilisateur.service';
import { AgenceService } from '../../services/agence.service';
import { SiteService } from '../../../membres/services/site.service';

class ActivatedRouteStub {
  snapshot = {
    queryParamMap: { get: () => null },
    paramMap: { get: () => null }
  };
  params = of({});
}

describe('EmployeFormComponent — rattachement site par fonction', () => {
  let fixture: ComponentFixture<EmployeFormComponent>;
  let component: EmployeFormComponent;
  let employeServiceMock: { create: jasmine.Spy; update: jasmine.Spy; getById: jasmine.Spy; uploadPhoto: jasmine.Spy; deletePhoto: jasmine.Spy };

  beforeEach(async () => {
    employeServiceMock = {
      create: jasmine.createSpy('create').and.returnValue(of({ id: 1 })),
      update: jasmine.createSpy('update').and.returnValue(of({ id: 1 })),
      getById: jasmine.createSpy('getById').and.returnValue(of({})),
      uploadPhoto: jasmine.createSpy('uploadPhoto').and.returnValue(of({ photoUrl: '/uploads/employes/1.jpg' })),
      deletePhoto: jasmine.createSpy('deletePhoto').and.returnValue(of({ photoUrl: null }))
    };

    await TestBed.configureTestingModule({
      imports: [EmployeFormComponent, ReactiveFormsModule, RouterTestingModule],
      providers: [
        { provide: EmployeService, useValue: employeServiceMock },
        { provide: UtilisateurService, useValue: { getById: () => of({}) } },
        { provide: AgenceService, useValue: { getAll: () => of([{ id: 1, nomAgence: 'AGENCE', actif: true }]) } },
        { provide: SiteService, useValue: { getAll: () => of([
          { id: 10, agenceId: 1, nomSite: 'MATERNITÉ', actif: true },
          { id: 11, agenceId: 1, nomSite: 'MÉTÉO', actif: true }
        ]) } },
        { provide: ActivatedRoute, useClass: ActivatedRouteStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function fillBaseForm(fonction: string, siteId: number | '' = ''): void {
    component.form.patchValue({
      nom: 'Falck',
      prenom: 'Jean',
      telephone: '+243812345678',
      fonction,
      agenceId: 1,
      siteId,
      dateEmbauche: '2026-01-15',
      salaireBase: 250000,
      primeFixe: 0,
      bonusVariable: 0,
      actif: true
    });
  }

  function getSiteSelect(): HTMLSelectElement | null {
    return fixture.nativeElement.querySelector('select[formControlName="siteId"]');
  }

  function renderedText(): string {
    fixture.detectChanges();
    return fixture.nativeElement.textContent as string;
  }

  function expectSiteChampMasque(): void {
    const text = renderedText();
    expect(getSiteSelect()).toBeNull();
    expect(text).not.toContain('-- Sélectionner un site --');
    expect(text).not.toContain('MATERNITÉ');
    expect(text).not.toContain('MÉTÉO');
  }

  it('formulaireAgentTerrainAfficheSite — AGENT_TERRAIN affiche le champ Site', () => {
    fillBaseForm('AGENT_TERRAIN', 10);

    const text = renderedText();

    expect(getSiteSelect()).not.toBeNull();
    expect(text).toContain('-- Sélectionner un site --');
    expect(text).toContain('MATERNITÉ');
    expect(text).toContain('MÉTÉO');
  });

  it('siteNonObligatoirePourCOO — COO sans site reste valide', () => {
    fillBaseForm('COO');

    expect(component.form.get('siteId')?.hasError('required')).toBeFalse();
    expect(component.form.valid).toBeTrue();
  });

  it('formulaireCooMasqueSite — COO masque complètement le champ Site', () => {
    fillBaseForm('COO');

    expect(component.isTransverseFunction).toBeTrue();
    expectSiteChampMasque();
  });

  it('formulaireCooAfficheAideTransverse — COO explique la couverture toutes agences', () => {
    fillBaseForm('COO');

    expect(renderedText()).toContain('Affectation opérationnelle : Toutes les agences');
    expect(renderedText()).toContain('Cette fonction est transverse');
  });

  it('formulaireGestionnaireMasqueSite — GESTIONNAIRE masque complètement le champ Site', () => {
    fillBaseForm('GESTIONNAIRE');

    expect(component.form.get('siteId')?.hasError('required')).toBeFalse();
    expect(component.form.valid).toBeTrue();
    expectSiteChampMasque();
  });

  it('formulaireControleurMasqueSite — CONTROLEUR masque complètement le champ Site', () => {
    fillBaseForm('CONTROLEUR');

    expect(component.form.get('siteId')?.hasError('required')).toBeFalse();
    expect(component.form.valid).toBeTrue();
    expectSiteChampMasque();
  });

  it('formulaireCaissierMasqueSite — CAISSIER masque complètement le champ Site', () => {
    fillBaseForm('CAISSIER');

    expect(component.form.get('siteId')?.hasError('required')).toBeFalse();
    expect(component.form.valid).toBeTrue();
    expectSiteChampMasque();
  });

  it('formulaireChefBureauMasqueSite — CHEF_BUREAU masque complètement le champ Site', () => {
    fillBaseForm('CHEF_BUREAU');

    expect(component.form.get('siteId')?.hasError('required')).toBeFalse();
    expect(component.form.valid).toBeTrue();
    expectSiteChampMasque();
  });

  it('siteNonObligatoirePourRCI — RCI sans site reste valide', () => {
    fillBaseForm('RCI');

    expect(component.form.get('siteId')?.hasError('required')).toBeFalse();
    expect(component.form.valid).toBeTrue();
  });

  it('formulaireRciMasqueSite — RCI masque complètement le champ Site', () => {
    fillBaseForm('RCI');

    expect(component.isTransverseFunction).toBeTrue();
    expectSiteChampMasque();
  });

  it('formulaireRciAfficheAideTransverse — RCI explique la couverture toutes agences', () => {
    fillBaseForm('RCI');

    expect(renderedText()).toContain('Affectation opérationnelle : Toutes les agences');
    expect(renderedText()).toContain('Aucun site à sélectionner pour une fonction transverse.');
  });

  it('siteNonObligatoirePourGerantGeneral — GERANT_GENERAL sans site reste valide', () => {
    fillBaseForm('GERANT_GENERAL');

    expect(component.form.get('siteId')?.hasError('required')).toBeFalse();
    expect(component.form.valid).toBeTrue();
  });

  it('formulaireAgentTerrainSiteObligatoire — AGENT_TERRAIN sans site reste bloqué', () => {
    fillBaseForm('AGENT_TERRAIN');
    component.form.get('siteId')?.markAsTouched();
    fixture.detectChanges();

    expect(getSiteSelect()).not.toBeNull();
    expect(component.form.get('siteId')?.hasError('required')).toBeTrue();
    expect(component.form.valid).toBeFalse();
    expect(fixture.nativeElement.textContent as string).toContain('Le site est obligatoire pour un Agent Terrain.');
  });

  it('messageSiteUniquementAgentTerrainAffiche — l’aide explique que le site concerne les Agents Terrain', () => {
    fillBaseForm('GESTIONNAIRE');
    expect(renderedText()).toContain('Le site concerne uniquement les Agents Terrain.');
  });

  it('payloadNonAgentTerrainEnvoieSiteIdNull — GESTIONNAIRE envoie siteId null', () => {
    fillBaseForm('GESTIONNAIRE');

    component.onSubmit();

    expect(employeServiceMock.create).toHaveBeenCalled();
    expect(employeServiceMock.create.calls.mostRecent().args[0].siteId).toBeNull();
  });

  it('photoUrlEstEnvoyeDansLePayload — la sauvegarde conserve l URL photo', () => {
    fillBaseForm('GESTIONNAIRE');
    component.isEditMode = true;
    component.employeId = 1;
    component.form.get('photoUrl')?.setValue('https://cdn.example/employe-1.jpg');

    component.onSubmit();

    expect(employeServiceMock.update.calls.mostRecent().args[1].photoUrl)
      .toBe('https://cdn.example/employe-1.jpg');
  });

  it('retirerPhotoVideLeChampEtLePayload — le retrait envoie une valeur nulle', () => {
    fillBaseForm('GESTIONNAIRE');
    component.isEditMode = true;
    component.employeId = 1;
    component.form.get('photoUrl')?.setValue('https://cdn.example/employe-1.jpg');

    component.clearPhoto();
    component.onSubmit();

    expect(component.form.get('photoUrl')?.value).toBe('');
    expect(employeServiceMock.update.calls.mostRecent().args[1].photoUrl).toBeNull();
  });

  it('formulairePhotoAfficheChampEtApercuSansUpload', () => {
    fillBaseForm('GESTIONNAIRE');
    component.form.get('photoUrl')?.setValue('https://cdn.example/employe-1.jpg');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('input[formControlName="photoUrl"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('img[alt*="Aperçu photo"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('input[type="file"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="choose-photo-button"]')?.textContent)
      .toContain('Importer une photo');
    expect(fixture.nativeElement.textContent).toContain('Ou coller une URL de photo');
  });

  it('boutonImporterUnePhotoDeclencheLeSelecteurFichier', () => {
    const fileInput = fixture.nativeElement.querySelector('input[type="file"]') as HTMLInputElement;
    const clickSpy = spyOn(fileInput, 'click');
    const button = fixture.nativeElement.querySelector('[data-testid="choose-photo-button"]') as HTMLButtonElement;

    button.click();

    expect(clickSpy).toHaveBeenCalled();
  });

  it('selectionPhotoValideDeclencheUploadEtMetAJourLeFormulaire', () => {
    fillBaseForm('GESTIONNAIRE');
    component.isEditMode = true;
    component.employeId = 1;
    const file = new File(['photo'], 'photo.png', { type: 'image/png' });

    component.onPhotoSelected({ target: { files: [file] } } as unknown as Event);

    expect(employeServiceMock.uploadPhoto).toHaveBeenCalledWith(1, file);
    expect(component.form.get('photoUrl')?.value).toBe('/uploads/employes/1.jpg');
    expect(component.photoSuccess).toBe('Photo importée avec succès');
  });

  it('afficheLeMessagePendantUploadEtEnCasDEchecBackend', () => {
    const uploadSubject = new Subject<any>();
    employeServiceMock.uploadPhoto.and.returnValue(uploadSubject);
    fillBaseForm('GESTIONNAIRE');
    component.isEditMode = true;
    component.employeId = 1;
    const file = new File(['photo'], 'photo.jpg', { type: 'image/jpeg' });

    component.onPhotoSelected({ target: { files: [file] } } as unknown as Event);
    expect(component.photoUploading).toBeTrue();

    uploadSubject.error({ error: { message: 'Fichier refusé par le serveur' } });
    expect(component.photoUploading).toBeFalse();
    expect(component.photoError).toBe('Fichier refusé par le serveur');
  });

  it('selectionFichierNonImageEstRefusee', () => {
    fillBaseForm('GESTIONNAIRE');
    component.isEditMode = true;
    component.employeId = 1;
    const file = new File(['document'], 'document.pdf', { type: 'application/pdf' });

    component.onPhotoSelected({ target: { files: [file] } } as unknown as Event);

    expect(employeServiceMock.uploadPhoto).not.toHaveBeenCalled();
    expect(component.photoError).toContain('JPEG');
  });

  it('selectionPhotoTropGrandeEstRefuseeAvecMessageClair', () => {
    fillBaseForm('GESTIONNAIRE');
    component.isEditMode = true;
    component.employeId = 1;
    const file = new File([new Uint8Array(2 * 1024 * 1024 + 1)], 'photo.jpg', { type: 'image/jpeg' });

    component.onPhotoSelected({ target: { files: [file] } } as unknown as Event);

    expect(employeServiceMock.uploadPhoto).not.toHaveBeenCalled();
    expect(component.photoError).toBe('La photo ne doit pas dépasser 2 Mo');
  });

  it('afficheUneErreurSiLaPhotoNePeutPasEtreAfficheeSansSupprimerViaApi', () => {
    component.form.get('photoUrl')?.setValue('/uploads/employes/photo.jpg');
    component.onPhotoPreviewError();

    expect(component.form.get('photoUrl')?.value).toBe('');
    expect(component.photoError).toContain('impossible à afficher');
    expect(employeServiceMock.deletePhoto).not.toHaveBeenCalled();
  });

  it('changementAgentTerrainVersChefBureauVideSiteEtMasqueChamp — le site est vidé et masqué', () => {
    fillBaseForm('AGENT_TERRAIN', 10);
    fixture.detectChanges();
    expect(component.form.get('siteId')?.value).toBe(10);
    expect(getSiteSelect()).not.toBeNull();

    component.form.get('fonction')?.setValue('CHEF_BUREAU');

    expect(component.form.get('siteId')?.value).toBe('');
    expect(component.form.get('siteId')?.hasError('required')).toBeFalse();
    expectSiteChampMasque();
  });

  it('ancienSiteNonTerrainEstNettoyeAuSubmit — un ancien site en édition part à null', () => {
    component.isEditMode = true;
    component.employeId = 99;
    fillBaseForm('CONTROLEUR', 10);

    component.onSubmit();

    expect(employeServiceMock.update).toHaveBeenCalled();
    expect(employeServiceMock.update.calls.mostRecent().args[1].siteId).toBeNull();
  });

  it('afficheErreurBackendTelephoneDoublon — message source backend conservé', () => {
    employeServiceMock.create.and.returnValue(throwError(() => ({
      error: { message: 'Ce numéro de téléphone est déjà utilisé.' }
    })));
    fillBaseForm('GESTIONNAIRE');

    component.onSubmit();

    expect(component.error).toBe('Ce numéro de téléphone est déjà utilisé.');
  });

  it('afficheErreurBackendPrenomNomDoublon — message source backend conservé', () => {
    employeServiceMock.create.and.returnValue(throwError(() => ({
      error: { message: 'Une personne avec le même prénom et le même nom existe déjà.' }
    })));
    fillBaseForm('GESTIONNAIRE');

    component.onSubmit();

    expect(component.error).toBe('Une personne avec le même prénom et le même nom existe déjà.');
  });

  it('memePrenomOuMemeNomSeulNonBloqueCoteFrontend — le submit reste envoyé au backend', () => {
    fillBaseForm('GESTIONNAIRE');

    component.onSubmit();

    expect(employeServiceMock.create).toHaveBeenCalledTimes(1);
  });
});
