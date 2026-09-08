import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { CaisseFormComponent } from './caisse-form.component';
import { CaisseService } from '../../services/caisse.service';
import { SiteService } from '../../../../shared/services/site.service';
import { AgenceService } from '../../../employes/services/agence.service';
import { UtilisateurService } from '../../../utilisateurs/services/utilisateur.service';
import { AuthService } from '../../../../core/services/auth.service';

describe('CaisseFormComponent', () => {
  let component: CaisseFormComponent;
  let fixture: ComponentFixture<CaisseFormComponent>;
  let caisseServiceSpy: jasmine.SpyObj<CaisseService>;
  let agenceServiceSpy: jasmine.SpyObj<AgenceService>;
  let siteServiceSpy: jasmine.SpyObj<SiteService>;
  let utilisateurServiceSpy: jasmine.SpyObj<UtilisateurService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    caisseServiceSpy = jasmine.createSpyObj<CaisseService>('CaisseService', ['create']);
    agenceServiceSpy = jasmine.createSpyObj<AgenceService>('AgenceService', ['getAll']);
    siteServiceSpy = jasmine.createSpyObj<SiteService>('SiteService', ['getActifs']);
    utilisateurServiceSpy = jasmine.createSpyObj<UtilisateurService>('UtilisateurService', ['getByRole']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser', 'hasRole']);

    caisseServiceSpy.create.and.returnValue(of({
      id: 1,
      codeCaisse: 'CAI202606160001',
      libelle: 'Caisse Test',
      agenceId: 1,
      agenceNom: 'Agence Centrale',
      siteId: 1,
      siteNom: 'Site A',
      devise: 'CDF',
      actif: true,
      createdAt: '2026-06-16T10:00:00',
      updatedAt: '2026-06-16T10:00:00'
    } as any));

    agenceServiceSpy.getAll.and.returnValue(of([
      { id: 1, codeAgence: 'AG01', nomAgence: 'Agence Centrale', actif: true }
    ] as any));

    siteServiceSpy.getActifs.and.returnValue(of([
      { id: 1, codeSite: 'S1', nomSite: 'Site A', zone: 'Zone 1', actif: true, agenceId: 1, nomAgence: 'Agence', villeAgence: null, communeAgence: null }
    ] as any));

    utilisateurServiceSpy.getByRole.and.returnValue(of([
      { id: 10, username: 'cashier', nomComplet: 'Caissier Test', active: true, roles: ['CAISSIER'] }
    ] as any));

    authServiceSpy.getCurrentUser.and.returnValue({
      id: 99,
      username: 'admin',
      email: 'admin@test.local',
      nomComplet: 'Admin',
      role: 'ADMIN'
    } as any);
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'ADMIN');

    await TestBed.configureTestingModule({
      imports: [CaisseFormComponent],
      providers: [
        provideRouter([]),
        { provide: CaisseService, useValue: caisseServiceSpy },
        { provide: AgenceService, useValue: agenceServiceSpy },
        { provide: SiteService, useValue: siteServiceSpy },
        { provide: UtilisateurService, useValue: utilisateurServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CaisseFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('affiche la liste déroulante des agences', () => {
    fixture.detectChanges();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Agence Centrale');
  });

  it('affiche la liste déroulante des caissiers', () => {
    fixture.detectChanges();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Caissier Test');
  });

  it('affiche le code caisse en lecture seule (généré)', () => {
    const input = fixture.nativeElement.querySelector('input[readonly]') as HTMLInputElement;
    expect(input).toBeTruthy();
    expect(input.value).toContain('Généré automatiquement');
  });

  it('pour un CAISSIER sans site, affiche un message clair', () => {
    authServiceSpy.getCurrentUser.and.returnValue({
      id: 10,
      username: 'cashier',
      email: 'cashier@test.local',
      nomComplet: 'Caissier Test',
      role: 'CAISSIER',
      siteId: undefined
    } as any);
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'CAISSIER');

    fixture = TestBed.createComponent(CaisseFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.error).toContain("n'est rattaché à aucune agence");
  });

  it('pour un CAISSIER avec site, le formulaire fixe son agence et son site', () => {
    siteServiceSpy.getActifs.and.returnValue(of([
      { id: 1, codeSite: 'S1', nomSite: 'Site A', zone: 'Zone 1', actif: true, agenceId: 1, nomAgence: 'Agence', villeAgence: null, communeAgence: null },
      { id: 2, codeSite: 'S2', nomSite: 'Site B', zone: 'Zone 2', actif: true, agenceId: 1, nomAgence: 'Agence', villeAgence: null, communeAgence: null }
    ] as any));
    authServiceSpy.getCurrentUser.and.returnValue({
      id: 10,
      username: 'cashier',
      email: 'cashier@test.local',
      nomComplet: 'Caissier Test',
      role: 'CAISSIER',
      siteId: 2
    } as any);
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'CAISSIER');

    fixture = TestBed.createComponent(CaisseFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.currentUserAgenceId).toBe(1);
    expect(component.form.get('agenceId')?.value).toBe(1);
    expect(component.form.get('siteId')?.value).toBe(2);
  });

  it('envoie agenceId et siteId via getRawValue même si les champs sont disabled', () => {
    siteServiceSpy.getActifs.and.returnValue(of([
      { id: 2, codeSite: 'S2', nomSite: 'Site B', zone: 'Zone 2', actif: true, agenceId: 1, nomAgence: 'Agence', villeAgence: null, communeAgence: null }
    ] as any));
    authServiceSpy.getCurrentUser.and.returnValue({
      id: 10,
      username: 'cashier',
      email: 'cashier@test.local',
      nomComplet: 'Caissier Test',
      role: 'CAISSIER',
      siteId: 2
    } as any);
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'CAISSIER');

    fixture = TestBed.createComponent(CaisseFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.patchValue({
      libelle: 'Caisse principal',
      devise: 'CDF'
    });

    component.submit();

    expect(caisseServiceSpy.create).toHaveBeenCalled();
    const payload = caisseServiceSpy.create.calls.mostRecent().args[0] as any;
    expect(payload.agenceId).toBe(1);
    expect(payload.siteId).toBe(2);
    expect(payload.caissierAffecteId).toBe(10);
  });

  it('envoie siteId undefined si non selectionne par un admin', () => {
    component.form.patchValue({
      libelle: 'Caisse agence centrale',
      devise: 'CDF',
      agenceId: 1,
      siteId: null
    });

    component.submit();

    expect(caisseServiceSpy.create).toHaveBeenCalled();
    const payload = caisseServiceSpy.create.calls.mostRecent().args[0] as any;
    expect(payload.agenceId).toBe(1);
    expect(payload.siteId).toBeUndefined();
  });
});
