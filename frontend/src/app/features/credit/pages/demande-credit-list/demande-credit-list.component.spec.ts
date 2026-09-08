import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { DemandeCreditListComponent } from './demande-credit-list.component';
import { DemandeCreditService } from '../../services/demande-credit.service';
import { CreditService } from '../../services/credit.service';
import { AuthService } from '../../../../core/services/auth.service';

describe('DemandeCreditListComponent', () => {
  let fixture: ComponentFixture<DemandeCreditListComponent>;
  let component: DemandeCreditListComponent;

  const demandeCreditServiceMock = {
    getAll: jasmine.createSpy('getAll').and.returnValue(of({ content: [], totalPages: 1, totalElements: 0 })),
    preAnalyser: jasmine.createSpy('preAnalyser').and.returnValue(of({})),
    preAnalyserDecision: jasmine.createSpy('preAnalyserDecision').and.returnValue(of({})),
    enregistrerObservationRisque: jasmine.createSpy('enregistrerObservationRisque').and.returnValue(of({})),
    validerAnalyseRisque: jasmine.createSpy('validerAnalyseRisque').and.returnValue(of({})),
    controlerRisque: jasmine.createSpy('controlerRisque').and.returnValue(of({})),
    controlerGarantie: jasmine.createSpy('controlerGarantie').and.returnValue(of({})),
    rejeter: jasmine.createSpy('rejeter').and.returnValue(of({}))
  };

  const creditServiceMock = {
    approuverDemande: jasmine.createSpy('approuverDemande').and.returnValue(of({}))
  };

  const authServiceMock = {
    hasAnyRole: jasmine.createSpy('hasAnyRole').and.callFake((roles: string[]) => roles.includes('GESTIONNAIRE')),
    getCurrentUser: jasmine.createSpy('getCurrentUser').and.returnValue({ id: 10 })
  };

  beforeEach(async () => {
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('GESTIONNAIRE'));

    await TestBed.configureTestingModule({
      imports: [DemandeCreditListComponent],
      providers: [
        provideRouter([]),
        { provide: DemandeCreditService, useValue: demandeCreditServiceMock },
        { provide: CreditService, useValue: creditServiceMock },
        { provide: AuthService, useValue: authServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DemandeCreditListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('ne doit jamais afficher paiement pour SOUMISE', () => {
    const demande: any = {
      statut: 'SOUMISE',
      fraisDemande: 1000,
      fraisDemandePayes: 0,
      depotGarantieRequis: 10000,
      depotGarantiePaye: 0
    };

    expect(component.peutPayerInitial(demande)).toBeFalse();
  });

  it('autorise Pre-analyser pour GESTIONNAIRE sur SOUMISE', () => {
    const demande: any = { statut: 'SOUMISE' };
    expect(component.peutPreAnalyser(demande)).toBeTrue();
  });

  it('cache Pre-analyser pour un rôle non autorisé sur SOUMISE', () => {
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CONTROLEUR'));

    expect(component.peutPreAnalyser({ statut: 'SOUMISE' } as any)).toBeFalse();
  });

  it('n autorise approuver que sur VALIDATION_CHEF', () => {
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CHEF_BUREAU'));

    expect(component.peutApprouver({ statut: 'SOUMISE' } as any)).toBeFalse();
    expect(component.peutApprouver({ statut: 'VALIDATION_CHEF' } as any)).toBeTrue();
  });

  it('reserve Analyser au CONTROLEUR et ADMIN', () => {
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CONTROLEUR'));
    expect(component.canSeeAnalyseActions).toBeTrue();
    expect(component.peutAnalyser({ statut: 'EN_ANALYSE', garantieBloquee: true } as any)).toBeTrue();
    expect(component.peutAnalyser({ statut: 'EN_ANALYSE', garantieBloquee: false } as any)).toBeFalse();

    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CHEF_BUREAU'));
    expect(component.canSeeAnalyseActions).toBeFalse();
    expect(component.peutAnalyser({ statut: 'EN_ANALYSE', garantieBloquee: true } as any)).toBeFalse();
  });

  it('CONTROLEUR ne voit pas Paiement sur EN_ANALYSE', () => {
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CONTROLEUR'));

    const demande: any = {
      statut: 'EN_ANALYSE',
      fraisDemande: 1000,
      fraisDemandePayes: 0,
      depotGarantieRequis: 10000,
      depotGarantiePaye: 0
    };

    expect(component.peutPayerInitial(demande)).toBeFalse();
  });

  it('CHEF_BUREAU ne voit pas Analyser ni Paiement sur EN_ANALYSE', () => {
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CHEF_BUREAU'));

    const demande: any = {
      statut: 'EN_ANALYSE',
      fraisDemande: 1000,
      fraisDemandePayes: 0,
      depotGarantieRequis: 10000,
      depotGarantiePaye: 0
    };

    expect(component.peutAnalyser(demande)).toBeFalse();
    expect(component.peutPayerInitial(demande)).toBeFalse();
  });

  it('CAISSIER voit Paiement pour frais incomplets et dépôt à solder', () => {
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CAISSIER'));

    const demandeEnAnalyse: any = {
      statut: 'EN_ANALYSE',
      fraisDemande: 1000,
      fraisDemandePayes: 0,
      depotGarantieRequis: 10000,
      depotGarantiePaye: 0
    };
    const demandeApprouvee: any = {
      statut: 'APPROUVEE',
      fraisDemande: 1000,
      fraisDemandePayes: 0,
      depotGarantieRequis: 10000,
      depotGarantiePaye: 0
    };

    expect(component.peutPayerInitial(demandeEnAnalyse)).toBeTrue();
    expect(component.peutPayerInitial(demandeApprouvee)).toBeTrue();
    expect(component.peutControlerGarantie(demandeEnAnalyse)).toBeFalse();
  });

  it('CONTROLEUR voit le bouton Contrôler garantie seulement après validation risque', () => {
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CONTROLEUR'));

    expect(component.peutControlerGarantie({ statut: 'ANALYSE_TERRAIN_VALIDEE' } as any)).toBeTrue();
    expect(component.peutControlerGarantie({ statut: 'EN_ANALYSE' } as any)).toBeFalse();
    expect(component.peutControlerGarantie({ statut: 'SOUMISE' } as any)).toBeFalse();
  });

  it('CONTROLEUR peut valider analyse uniquement en EN_ANALYSE', () => {
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CONTROLEUR'));

    expect(component.peutValiderAnalyseRisque({ statut: 'EN_ANALYSE', garantieBloquee: true } as any)).toBeTrue();
    expect(component.peutValiderAnalyseRisque({ statut: 'EN_ANALYSE', garantieBloquee: false } as any)).toBeFalse();
    expect(component.peutValiderAnalyseRisque({ statut: 'ANALYSE_TERRAIN_VALIDEE', garantieBloquee: true } as any)).toBeFalse();
  });

  it('CHEF_BUREAU voit seulement Voir garantie', () => {
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CHEF_BUREAU'));

    expect(component.peutControlerGarantie({ statut: 'ANALYSE_TERRAIN_VALIDEE' } as any)).toBeFalse();
    expect(component.peutVoirGarantie({ statut: 'ANALYSE_TERRAIN_VALIDEE' } as any)).toBeTrue();
  });

  it('une demande SOUMISE affiche Gestionnaire comme responsable et Pré-analyse comme étape suivante', () => {
    authServiceMock.getCurrentUser.and.returnValue({ id: 10, role: 'GESTIONNAIRE' });
    demandeCreditServiceMock.getAll.and.returnValue(of({
      content: [{
        id: 1,
        numeroDemande: 'DMD-001',
        membreNomComplet: 'Membre Test',
        siteNom: 'Site 3N',
        dateDemande: new Date().toISOString(),
        montantDemande: 10000,
        devise: 'CDF',
        dureeValeur: 6,
        dureeUnite: 'MOIS',
        tauxInteret: 2,
        depotGarantiePaye: 0,
        depotGarantieRequis: 2000,
        montantGarantieBloque: 0,
        statut: 'SOUMISE'
      }],
      totalPages: 1,
      totalElements: 1
    }));

    component.chargerDemandes();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Demande de crédit soumise');
    expect(text).toContain('Gestionnaire');
    expect(text).toContain('Pré-analyse');
    expect(text).toContain('Effectuer la pré-analyse du dossier');
  });

  it('pour utilisateur CONTROLEUR sur SOUMISE le message indique qu’il intervient après la pré-analyse', () => {
    authServiceMock.getCurrentUser.and.returnValue({ id: 11, role: 'CONTROLEUR' });
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CONTROLEUR'));
    demandeCreditServiceMock.getAll.and.returnValue(of({
      content: [{
        id: 2,
        numeroDemande: 'DMD-002',
        membreNomComplet: 'Membre Test 2',
        siteNom: 'Site 3N',
        dateDemande: new Date().toISOString(),
        montantDemande: 15000,
        devise: 'CDF',
        dureeValeur: 8,
        dureeUnite: 'MOIS',
        tauxInteret: 2,
        depotGarantiePaye: 0,
        depotGarantieRequis: 3000,
        montantGarantieBloque: 0,
        statut: 'SOUMISE'
      }],
      totalPages: 1,
      totalElements: 1
    }));

    component.chargerDemandes();
    fixture.detectChanges();

    const banners = fixture.debugElement.queryAll(By.css('app-workflow-guidance-banner'));
    const demandeBannerText = banners[banners.length - 1].nativeElement.textContent;
    expect(demandeBannerText).toContain('Le Contrôleur interviendra ensuite pour l’analyse de risque');
    expect(demandeBannerText).not.toContain('Vous pouvez agir');
  });

  it('une demande EN_ANALYSE affiche Contrôleur comme responsable et Analyse Contrôleur comme étape', () => {
    authServiceMock.getCurrentUser.and.returnValue({ id: 11, role: 'CONTROLEUR' });
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CONTROLEUR'));
    demandeCreditServiceMock.getAll.and.returnValue(of({
      content: [{
        id: 4,
        numeroDemande: 'DMD-004',
        membreNomComplet: 'Membre Analyse',
        siteNom: 'Site 3N',
        dateDemande: new Date().toISOString(),
        montantDemande: 20000,
        devise: 'CDF',
        dureeValeur: 10,
        dureeUnite: 'MOIS',
        tauxInteret: 2,
        depotGarantiePaye: 4000,
        depotGarantieRequis: 4000,
        montantGarantieBloque: 4000,
        garantieBloquee: true,
        statut: 'EN_ANALYSE'
      }],
      totalPages: 1,
      totalElements: 1
    }));

    component.chargerDemandes();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Analyse Contrôleur');
    expect(text).toContain('Contrôleur');
    expect(text).toContain('Analyser le dossier et valider l’analyse risque');
    expect(text).not.toContain('Rôle attendu : Gestionnaire');
  });

  it('cache le bouton Rejeter pour CONTROLEUR sur une demande SOUMISE', () => {
    authServiceMock.getCurrentUser.and.returnValue({ id: 11, role: 'CONTROLEUR' });
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CONTROLEUR'));
    demandeCreditServiceMock.getAll.and.returnValue(of({
      content: [{
        id: 3,
        numeroDemande: 'DMD-003',
        membreNomComplet: 'Membre Test 3',
        siteNom: 'Site 3N',
        dateDemande: new Date().toISOString(),
        montantDemande: 9000,
        devise: 'CDF',
        dureeValeur: 4,
        dureeUnite: 'MOIS',
        tauxInteret: 2,
        depotGarantiePaye: 0,
        depotGarantieRequis: 1800,
        montantGarantieBloque: 0,
        statut: 'SOUMISE'
      }],
      totalPages: 1,
      totalElements: 1
    }));

    component.chargerDemandes();
    fixture.detectChanges();

    const buttons = fixture.debugElement.queryAll(By.css('button'));
    const rejectButton = buttons.find(btn => (btn.nativeElement.textContent || '').includes('Rejeter'));
    expect(rejectButton).toBeUndefined();
  });
});
