import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';

import { GarantieCreditPageComponent } from './garantie-credit-page.component';
import { DemandeCreditService } from '../../services/demande-credit.service';
import { GarantieCreditService } from '../../services/garantie-credit.service';
import { AuthService } from '../../../../core/services/auth.service';

describe('GarantieCreditPageComponent', () => {
  let fixture: ComponentFixture<GarantieCreditPageComponent>;
  let component: GarantieCreditPageComponent;

  const demandeMock = {
    id: 1,
    numeroDemande: 'DCR-001',
    membreId: 9,
    membreNomComplet: 'Client Test',
    siteId: 3,
    siteNom: 'Site A',
    dateDemande: '2026-06-23',
    montantDemande: 50000,
    fraisDemande: 1000,
    fraisDemandePayes: 1000,
    depotGarantieRequis: 10000,
    depotGarantiePaye: 0,
    devise: 'CDF',
    dureeValeur: 3,
    dureeUnite: 'MOIS',
    periodiciteRemboursement: 'MENSUEL',
    tauxInteret: 5,
    objetCredit: 'Stock',
    revenusEstimes: 100000,
    chargesEstimees: 20000,
    statut: 'ANALYSE_TERRAIN_VALIDEE'
  } as any;

  const garantieBase = {
    id: 10,
    demandeCreditId: 1,
    membreId: 9,
    devise: 'CDF',
    montantCredit: 50000,
    montantGarantieRequis: 10000,
    montantGarantieBloque: 0,
    montantGarantieManquant: 10000,
    statutGarantieEpargne: 'INSUFFISANTE',
    soldeDisponible: 0,
    soldeBloque: 0,
    montantMaterielTotal: 0,
    statutGlobal: 'INSUFFISANTE',
    garantiesMaterielles: []
  } as any;

  const demandeCreditServiceMock = {
    getById: jasmine.createSpy('getById').and.returnValue(of(demandeMock)),
    controlerGarantie: jasmine.createSpy('controlerGarantie').and.returnValue(of({
      ...demandeMock,
      statut: 'VALIDATION_CHEF'
    }))
  };

  const garantieCreditServiceMock = {
    getGarantie: jasmine.createSpy('getGarantie').and.returnValue(of({ ...garantieBase })),
    verifierGarantie: jasmine.createSpy('verifierGarantie').and.returnValue(of({ ...garantieBase })),
    bloquerEpargne: jasmine.createSpy('bloquerEpargne').and.returnValue(of({
      ...garantieBase,
      soldeDisponible: 5000,
      soldeBloque: 10000,
      montantGarantieBloque: 10000,
      montantGarantieManquant: 0,
      statutGarantieEpargne: 'BLOQUEE'
    })),
    ajouterGarantieMaterielle: jasmine.createSpy('ajouterGarantieMaterielle').and.returnValue(of({ id: 70 })),
    accepterGarantieMaterielle: jasmine.createSpy('accepterGarantieMaterielle').and.returnValue(of({})),
    refuserGarantieMaterielle: jasmine.createSpy('refuserGarantieMaterielle').and.returnValue(of({})),
    validerGarantie: jasmine.createSpy('validerGarantie').and.returnValue(of({
      ...garantieBase,
      montantGarantieBloque: 10000,
      montantGarantieManquant: 0,
      statutGarantieEpargne: 'VALIDEE',
      statutGlobal: 'VALIDEE'
    })),
    rejeterGarantie: jasmine.createSpy('rejeterGarantie').and.returnValue(of({ ...garantieBase, statutGlobal: 'REFUSEE' })),
    getGarantiesMaterielles: jasmine.createSpy('getGarantiesMaterielles').and.returnValue(of([]))
  };

  const authServiceMock = {
    hasAnyRole: jasmine.createSpy('hasAnyRole').and.callFake((roles: string[]) => roles.includes('CONTROLEUR'))
  };

  beforeEach(async () => {
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CONTROLEUR'));
    demandeCreditServiceMock.controlerGarantie.and.returnValue(of({
      ...demandeMock,
      statut: 'VALIDATION_CHEF'
    }));
    garantieCreditServiceMock.getGarantie.and.returnValue(of({ ...garantieBase }));
    garantieCreditServiceMock.bloquerEpargne.and.returnValue(of({
      ...garantieBase,
      soldeDisponible: 5000,
      soldeBloque: 10000,
      montantGarantieBloque: 10000,
      montantGarantieManquant: 0,
      statutGarantieEpargne: 'BLOQUEE'
    }));

    await TestBed.configureTestingModule({
      imports: [GarantieCreditPageComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '1' } } }
        },
        { provide: DemandeCreditService, useValue: demandeCreditServiceMock },
        { provide: GarantieCreditService, useValue: garantieCreditServiceMock },
        { provide: AuthService, useValue: authServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GarantieCreditPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('affiche garantie requise 10 000 CDF pour demande 50 000 CDF', () => {
    expect(component.montantGarantieRequis()).toBe(10000);
  });

  it('affiche montant manquant si solde 0', () => {
    expect(component.montantManquant()).toBe(10000);
    expect(component.canBloquer()).toBeFalse();
  });

  it('bouton Bloquer visible si solde suffisant', () => {
    garantieCreditServiceMock.getGarantie.and.returnValue(of({
      ...garantieBase,
      statutGarantieEpargne: 'SUFFISANTE',
      soldeDisponible: 15000,
      montantGarantieManquant: 0
    }));

    fixture = TestBed.createComponent(GarantieCreditPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.canBloquer()).toBeTrue();
  });

  it('ajout garantie matérielle avec valeur/devise', () => {
    component.materielleForm.patchValue({
      typeBien: 'TELEVISION',
      description: 'TV UHD',
      valeurEstimee: 2000000,
      devise: 'CDF'
    });

    component.ajouterMaterielle();

    expect(garantieCreditServiceMock.ajouterGarantieMaterielle).toHaveBeenCalled();
    const request = garantieCreditServiceMock.ajouterGarantieMaterielle.calls.mostRecent().args[1];
    expect(request.valeurEstimee).toBe(2000000);
    expect(request.devise).toBe('CDF');
  });

  it('refus garantie matérielle exige commentaire', () => {
    spyOn(window, 'prompt').and.returnValue('   ');

    component.refuserMaterielle({ id: 900, statut: 'DECLAREE' } as any);

    expect(garantieCreditServiceMock.refuserGarantieMaterielle).not.toHaveBeenCalled();
    expect(component.errorMessage).toContain('commentaire est obligatoire');
  });

  it('valider garantie appelle le service avec la décision du contrôleur', () => {
    component.garantie.set({
      ...garantieBase,
      montantGarantieBloque: 10000,
      montantGarantieManquant: 0,
      statutGarantieEpargne: 'BLOQUEE'
    });
    component.decisionForm.patchValue({ commentaire: 'Validation finale' });
    component.validerGarantie();

    expect(garantieCreditServiceMock.validerGarantie).toHaveBeenCalledWith(1, { commentaire: 'Validation finale' });
  });

  it('valider garantie transmet la demande au Chef de Bureau', () => {
    component.garantie.set({
      ...garantieBase,
      montantGarantieBloque: 10000,
      montantGarantieManquant: 0,
      statutGarantieEpargne: 'BLOQUEE'
    });
    component.decisionForm.patchValue({ commentaire: 'Validation finale' });

    component.validerGarantie();

    expect(garantieCreditServiceMock.validerGarantie).toHaveBeenCalledWith(1, { commentaire: 'Validation finale' });
    expect(demandeCreditServiceMock.controlerGarantie).toHaveBeenCalledWith(1, 'Validation finale');
    expect(component.demande()?.statut).toBe('VALIDATION_CHEF');
    expect(component.successMessage).toContain('Chef de Bureau');
  });

  it('CHEF_BUREAU ne voit pas bouton bloquer', () => {
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CHEF_BUREAU'));

    fixture = TestBed.createComponent(GarantieCreditPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.isControleur()).toBeFalse();
    expect(component.canBloquer()).toBeFalse();
  });

  it('CAISSIER ne voit pas actions de contrôle', () => {
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('CAISSIER'));

    fixture = TestBed.createComponent(GarantieCreditPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.isControleur()).toBeFalse();
    expect(component.canBloquer()).toBeFalse();
  });

  it('GESTIONNAIRE ne peut pas vérifier ni bloquer la garantie', () => {
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('GESTIONNAIRE'));

    fixture = TestBed.createComponent(GarantieCreditPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.canVerifier()).toBeFalse();
    expect(component.canBloquer()).toBeFalse();
  });

  it('RCI reste en lecture seule sur la garantie', () => {
    authServiceMock.hasAnyRole.and.callFake((roles: string[]) => roles.includes('RCI'));

    fixture = TestBed.createComponent(GarantieCreditPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.canVerifier()).toBeFalse();
    expect(component.canBloquer()).toBeFalse();
    expect(component.isControleur()).toBeFalse();
  });

  it('bloquer déclenche message manquant via backend si insuffisant', () => {
    garantieCreditServiceMock.bloquerEpargne.and.returnValue(
      throwError(() => ({ error: { message: 'Solde disponible insuffisant. Dépôt complémentaire requis: 10000.00 CDF' } }))
    );

    component.bloquerEpargne();

    expect(component.errorMessage).toContain('Dépôt complémentaire requis');
  });
});
