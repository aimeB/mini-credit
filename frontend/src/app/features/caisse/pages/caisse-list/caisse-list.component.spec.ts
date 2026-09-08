import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { CaisseListComponent } from './caisse-list.component';
import { CaisseService } from '../../services/caisse.service';
import { SessionCaisseService } from '../../services/session-caisse.service';
import { AuthService } from '../../../../core/services/auth.service';

describe('CaisseListComponent', () => {
  let component: CaisseListComponent;
  let fixture: ComponentFixture<CaisseListComponent>;
  let caisseServiceSpy: jasmine.SpyObj<CaisseService>;
  let sessionServiceSpy: jasmine.SpyObj<SessionCaisseService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    caisseServiceSpy = jasmine.createSpyObj<CaisseService>('CaisseService', ['getAccessibles', 'initialiserMaCaisse']);
    sessionServiceSpy = jasmine.createSpyObj<SessionCaisseService>('SessionCaisseService', ['getAll', 'cloturerFinale']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['hasRole', 'hasAnyRole', 'hasPermission']);

    caisseServiceSpy.getAccessibles.and.returnValue(of([]));
    caisseServiceSpy.initialiserMaCaisse.and.returnValue(of({
      id: 1,
      codeCaisse: 'CAI-001',
      libelle: 'Caisse principale',
      devise: 'CDF',
      actif: true,
      soldeDisponibleActuel: 0,
      createdAt: '2026-07-28T00:00:00',
      updatedAt: '2026-07-28T00:00:00'
    }));
    sessionServiceSpy.getAll.and.returnValue(of([]));
    sessionServiceSpy.cloturerFinale.and.returnValue(of({ id: 1, statut: 'CLOTUREE' } as any));
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'CAISSIER');
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CAISSIER'));
    authServiceSpy.hasPermission.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [CaisseListComponent],
      providers: [
        provideRouter([]),
        { provide: CaisseService, useValue: caisseServiceSpy },
        { provide: SessionCaisseService, useValue: sessionServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('affiche Initialiser ma caisse si CAISSIER sans caisse accessible', () => {
    fixture.detectChanges();
    const html = fixture.nativeElement as HTMLElement;

    expect(html.textContent).toContain('Aucune caisse trouvée');
    expect(html.textContent).toContain('Initialiser ma caisse');
  });

  it('aucuneCaisseNaffichePasSessionCloturee', () => {
    fixture.detectChanges();
    const html = fixture.nativeElement as HTMLElement;

    expect(html.textContent).not.toContain('Session clôturée');
    expect(html.textContent).not.toContain('Vous ne pouvez pas agir à cette étape');
    expect(component.globalGuidance).toBeNull();
  });

  it('clicInitialiserAppelleEndpointEtRechargeListe', () => {
    caisseServiceSpy.getAccessibles.and.returnValues(
      of([]),
      of([{
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse principale',
        devise: 'CDF',
        actif: true,
        createdAt: '2026-07-28T00:00:00',
        updatedAt: '2026-07-28T00:00:00'
      }] as any)
    );

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const button = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'))
      .find((candidate) => candidate.textContent?.includes('Initialiser ma caisse')) as HTMLButtonElement;
    button.click();
    fixture.detectChanges();

    expect(caisseServiceSpy.initialiserMaCaisse).toHaveBeenCalledTimes(1);
    expect(caisseServiceSpy.getAccessibles.calls.count()).toBeGreaterThanOrEqual(2);
    expect(component.successMessage).toContain('Caisse initialisée');
  });

  it('rciNeVoitPasInitialiserMaCaisse', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'RCI');
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('RCI'));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Initialiser ma caisse');
  });

  it('caissierNeVoitPasNouvelleCaisseLibre', () => {
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('+ Nouvelle caisse');
    expect(component.canCreateCaisse).toBeFalse();
  });

  it('affiche Ouvrir une session si CAISSIER avec caisse sans session', () => {
    caisseServiceSpy.getAccessibles.and.returnValue(of([
      {
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse A',
        siteId: 10,
        siteNom: 'Site A',
        devise: 'CDF',
        actif: true,
        createdAt: '2026-06-18T00:00:00',
        updatedAt: '2026-06-18T00:00:00'
      }
    ] as any));
    sessionServiceSpy.getAll.and.returnValue(of([]));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Ouvrir');
    expect(html.textContent).toContain('Ouvrir une session');
  });

  it('affiche Session ouverte si session active', () => {
    caisseServiceSpy.getAccessibles.and.returnValue(of([
      {
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse A',
        siteId: 10,
        siteNom: 'Site A',
        devise: 'CDF',
        actif: true,
        createdAt: '2026-06-18T00:00:00',
        updatedAt: '2026-06-18T00:00:00'
      }
    ] as any));
    sessionServiceSpy.getAll.and.returnValue(of([
      {
        id: 100,
        caisseId: 1,
        statut: 'OUVERTE',
        dateComptable: new Date().toISOString().slice(0, 10),
        soldeTheorique: 1000
      }
    ] as any));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Session ouverte');
  });

  it('ne compte pas PRE_CLOTUREE dans les sessions ouvertes', () => {
    caisseServiceSpy.getAccessibles.and.returnValue(of([
      {
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse A',
        siteId: 10,
        siteNom: 'Site A',
        devise: 'CDF',
        actif: true,
        createdAt: '2026-06-18T00:00:00',
        updatedAt: '2026-06-18T00:00:00'
      }
    ] as any));
    sessionServiceSpy.getAll.and.returnValue(of([
      {
        id: 101,
        caisseId: 1,
        statut: 'PRE_CLOTUREE',
        dateComptable: new Date().toISOString().slice(0, 10),
        soldeTheorique: 1000
      }
    ] as any));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.nbSessionsOuvertes).toBe(0);
  });

  it('compte VALIDEE_CONTROLE comme session en attente de clôture finale', () => {
    caisseServiceSpy.getAccessibles.and.returnValue(of([
      {
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse A',
        siteId: 10,
        siteNom: 'Site A',
        devise: 'CDF',
        actif: true,
        createdAt: '2026-06-18T00:00:00',
        updatedAt: '2026-06-18T00:00:00'
      }
    ] as any));
    sessionServiceSpy.getAll.and.returnValue(of([
      {
        id: 101,
        caisseId: 1,
        statut: 'VALIDEE_CONTROLE',
        dateComptable: '2026-06-29',
        soldeTheorique: 1000
      }
    ] as any));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.nbSessionsOuvertes).toBe(0);
    expect(component.nbSessionsEnAttenteCloture).toBe(1);
  });

  it('n applique pas le flag session ancienne ouverte pour PRE_CLOTUREE', () => {
    caisseServiceSpy.getAccessibles.and.returnValue(of([
      {
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse A',
        siteId: 10,
        siteNom: 'Site A',
        devise: 'CDF',
        actif: true,
        createdAt: '2026-06-18T00:00:00',
        updatedAt: '2026-06-18T00:00:00'
      }
    ] as any));
    sessionServiceSpy.getAll.and.returnValue(of([
      {
        id: 102,
        caisseId: 1,
        statut: 'PRE_CLOTUREE',
        dateComptable: '2026-06-20',
        soldeTheorique: 1000
      }
    ] as any));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.sessionsAnciennesOuvertes.length).toBe(0);
    expect(component.globalGuidance?.currentStep).toBe('Contrôle physique');
  });

  it('bloque le CAISSIER quand le statut global est PRE_CLOTUREE', () => {
    caisseServiceSpy.getAccessibles.and.returnValue(of([
      {
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse A',
        siteId: 10,
        siteNom: 'Site A',
        devise: 'CDF',
        actif: true,
        createdAt: '2026-06-18T00:00:00',
        updatedAt: '2026-06-18T00:00:00'
      }
    ] as any));
    sessionServiceSpy.getAll.and.returnValue(of([
      {
        id: 103,
        caisseId: 1,
        statut: 'PRE_CLOTUREE',
        dateComptable: new Date().toISOString().slice(0, 10),
        soldeTheorique: 1000
      }
    ] as any));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.globalGuidance?.canCurrentUserAct).toBeFalse();
    expect(component.globalGuidance?.expectedRole).toContain('Contr');
  });

  it('masque le bouton Contrôle caisse pour CAISSIER', () => {
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => {
      // CAISSIER oui, mais pas de droit controle (ADMIN/CONTROLEUR/RCI/CHEF_BUREAU)
      return roles.includes('CAISSIER') && !roles.includes('CONTROLEUR') && !roles.includes('ADMIN')
        && !roles.includes('RCI') && !roles.includes('CHEF_BUREAU');
    });

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).not.toContain('Contrôle caisse');
  });

  it('formate les montants en CDF', () => {
    expect(component.formatCdf(50000)).toContain('CDF');
  });

  it('masque les actions d ouverture de session pour CONTROLEUR', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'CONTROLEUR');
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CONTROLEUR'));

    caisseServiceSpy.getAccessibles.and.returnValue(of([
      {
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse A',
        siteId: 10,
        siteNom: 'Site A',
        devise: 'CDF',
        actif: true,
        createdAt: '2026-06-18T00:00:00',
        updatedAt: '2026-06-18T00:00:00'
      }
    ] as any));
    sessionServiceSpy.getAll.and.returnValue(of([]));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).not.toContain('Ouvrir une session');
  });

  it('affiche Finaliser clôture pour CHEF_BUREAU sur une session VALIDEE_CONTROLE', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'CHEF_BUREAU');
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CHEF_BUREAU'));
    authServiceSpy.hasPermission.and.callFake((permission: string) => permission === 'SESSION_CAISSE_FINAL_CLOSE');

    caisseServiceSpy.getAccessibles.and.returnValue(of([
      {
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse A',
        siteId: 10,
        siteNom: 'Site A',
        devise: 'CDF',
        actif: true,
        createdAt: '2026-06-18T00:00:00',
        updatedAt: '2026-06-18T00:00:00'
      }
    ] as any));
    sessionServiceSpy.getAll.and.returnValue(of([
      {
        id: 201,
        caisseId: 1,
        statut: 'VALIDEE_CONTROLE',
        dateComptable: new Date().toISOString().slice(0, 10),
        dateOuverture: '2026-06-18T08:00:00',
        dateCloture: '2026-06-18T17:00:00',
        soldeTheorique: 1000
      }
    ] as any));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    expect(buttonLabels(html)).toContain('Clôturer définitivement');
  });

  it('affiche Clôturer définitivement pour CHEF_BUREAU sur une session VALIDEE_CONTROLE ancienne', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'CHEF_BUREAU');
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CHEF_BUREAU'));
    authServiceSpy.hasPermission.and.callFake((permission: string) => permission === 'SESSION_CAISSE_FINAL_CLOSE');

    caisseServiceSpy.getAccessibles.and.returnValue(of([
      {
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse A',
        siteId: 10,
        siteNom: 'Site A',
        devise: 'CDF',
        actif: true,
        createdAt: '2026-06-18T00:00:00',
        updatedAt: '2026-06-18T00:00:00'
      }
    ] as any));
    sessionServiceSpy.getAll.and.returnValue(of([
      {
        id: 205,
        caisseId: 1,
        statut: 'VALIDEE_CONTROLE',
        dateComptable: '2026-06-20',
        dateOuverture: '2026-06-20T08:00:00',
        dateCloture: '2026-06-20T17:00:00',
        soldeTheorique: 1000
      }
    ] as any));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    expect(buttonLabels(html)).toContain('Clôturer définitivement');
  });

  it('n affiche pas Clôturer définitivement pour CHEF_BUREAU sans permission finale', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'CHEF_BUREAU');
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CHEF_BUREAU'));
    authServiceSpy.hasPermission.and.returnValue(false);

    caisseServiceSpy.getAccessibles.and.returnValue(of([
      {
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse A',
        siteId: 10,
        siteNom: 'Site A',
        devise: 'CDF',
        actif: true,
        createdAt: '2026-06-18T00:00:00',
        updatedAt: '2026-06-18T00:00:00'
      }
    ] as any));
    sessionServiceSpy.getAll.and.returnValue(of([
      {
        id: 206,
        caisseId: 1,
        statut: 'VALIDEE_CONTROLE',
        dateComptable: new Date().toISOString().slice(0, 10),
        soldeTheorique: 1000
      }
    ] as any));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    expect(buttonLabels(html)).not.toContain('Clôturer définitivement');
  });

  it('n affiche pas Finaliser clôture pour CONTROLEUR', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'CONTROLEUR');
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CONTROLEUR'));

    caisseServiceSpy.getAccessibles.and.returnValue(of([
      {
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse A',
        siteId: 10,
        siteNom: 'Site A',
        devise: 'CDF',
        actif: true,
        createdAt: '2026-06-18T00:00:00',
        updatedAt: '2026-06-18T00:00:00'
      }
    ] as any));
    sessionServiceSpy.getAll.and.returnValue(of([
      {
        id: 202,
        caisseId: 1,
        statut: 'VALIDEE_CONTROLE',
        dateComptable: new Date().toISOString().slice(0, 10),
        dateOuverture: '2026-06-18T08:00:00',
        dateCloture: '2026-06-18T17:00:00',
        soldeTheorique: 1000
      }
    ] as any));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    expect(buttonLabels(html)).not.toContain('Clôturer définitivement');
  });

  it('n affiche pas Finaliser clôture pour CAISSIER', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'CAISSIER');
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CAISSIER'));

    caisseServiceSpy.getAccessibles.and.returnValue(of([
      {
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse A',
        siteId: 10,
        siteNom: 'Site A',
        devise: 'CDF',
        actif: true,
        createdAt: '2026-06-18T00:00:00',
        updatedAt: '2026-06-18T00:00:00'
      }
    ] as any));
    sessionServiceSpy.getAll.and.returnValue(of([
      {
        id: 203,
        caisseId: 1,
        statut: 'VALIDEE_CONTROLE',
        dateComptable: new Date().toISOString().slice(0, 10),
        dateOuverture: '2026-06-18T08:00:00',
        dateCloture: '2026-06-18T17:00:00',
        soldeTheorique: 1000
      }
    ] as any));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    expect(buttonLabels(html)).not.toContain('Clôturer définitivement');
  });

  it('n affiche pas Finaliser clôture pour RCI', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'RCI');
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('RCI'));

    caisseServiceSpy.getAccessibles.and.returnValue(of([
      {
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse A',
        siteId: 10,
        siteNom: 'Site A',
        devise: 'CDF',
        actif: true,
        createdAt: '2026-06-18T00:00:00',
        updatedAt: '2026-06-18T00:00:00'
      }
    ] as any));
    sessionServiceSpy.getAll.and.returnValue(of([
      {
        id: 204,
        caisseId: 1,
        statut: 'VALIDEE_CONTROLE',
        dateComptable: new Date().toISOString().slice(0, 10),
        dateOuverture: '2026-06-18T08:00:00',
        dateCloture: '2026-06-18T17:00:00',
        soldeTheorique: 1000
      }
    ] as any));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    expect(buttonLabels(html)).not.toContain('Clôturer définitivement');
  });

  it('n affiche pas Clôturer définitivement pour les rôles non autorisés même avec permission', () => {
    for (const role of ['AGENT_TERRAIN', 'COO', 'GERANT_GENERAL']) {
      authServiceSpy.hasRole.and.callFake((checkedRole: string) => checkedRole === role);
      authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes(role));
      authServiceSpy.hasPermission.and.callFake((permission: string) => permission === 'SESSION_CAISSE_FINAL_CLOSE');

      caisseServiceSpy.getAccessibles.and.returnValue(of([
        {
          id: 1,
          codeCaisse: 'CAI-001',
          libelle: 'Caisse A',
          siteId: 10,
          siteNom: 'Site A',
          devise: 'CDF',
          actif: true,
          createdAt: '2026-06-18T00:00:00',
          updatedAt: '2026-06-18T00:00:00'
        }
      ] as any));
      sessionServiceSpy.getAll.and.returnValue(of([
        {
          id: 210,
          caisseId: 1,
          statut: 'VALIDEE_CONTROLE',
          dateComptable: '2026-06-29',
          soldeTheorique: 1000
        }
      ] as any));

      fixture = TestBed.createComponent(CaisseListComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      expect(buttonLabels(fixture.nativeElement as HTMLElement)).not.toContain('Clôturer définitivement');
    }
  });

  it('n affiche jamais Clôturer définitivement pour GESTIONNAIRE', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'GESTIONNAIRE');
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('GESTIONNAIRE'));
    authServiceSpy.hasPermission.and.callFake((permission: string) => permission === 'SESSION_CAISSE_FINAL_CLOSE');

    caisseServiceSpy.getAccessibles.and.returnValue(of([
      {
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse A',
        siteId: 10,
        siteNom: 'Site A',
        devise: 'CDF',
        actif: true,
        createdAt: '2026-06-18T00:00:00',
        updatedAt: '2026-06-18T00:00:00'
      }
    ] as any));
    sessionServiceSpy.getAll.and.returnValue(of([
      {
        id: 207,
        caisseId: 1,
        statut: 'VALIDEE_CONTROLE',
        dateComptable: new Date().toISOString().slice(0, 10),
        soldeTheorique: 1000
      }
    ] as any));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    expect(buttonLabels(html)).not.toContain('Clôturer définitivement');
  });

  it('n affiche pas Clôturer définitivement pour CHEF_BUREAU sur une session OUVERTE', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'CHEF_BUREAU');
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CHEF_BUREAU'));
    authServiceSpy.hasPermission.and.callFake((permission: string) => permission === 'SESSION_CAISSE_FINAL_CLOSE');

    caisseServiceSpy.getAccessibles.and.returnValue(of([
      {
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse A',
        siteId: 10,
        siteNom: 'Site A',
        devise: 'CDF',
        actif: true,
        createdAt: '2026-06-18T00:00:00',
        updatedAt: '2026-06-18T00:00:00'
      }
    ] as any));
    sessionServiceSpy.getAll.and.returnValue(of([
      {
        id: 208,
        caisseId: 1,
        statut: 'OUVERTE',
        dateComptable: new Date().toISOString().slice(0, 10),
        soldeTheorique: 1000
      }
    ] as any));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    expect(buttonLabels(html)).not.toContain('Clôturer définitivement');
  });

  it('n affiche pas Clôturer définitivement pour CHEF_BUREAU sur une session CLOTUREE', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'CHEF_BUREAU');
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CHEF_BUREAU'));
    authServiceSpy.hasPermission.and.callFake((permission: string) => permission === 'SESSION_CAISSE_FINAL_CLOSE');

    caisseServiceSpy.getAccessibles.and.returnValue(of([
      {
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse A',
        siteId: 10,
        siteNom: 'Site A',
        devise: 'CDF',
        actif: true,
        statutSession: 'CLOTUREE',
        createdAt: '2026-06-18T00:00:00',
        updatedAt: '2026-06-18T00:00:00'
      }
    ] as any));
    sessionServiceSpy.getAll.and.returnValue(of([
      {
        id: 211,
        caisseId: 1,
        statut: 'CLOTUREE',
        dateComptable: '2026-06-29',
        soldeTheorique: 1000
      }
    ] as any));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(buttonLabels(fixture.nativeElement as HTMLElement)).not.toContain('Clôturer définitivement');
  });

  it('clique sur Clôturer définitivement et recharge la liste après succès', () => {
    authServiceSpy.hasRole.and.callFake((role: string) => role === 'CHEF_BUREAU');
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CHEF_BUREAU'));
    authServiceSpy.hasPermission.and.callFake((permission: string) => permission === 'SESSION_CAISSE_FINAL_CLOSE');
    spyOn(window, 'prompt').and.returnValue('RAS');
    spyOn(window, 'confirm').and.returnValue(true);

    caisseServiceSpy.getAccessibles.and.returnValue(of([
      {
        id: 1,
        codeCaisse: 'CAI-001',
        libelle: 'Caisse A',
        siteId: 10,
        siteNom: 'Site A',
        devise: 'CDF',
        actif: true,
        createdAt: '2026-06-18T00:00:00',
        updatedAt: '2026-06-18T00:00:00'
      }
    ] as any));
    sessionServiceSpy.getAll.and.returnValues(
      of([
        {
          id: 209,
          caisseId: 1,
          statut: 'VALIDEE_CONTROLE',
          dateComptable: new Date().toISOString().slice(0, 10),
          soldeTheorique: 1000
        }
      ] as any),
      of([
        {
          id: 209,
          caisseId: 1,
          statut: 'CLOTUREE',
          dateComptable: new Date().toISOString().slice(0, 10),
          soldeTheorique: 1000
        }
      ] as any)
    );
    sessionServiceSpy.cloturerFinale.and.returnValue(of({ id: 209, statut: 'CLOTUREE' } as any));

    fixture = TestBed.createComponent(CaisseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const button = (fixture.nativeElement as HTMLElement).querySelector('button') as HTMLButtonElement;
    button.click();
    fixture.detectChanges();

    expect(sessionServiceSpy.cloturerFinale).toHaveBeenCalledWith(209, { observation: 'RAS' });
    expect(sessionServiceSpy.getAll.calls.count()).toBeGreaterThanOrEqual(2);
    expect(component.successMessage).toContain('Clôture finale effectuée');
    expect(buttonLabels(fixture.nativeElement as HTMLElement)).not.toContain('Clôturer définitivement');
  });

  function buttonLabels(root: HTMLElement): string[] {
    return Array.from(root.querySelectorAll('button'))
      .map((button) => (button.textContent || '').trim());
  }
});
