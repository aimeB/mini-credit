import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';

import { AnalyseRisqueFormComponent } from './analyse-risque-form.component';
import { DemandeCreditService } from '../../services/demande-credit.service';
import { ParametresMetierService } from '../../../../core/services/parametres-metier.service';
import { AuthService } from '../../../../core/services/auth.service';

describe('AnalyseRisqueFormComponent', () => {
  let fixture: ComponentFixture<AnalyseRisqueFormComponent>;
  let component: AnalyseRisqueFormComponent;

  const remplirFormulaireValide = () => {
    component.form.patchValue({
      dateVisite: '2026-06-22',
      lieuVisite: 'Marche central',
      activiteVerifiee: true,
      descriptionActivite: 'Activite verifiee et stable',
      revenuNetEstime: 50000,
      chargesMensuelles: 10000,
      capaciteRemboursement: 15000,
      recommandation: 'DEFAVORABLE',
      commentaire: 'Commentaire obligatoire present'
    });
    fixture.detectChanges();
  };

  const demandeServiceMock = {
    getById: jasmine.createSpy('getById').and.returnValue(of({
      id: 1,
      numeroDemande: 'DCR-001',
      statut: 'EN_ANALYSE',
      membreNomComplet: 'Kingani jean',
      siteNom: 'Site 3N',
      montantDemande: 50000,
      devise: 'CDF',
      dureeValeur: 6,
      dureeUnite: 'MOIS',
      tauxInteret: 2,
      fraisDemande: 0,
      fraisDemandePayes: 0,
      depotGarantieRequis: 10000,
      depotGarantiePaye: 10000
    })),
    ajouterAnalyse: jasmine.createSpy('ajouterAnalyse').and.returnValue(of({ id: 1 }))
  };

  const parametresServiceMock = {
    getDecimal: jasmine.createSpy('getDecimal').and.returnValue(0),
    getEntier: jasmine.createSpy('getEntier').and.returnValue(0)
  };

  const authServiceMock = {
    getCurrentUser: jasmine.createSpy('getCurrentUser').and.returnValue({ role: 'CONTROLEUR', permissions: [] })
  };

  beforeEach(async () => {
    demandeServiceMock.ajouterAnalyse.calls.reset();

    demandeServiceMock.getById.and.returnValue(of({
      id: 1,
      numeroDemande: 'DCR-001',
      statut: 'EN_ANALYSE',
      membreNomComplet: 'Kingani jean',
      siteNom: 'Site 3N',
      montantDemande: 50000,
      devise: 'CDF',
      dureeValeur: 6,
      dureeUnite: 'MOIS',
      tauxInteret: 2,
      fraisDemande: 0,
      fraisDemandePayes: 0,
      depotGarantieRequis: 10000,
      depotGarantiePaye: 10000
    }));

    await TestBed.configureTestingModule({
      imports: [AnalyseRisqueFormComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }) } }
        },
        { provide: DemandeCreditService, useValue: demandeServiceMock },
        { provide: ParametresMetierService, useValue: parametresServiceMock },
        { provide: AuthService, useValue: authServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AnalyseRisqueFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('autorise analyse uniquement au statut EN_ANALYSE', () => {
    expect(component.autoriseAnalyse).toBeTrue();
  });

  it('affiche un numéro analyse généré automatiquement et aucun champ Analyse ID éditable', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('N° analyse généré automatiquement');
    expect(fixture.debugElement.query(By.css('#analyseId'))).toBeNull();
  });

  it('affiche les montants avec devise', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('50');
    expect(text).toContain('CDF');
  });

  it('affiche Non calculé tant que le score n’est pas calculable', () => {
    component.form.patchValue({ dateVisite: '', lieuVisite: '', capaciteRemboursement: null });
    fixture.detectChanges();

    expect(component.scoreRisqueAffiche).toBe('Non calculé');
  });

  it('refuse submit si form invalide', () => {
    expect(component.form.invalid).toBeTrue();
    component.submit();
    expect(demandeServiceMock.ajouterAnalyse).not.toHaveBeenCalled();
  });

  it('empêche incohérence score 0 avec niveau MOYEN', () => {
    component.form.patchValue({
      dateVisite: '2026-06-22',
      lieuVisite: 'Marché central',
      capaciteRemboursement: 1000,
      scoreRisqueOverride: true,
      scoreRisque: 0,
      risqueNiveau: 'MOYEN',
      recommandation: 'FAVORABLE'
    });
    fixture.detectChanges();

    expect(component.form.errors?.['incoherenceScoreNiveau']).toBeTrue();
  });

  it('exige commentaire pour recommandation favorable avec réserve', () => {
    component.form.patchValue({
      dateVisite: '2026-06-22',
      lieuVisite: 'Marché central',
      capaciteRemboursement: 1000,
      risqueNiveau: 'MOYEN',
      recommandation: 'FAVORABLE_AVEC_RESERVE',
      commentaire: ''
    });
    fixture.detectChanges();

    expect(component.form.errors?.['commentaireRequiredByRecommandation']).toBeTrue();
  });

  it('exige commentaire pour recommandation défavorable', () => {
    component.form.patchValue({
      dateVisite: '2026-06-22',
      lieuVisite: 'Marché central',
      capaciteRemboursement: 1000,
      risqueNiveau: 'ELEVE',
      recommandation: 'DEFAVORABLE',
      commentaire: ''
    });
    fixture.detectChanges();

    expect(component.form.errors?.['commentaireRequiredByRecommandation']).toBeTrue();
  });

  it('désactive le bouton enregistrer si formulaire incomplet', () => {
    fixture.detectChanges();
    const submitBtn: HTMLButtonElement = fixture.debugElement.query(By.css('button[type="submit"]')).nativeElement;
    expect(submitBtn.disabled).toBeTrue();
  });

  it('active le bouton enregistrer si score calculé et champs valides', () => {
    remplirFormulaireValide();

    expect(component.scoreRisqueAffiche).toBe('70.00 / 100');
    const submitBtn: HTMLButtonElement = fixture.debugElement.query(By.css('button[type="submit"]')).nativeElement;
    expect(component.form.valid).toBeTrue();
    expect(submitBtn.disabled).toBeFalse();
  });

  it('garde le bouton désactivé si score non calculé', () => {
    component.form.patchValue({
      dateVisite: '',
      lieuVisite: '',
      capaciteRemboursement: null
    });
    fixture.detectChanges();

    const submitBtn: HTMLButtonElement = fixture.debugElement.query(By.css('button[type="submit"]')).nativeElement;
    expect(component.scoreRisqueAffiche).toBe('Non calculé');
    expect(component.form.errors?.['scoreNonCalcule']).toBeTrue();
    expect(submitBtn.disabled).toBeTrue();
  });

  it('permet la soumission avec score calculé sans correction manuelle', () => {
    remplirFormulaireValide();

    expect(component.form.getRawValue().scoreRisqueOverride).toBeFalse();
    component.submit();

    expect(demandeServiceMock.ajouterAnalyse).toHaveBeenCalled();
  });

  it('getRawValue contient scoreRisque meme si champ desactive', () => {
    remplirFormulaireValide();

    expect(component.form.controls.scoreRisque.disabled).toBeTrue();
    expect(component.form.getRawValue().scoreRisque).toBe(70);
  });

  it('envoie la recommandation et laisse le backend recalculer score et niveau hors correction manuelle', () => {
    remplirFormulaireValide();

    component.submit();

    const [, payload] = demandeServiceMock.ajouterAnalyse.calls.mostRecent().args;
    expect(payload.scoreRisque).toBeUndefined();
    expect(payload.risqueNiveau).toBeUndefined();
    expect(payload.recommandation).toBe('DEFAVORABLE');
    expect(payload.commentaire).toContain('Commentaire obligatoire');
  });

  it('montantDifferentAfficheScoreDifferent', () => {
    remplirFormulaireValide();
    const scoreFaible = component.form.getRawValue().scoreRisque;

    component.demande = { ...component.demande!, montantDemande: 1000000, depotGarantieRequis: 200000, depotGarantiePaye: 200000 };
    component.form.patchValue({ revenuNetEstime: 50000, chargesMensuelles: 10000, capaciteRemboursement: 15000 });
    fixture.detectChanges();

    expect(component.form.getRawValue().scoreRisque).toBeLessThan(scoreFaible!);
  });

  it('detailCriteresAfficheMontantDemande', () => {
    remplirFormulaireValide();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Montant demandé / revenu mensuel');
    expect(text).toContain('mois de revenus déclarés');
  });

  it('alerteMontantEleveAffichee', () => {
    remplirFormulaireValide();
    component.demande = { ...component.demande!, montantDemande: 1000000, depotGarantieRequis: 200000, depotGarantiePaye: 200000 };
    component.form.patchValue({ revenuNetEstime: 50000 });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('20.00 mois de revenus déclarés');
  });

  it('garantie20PourcentAffichee', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Garantie requise (20%)');
    expect(text).toContain('10');
  });

  it('aucunScoreMockeConstant', () => {
    remplirFormulaireValide();
    const scoreInitial = component.form.getRawValue().scoreRisque;

    component.form.patchValue({ capaciteRemboursement: 1000000 });
    fixture.detectChanges();

    expect(component.form.getRawValue().scoreRisque).not.toBe(scoreInitial);
  });

  it('affiche les sections métier attendues', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('A. Informations dossier');
    expect(text).toContain('B. Visite et activité');
    expect(text).toContain('C. Données financières');
    expect(text).toContain('D. Analyse risque');
    expect(text).toContain('E. Commentaire et décision');
  });

  it('refuse analyse hors statut EN_ANALYSE', () => {
    demandeServiceMock.getById.and.returnValue(of({ id: 2, statut: 'SOUMISE' }));
    component.chargerDemande();
    expect(component.autoriseAnalyse).toBeFalse();
  });
});
