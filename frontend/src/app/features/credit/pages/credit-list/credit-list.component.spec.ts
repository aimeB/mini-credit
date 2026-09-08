import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { throwError, of } from 'rxjs';

import { CreditListComponent } from './credit-list.component';
import { CreditService } from '../../services/credit.service';
import { SessionCaisseService } from '../../../caisse/services/session-caisse.service';
import { AuthService } from '../../../../core/services/auth.service';

describe('CreditListComponent', () => {
  let fixture: ComponentFixture<CreditListComponent>;

  const credit = {
    id: 1,
    numeroCredit: 'CR-001',
    membreNomComplet: 'Membre Test',
    siteNom: 'Site 1',
    dateApprobation: new Date().toISOString(),
    montantOctroye: 1000,
    devise: 'CDF',
    encoursPrincipal: 1000,
    totalARembourser: 1200,
    tauxInteret: 2,
    statut: 'APPROUVE'
  };

  let creditServiceMock: any;

  const sessionCaisseServiceMock = {
    getSessionActive: jasmine.createSpy('getSessionActive').and.returnValue(of({ id: 10, caisseCode: 'CAI-01' }))
  };

  const authServiceMock = {
    getCurrentUser: jasmine.createSpy('getCurrentUser').and.returnValue({ id: 7, role: 'CAISSIER', permissions: [] }),
    hasAnyRole: jasmine.createSpy('hasAnyRole').and.callFake((roles: string[]) => roles.includes(authServiceMock.getCurrentUser()?.role))
  };

  beforeEach(async () => {
    creditServiceMock = {
      getAll: jasmine.createSpy('getAll').and.returnValue(of([credit])),
      getCreditsADecaisser: jasmine.createSpy('getCreditsADecaisser').and.returnValue(of([credit])),
      getById: jasmine.createSpy('getById').and.returnValue(of(credit)),
      decaisserCredit: jasmine.createSpy('decaisserCredit').and.returnValue(of({}))
    };

    await TestBed.configureTestingModule({
      imports: [CreditListComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => null }, paramMap: { get: () => null } } } },
        { provide: CreditService, useValue: creditServiceMock },
        { provide: SessionCaisseService, useValue: sessionCaisseServiceMock },
        { provide: AuthService, useValue: authServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreditListComponent);
    fixture.detectChanges();
  });

  it('affiche la guidance globale et la guidance de statut du crédit', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Suivi du dossier crédit');
    expect(text).toContain('Crédit approuvé');
  });

  it('charge les crédits approuvés à décaisser via l endpoint dédié CAISSIER', () => {
    const component = fixture.componentInstance;

    expect(creditServiceMock.getCreditsADecaisser).toHaveBeenCalled();
    expect(creditServiceMock.getAll).not.toHaveBeenCalled();
    expect(component.loading).toBeFalse();
    expect(component.credits.length).toBe(1);
    expect(component.credits[0].statut).toBe('APPROUVE');
  });

  it('reserve le bouton décaisser au rôle CAISSIER', () => {
    const component = fixture.componentInstance;

    expect(component.canDecaisser(credit as any)).toBeTrue();

    authServiceMock.getCurrentUser.and.returnValue({ id: 8, role: 'GESTIONNAIRE', permissions: [] });

    expect(component.canDecaisser(credit as any)).toBeFalse();
  });

  it('termine le chargement et affiche une erreur si les crédits approuvés échouent', async () => {
    TestBed.resetTestingModule();

    creditServiceMock = {
      getAll: jasmine.createSpy('getAll').and.returnValue(of([credit])),
      getCreditsADecaisser: jasmine.createSpy('getCreditsADecaisser').and.returnValue(throwError(() => new Error('403'))),
      getById: jasmine.createSpy('getById').and.returnValue(of(credit)),
      decaisserCredit: jasmine.createSpy('decaisserCredit').and.returnValue(of({}))
    };

    await TestBed.configureTestingModule({
      imports: [CreditListComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => null }, paramMap: { get: () => null } } } },
        { provide: CreditService, useValue: creditServiceMock },
        { provide: SessionCaisseService, useValue: sessionCaisseServiceMock },
        { provide: AuthService, useValue: authServiceMock }
      ]
    }).compileComponents();

    const errorFixture = TestBed.createComponent(CreditListComponent);
    errorFixture.detectChanges();
    const component = errorFixture.componentInstance;

    expect(component.loading).toBeFalse();
    expect(component.errorMessage).toBe('Impossible de charger les crédits approuvés.');
    expect(errorFixture.nativeElement.textContent).toContain('Impossible de charger les crédits approuvés.');
  });

  it('charge uniquement le crédit ciblé sans appeler la liste agents sur la route décaissement', async () => {
    TestBed.resetTestingModule();

    creditServiceMock = {
      getAll: jasmine.createSpy('getAll').and.returnValue(of([credit])),
      getCreditsADecaisser: jasmine.createSpy('getCreditsADecaisser').and.returnValue(of([credit])),
      getById: jasmine.createSpy('getById').and.returnValue(of(credit)),
      decaisserCredit: jasmine.createSpy('decaisserCredit').and.returnValue(of({}))
    };

    await TestBed.configureTestingModule({
      imports: [CreditListComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => null }, paramMap: { get: (key: string) => key === 'creditId' ? '1' : null } } } },
        { provide: CreditService, useValue: creditServiceMock },
        { provide: SessionCaisseService, useValue: sessionCaisseServiceMock },
        { provide: AuthService, useValue: authServiceMock }
      ]
    }).compileComponents();

    const routeFixture = TestBed.createComponent(CreditListComponent);
    routeFixture.detectChanges();
    const component = routeFixture.componentInstance;

    expect(creditServiceMock.getById).toHaveBeenCalledWith(1);
    expect(creditServiceMock.getAll).not.toHaveBeenCalled();
    expect(component.decaissementModalOpen).toBeTrue();
    expect(component.selectedCredit?.id).toBe(1);
  });

  it('affiche l agent terrain du dossier en lecture seule dans le popup de décaissement', () => {
    const component = fixture.componentInstance;

    component.ouvrirPopupDecaissement({
      ...credit,
      agentTerrainId: 12,
      agentTerrainNom: 'Agent Terrain Associé'
    } as any);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Agent terrain');
    expect(text).toContain('Agent Terrain Associé');
    expect(text).not.toContain('-- Aucun agent associé au dossier --');
    expect(fixture.nativeElement.querySelector('select[name="agentId"]')).toBeNull();
  });

  it('affiche le message aucun agent quand le dossier crédit n a pas d agent associé', () => {
    const component = fixture.componentInstance;

    component.ouvrirPopupDecaissement({
      ...credit,
      agentTerrainId: null,
      agentTerrainNom: null
    } as any);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('-- Aucun agent associé au dossier --');
    expect(fixture.nativeElement.querySelector('select[name="agentId"]')).toBeNull();
  });

  it('n envoie pas l agent terrain dans le payload de décaissement', () => {
    const component = fixture.componentInstance;

    component.ouvrirPopupDecaissement({
      ...credit,
      agentTerrainId: 12,
      agentTerrainNom: 'Agent Terrain Associé'
    } as any);
    component.confirmerDecaissement();

    const payload = creditServiceMock.decaisserCredit.calls.mostRecent().args[1];
    expect(payload.agentId).toBeUndefined();
    expect(payload.agentTerrainId).toBeUndefined();
  });
});
