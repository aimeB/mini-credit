import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { SessionCaisseFormComponent } from './session-caisse-form.component';
import { CaisseService } from '../../services/caisse.service';
import { SessionCaisseService } from '../../services/session-caisse.service';
import { AuthService } from '../../../../core/services/auth.service';

describe('SessionCaisseFormComponent', () => {
  let component: SessionCaisseFormComponent;
  let fixture: ComponentFixture<SessionCaisseFormComponent>;
  let caisseServiceSpy: jasmine.SpyObj<CaisseService>;
  let sessionServiceSpy: jasmine.SpyObj<SessionCaisseService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    caisseServiceSpy = jasmine.createSpyObj<CaisseService>('CaisseService', ['getAccessibles']);
    sessionServiceSpy = jasmine.createSpyObj<SessionCaisseService>('SessionCaisseService', ['ouvrir', 'getOuvertureContext']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);

    caisseServiceSpy.getAccessibles.and.returnValue(of([
      { id: 1, codeCaisse: 'CAI1', libelle: 'Caisse A', siteId: 1, siteNom: 'Site A', devise: 'CDF', actif: true, createdAt: '', updatedAt: '' }
    ] as any));

    authServiceSpy.getCurrentUser.and.returnValue({
      id: 10,
      username: 'cashier',
      nomComplet: 'Caissier Connecté',
      email: 'c@x.com',
      role: 'CAISSIER'
    } as any);

    sessionServiceSpy.ouvrir.and.returnValue(of({ id: 99 } as any));
    sessionServiceSpy.getOuvertureContext.and.returnValue(of({
      caisseId: 1,
      dateComptable: new Date().toISOString().slice(0, 10),
      devise: 'CDF',
      premiereSession: true,
      soldeOuvertureAutomatique: 0,
      forcageAutorise: false,
      soldeVerrouille: false,
      sessionExistante: false
    } as any));

    await TestBed.configureTestingModule({
      imports: [SessionCaisseFormComponent],
      providers: [
        provideRouter([]),
        { provide: CaisseService, useValue: caisseServiceSpy },
        { provide: SessionCaisseService, useValue: sessionServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SessionCaisseFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('n affiche pas de champ Utilisateur ID manuel', () => {
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).not.toContain('Utilisateur ID');
  });

  it('affiche les caisses actives dans la liste', () => {
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('CAI1');
  });

  it('affiche un message de préparation et pas de session ouverte avant création', () => {
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain("Préparation de l'ouverture de session");
    expect(html.textContent).not.toContain('Session de caisse ouverte');
  });

  it('affiche le bloc de conflit uniquement si une session active existe', () => {
    component.openingContext = {
      caisseId: 1,
      dateComptable: new Date().toISOString().slice(0, 10),
      devise: 'CDF',
      premiereSession: false,
      soldeOuvertureAutomatique: 100,
      forcageAutorise: false,
      soldeVerrouille: true,
      sessionExistante: true,
      sessionExistanteId: 12,
      sessionExistanteStatut: 'PRE_CLOTUREE',
      sessionExistanteDateOuverture: new Date().toISOString(),
      sessionExistanteUtilisateurNom: 'Caissier X'
    } as any;
    component.existingSessionMessage = 'Une session active existe déjà pour cette caisse.';
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Une session active existe déjà');
  });

  it('affiche un message clair si aucune caisse active', () => {
    caisseServiceSpy.getAccessibles.and.returnValue(of([]));
    component.loadCaisses();
    fixture.detectChanges();

    expect(component.error).toContain('Aucune caisse active disponible');
  });

  it('n utilise pas siteNom comme valeur par défaut d antenne', () => {
    component.selectedCaisse = {
      id: 1,
      codeCaisse: 'CAI1',
      libelle: 'Caisse A',
      siteId: 1,
      siteNom: 'Site de Sakombi',
      devise: 'CDF',
      actif: true,
      createdAt: '',
      updatedAt: ''
    } as any;

    expect(component.antenneDisplayName).toBe('Non renseignée');
  });

  it('affiche antenneNom quand la source réelle existe', () => {
    component.selectedCaisse = {
      id: 1,
      codeCaisse: 'CAI1',
      libelle: 'Caisse A',
      siteId: 1,
      siteNom: 'Site de Sakombi',
      antenneNom: 'Antenne Sakombi Centre',
      devise: 'CDF',
      actif: true,
      createdAt: '',
      updatedAt: ''
    } as any;

    expect(component.antenneDisplayName).toBe('Antenne Sakombi Centre');
  });

  it('verrouille le solde et affiche le report quand une session precedente existe', () => {
    (component as any).applyOpeningContext({
      caisseId: 1,
      dateComptable: new Date().toISOString().slice(0, 10),
      devise: 'CDF',
      premiereSession: false,
      soldeOuvertureAutomatique: 1048000,
      forcageAutorise: true,
      soldeVerrouille: false,
      sessionExistante: false
    });
    component.openingContext = {
      caisseId: 1,
      dateComptable: new Date().toISOString().slice(0, 10),
      devise: 'CDF',
      premiereSession: false,
      soldeOuvertureAutomatique: 1048000,
      forcageAutorise: true,
      soldeVerrouille: false,
      sessionExistante: false
    } as any;
    fixture.detectChanges();

    expect(component.form.get('soldeOuverture')?.disabled).toBeTrue();
    expect(component.form.getRawValue().soldeOuverture).toBe(1048000);
    const text = (fixture.nativeElement.textContent as string).replace(/[\u202f\u00a0]/g, ' ');
    expect(text).toContain('Solde reporté de la session précédente : 1 048 000 CDF');
  });
});
