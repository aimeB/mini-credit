import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { DemandeCreditResponse } from '../../models/demande-credit-response';
import { DemandeCreditService } from '../../services/demande-credit.service';
import { NIVEAU_RISQUE_OPTIONS } from '../../models/enums/niveau-risque.enum';
import {
  RECOMMANDATION_RISQUE_LABELS,
  RECOMMANDATION_RISQUE_OPTIONS,
  RecommandationRisque
} from '../../models/enums/recommandation-risque.enum';
import { AnalyseRisqueRequest } from '../../models/analyse-risque-request';
import { NiveauRisque } from '../../models/enums/niveau-risque.enum';
import { ParametresMetierService } from '../../../../core/services/parametres-metier.service';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';

@Component({
  selector: 'app-analyse-risque-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './analyse-risque-form.component.html'
})
export class AnalyseRisqueFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly demandeCreditService = inject(DemandeCreditService);
  private readonly parametresMetierService = inject(ParametresMetierService);
  private readonly authService = inject(AuthService);
  private readonly workflowMessageService = inject(WorkflowMessageService);

  readonly niveauRisqueOptions = NIVEAU_RISQUE_OPTIONS;
  readonly recommandationOptions = RECOMMANDATION_RISQUE_OPTIONS;
  readonly recommandationLabels = RECOMMANDATION_RISQUE_LABELS;
  readonly deviseOptions = ['CDF', 'USD'];
  readonly demandeId = Number(this.route.snapshot.paramMap.get('id'));

  demande: DemandeCreditResponse | null = null;
  autoriseAnalyse = false;
  scoreAutoCalcule: number | null = null;
  scoreDetail: Array<{ critere: string; points: number; detail: string }> = [];

  loading = false;
  errorMessage = '';
  successMessage = '';
  guidance: WorkflowGuidance | null = null;

  readonly form = this.fb.group({
    dateVisite: ['', [Validators.required]],
    lieuVisite: ['', [Validators.required]],
    activiteVerifiee: [false],
    descriptionActivite: [''],
    ancienneteActivite: [''],
    chiffreAffairesEstime: [null as number | null, [Validators.min(0)]],
    chiffreAffairesDevise: ['CDF', [Validators.required]],
    revenuNetEstime: [null as number | null, [Validators.min(0)]],
    revenuNetDevise: ['CDF', [Validators.required]],
    chargesMensuelles: [null as number | null, [Validators.min(0)]],
    chargesMensuellesDevise: ['CDF', [Validators.required]],
    capaciteRemboursement: [null as number | null, [Validators.required, Validators.min(0)]],
    capaciteRemboursementDevise: ['CDF', [Validators.required]],
    scoreRisqueOverride: [false],
    scoreRisque: [{ value: null as number | null, disabled: true }],
    risqueNiveau: [null as NiveauRisque | null],
    recommandation: ['FAVORABLE', [Validators.required]],
    commentaire: ['']
  }, { validators: [this.validationMetier()] });

  ngOnInit(): void {
    if (!this.demandeId || Number.isNaN(this.demandeId)) {
      this.errorMessage = 'Identifiant de demande invalide.';
      return;
    }

    this.chargerDemande();
    this.initialiserReactionsFormulaire();
  }

  chargerDemande(): void {
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.demandeCreditService.getById(this.demandeId).subscribe({
      next: (data) => {
        this.demande = data;
        const devise = (data.devise || 'CDF').toUpperCase();
        this.form.patchValue({
          chiffreAffairesDevise: devise,
          revenuNetDevise: devise,
          chargesMensuellesDevise: devise,
          capaciteRemboursementDevise: devise
        }, { emitEvent: false });
        this.calculerScoreAutomatique();

        this.autoriseAnalyse = data.statut === 'EN_ANALYSE';
        this.guidance = this.workflowMessageService.getGuidance({
          module: 'CREDIT',
          status: data.statut,
          currentRole: this.authService.getCurrentUser()?.role,
          permissions: this.authService.getCurrentUser()?.permissions
        });
        this.loading = false;

        if (!this.autoriseAnalyse) {
          this.errorMessage = 'Analyse autorisée uniquement au statut EN_ANALYSE.';
        }
      },
      error: (error) => {
        console.error('Erreur chargement demande :', error);
        this.loading = false;
        this.errorMessage =
          error?.error?.message || 'Impossible de charger la demande.';
      }
    });
  }

  submit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.demandeId || Number.isNaN(this.demandeId)) {
      this.errorMessage = 'Identifiant de demande invalide.';
      return;
    }

    if (!this.autoriseAnalyse) {
      this.errorMessage = 'Analyse autorisée uniquement au statut EN_ANALYSE.';
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage = 'Veuillez corriger les champs obligatoires et les incohérences avant de continuer.';
      return;
    }

    const raw = this.form.getRawValue();
    const scoreRisque = raw.scoreRisqueOverride && raw.scoreRisque != null ? Number(raw.scoreRisque) : undefined;
    const commentaire = raw.commentaire?.trim() || undefined;

    const request: AnalyseRisqueRequest = {
      dateVisite: raw.dateVisite || undefined,
      lieuVisite: raw.lieuVisite || undefined,
      activiteVerifiee: raw.activiteVerifiee ?? false,
      descriptionActivite: raw.descriptionActivite || undefined,
      ancienneteActivite: raw.ancienneteActivite || undefined,
      chiffreAffairesEstime:
        raw.chiffreAffairesEstime != null ? Number(raw.chiffreAffairesEstime) : undefined,
      chiffreAffairesDevise: raw.chiffreAffairesDevise || this.deviseParDefaut,
      revenuNetEstime:
        raw.revenuNetEstime != null ? Number(raw.revenuNetEstime) : undefined,
      revenuNetDevise: raw.revenuNetDevise || this.deviseParDefaut,
      chargesMensuelles:
        raw.chargesMensuelles != null ? Number(raw.chargesMensuelles) : undefined,
      chargesMensuellesDevise: raw.chargesMensuellesDevise || this.deviseParDefaut,
      capaciteRemboursement:
        raw.capaciteRemboursement != null ? Number(raw.capaciteRemboursement) : undefined,
      capaciteRemboursementDevise: raw.capaciteRemboursementDevise || this.deviseParDefaut,
      montantDemandeDevise: this.deviseParDefaut,
      fraisDemandeDevise: this.deviseParDefaut,
      depotRequisDevise: this.deviseParDefaut,
      depotPayeDevise: this.deviseParDefaut,
      risqueNiveau: raw.scoreRisqueOverride ? raw.risqueNiveau as AnalyseRisqueRequest['risqueNiveau'] : undefined,
      scoreRisque,
      scoreRisqueCorrigeManuellement: raw.scoreRisqueOverride ?? false,
      recommandation: raw.recommandation as AnalyseRisqueRequest['recommandation'],
      commentaire
    };

    this.loading = true;

    this.demandeCreditService.ajouterAnalyse(this.demandeId, request).subscribe({
      next: () => {
        this.loading = false;
        this.successMessage = 'Analyse de risque enregistrée avec succès.';

        setTimeout(() => {
          this.router.navigate(['/credits/demandes']);
        }, 700);
      },
      error: (error) => {
        console.error('Erreur ajout analyse de risque :', error);
        this.loading = false;
        this.errorMessage =
          error?.error?.message || 'Impossible d’enregistrer l’analyse de risque.';
      }
    });
  }

  get f() {
    return this.form.controls;
  }

  get deviseParDefaut(): string {
    return (this.demande?.devise || 'CDF').toUpperCase();
  }

  get scoreRisqueAffiche(): string {
    const value = this.form.getRawValue().scoreRisque;
    if (value == null) {
      return 'Non calculé';
    }
    return `${Number(value).toFixed(2)} / 100`;
  }

  get recommandationLabelActuelle(): string {
    const recommandation = this.form.getRawValue().recommandation as RecommandationRisque;
    return this.recommandationLabels[recommandation] ?? recommandation;
  }

  formatMontantAvecDevise(value: number | null | undefined, devise?: string | null): string {
    if (value == null) {
      return `0 ${devise || this.deviseParDefaut}`;
    }

    return `${new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2
    }).format(value)} ${devise || this.deviseParDefaut}`;
  }

  private initialiserReactionsFormulaire(): void {
    this.f.activiteVerifiee.valueChanges.subscribe(() => this.appliquerValidationDescriptionActivite());
    this.f.scoreRisqueOverride.valueChanges.subscribe(() => this.appliquerModeScore());

    this.form.valueChanges.subscribe(() => {
      this.calculerScoreAutomatique();
      if (!this.f.scoreRisqueOverride.value) {
        this.synchroniserNiveauSurScore();
      }
      this.form.updateValueAndValidity({ emitEvent: false });
    });

    this.appliquerValidationDescriptionActivite();
    this.appliquerModeScore();
  }

  private appliquerValidationDescriptionActivite(): void {
    const control = this.f.descriptionActivite;
    if (this.f.activiteVerifiee.value) {
      control.setValidators([Validators.required]);
    } else {
      control.clearValidators();
    }
    control.updateValueAndValidity({ emitEvent: false });
  }

  private appliquerModeScore(): void {
    const scoreControl = this.f.scoreRisque;
    if (this.f.scoreRisqueOverride.value) {
      scoreControl.enable({ emitEvent: false });
      scoreControl.setValidators([Validators.required, Validators.min(0), Validators.max(100)]);
      if (scoreControl.value == null) {
        scoreControl.setValue(this.scoreAutoCalcule, { emitEvent: false });
      }
    } else {
      scoreControl.clearValidators();
      scoreControl.disable({ emitEvent: false });
      scoreControl.setValue(this.scoreAutoCalcule, { emitEvent: false });
    }

    scoreControl.updateValueAndValidity({ emitEvent: false });
  }

  private calculerScoreAutomatique(): void {
    const raw = this.form.getRawValue();
    const details: Array<{ critere: string; points: number; detail: string }> = [];

    const poidsActiviteVerifiee = this.getParametreNombre('credit.risk.weights.activiteVerifiee', 10);
    details.push({
      critere: 'Activité vérifiée',
      points: raw.activiteVerifiee ? poidsActiviteVerifiee : 0,
      detail: raw.activiteVerifiee ? 'Oui' : 'Non'
    });

    const capacite = raw.capaciteRemboursement != null ? Number(raw.capaciteRemboursement) : null;
    const revenu = raw.revenuNetEstime != null ? Number(raw.revenuNetEstime) : null;
    const charges = raw.chargesMensuelles != null ? Number(raw.chargesMensuelles) : null;

    const poidsCapacite = this.getParametreNombre('credit.risk.weights.capaciteRemboursement', 30);
    const mensualiteEstimee = this.calculerMensualiteEstimee();
    if (capacite != null && capacite > 0 && mensualiteEstimee > 0) {
      const ratioMensualiteCapacite = mensualiteEstimee / capacite;
      const pointsCapacite = ratioMensualiteCapacite <= 0.3 ? poidsCapacite : ratioMensualiteCapacite <= 0.5 ? poidsCapacite * 0.6 : 0;
      details.push({
        critere: 'Mensualité estimée / capacité',
        points: Number(pointsCapacite.toFixed(2)),
        detail: `Mensualité ${this.formatMontantAvecDevise(mensualiteEstimee, raw.capaciteRemboursementDevise)} = ${(ratioMensualiteCapacite * 100).toFixed(2)}% capacité`
      });
    } else {
      details.push({
        critere: 'Mensualité estimée / capacité',
        points: 0,
        detail: 'Capacité ou durée non renseignée'
      });
    }

    const poidsMontantRevenu = this.getParametreNombre('credit.risk.weights.montantRevenu', 20);
    if (this.demande && revenu != null && revenu > 0) {
      const ratioMontantRevenu = this.demande.montantDemande / revenu;
      const pointsMontant = ratioMontantRevenu <= 1 ? poidsMontantRevenu : ratioMontantRevenu <= 3 ? poidsMontantRevenu * 0.6 : 0;
      details.push({
        critere: 'Montant demandé / revenu mensuel',
        points: Number(pointsMontant.toFixed(2)),
        detail: `Le montant demandé représente ${ratioMontantRevenu.toFixed(2)} mois de revenus déclarés`
      });
    } else {
      details.push({
        critere: 'Montant demandé / revenu mensuel',
        points: 0,
        detail: 'Revenu requis pour comparer le montant demandé'
      });
    }

    const poidsRatioCharges = this.getParametreNombre('credit.risk.weights.ratioCharges', 10);
    if (revenu != null && revenu > 0 && charges != null && charges >= 0) {
      const ratio = charges / revenu;
      const pointsRatio = ratio <= 0.5 ? poidsRatioCharges : ratio <= 0.7 ? poidsRatioCharges * 0.6 : poidsRatioCharges * 0.2;
      details.push({
        critere: 'Ratio charges / revenu net',
        points: Number(pointsRatio.toFixed(2)),
        detail: `Ratio = ${(ratio * 100).toFixed(2)}%`
      });
    } else {
      details.push({
        critere: 'Ratio charges / revenu net',
        points: 0,
        detail: 'Revenu et charges requis pour calcul'
      });
    }

    const poidsGarantie = this.getParametreNombre('credit.risk.weights.garantie', 20);
    if (this.demande && this.demande.depotGarantieRequis > 0) {
      const ratioGarantie = this.demande.depotGarantiePaye / this.demande.depotGarantieRequis;
      const pointsGarantie = ratioGarantie >= 1 ? poidsGarantie : ratioGarantie >= 0.8 ? poidsGarantie * 0.6 : 0;
      details.push({
        critere: 'Garantie disponible',
        points: Number(pointsGarantie.toFixed(2)),
        detail: `Couverture garantie = ${(ratioGarantie * 100).toFixed(2)}%`
      });
    }

    const poidsVisite = this.getParametreNombre('credit.risk.weights.visite', 10);
    const pointsVisite = raw.dateVisite && raw.lieuVisite ? poidsVisite : 0;
    details.push({
      critere: 'Visite terrain',
      points: pointsVisite,
      detail: pointsVisite > 0 ? 'Date et lieu renseignés' : 'Date/lieu manquants'
    });

    const peutCalculer = !!raw.dateVisite
      && !!raw.lieuVisite
      && raw.capaciteRemboursement != null;

    const total = details.reduce((acc, item) => acc + item.points, 0);
    const score = peutCalculer ? Number(Math.max(0, Math.min(100, total)).toFixed(2)) : null;

    this.scoreDetail = details;
    this.scoreAutoCalcule = score;

    if (!this.f.scoreRisqueOverride.value) {
      this.f.scoreRisque.setValue(this.scoreAutoCalcule, { emitEvent: false });
    }
  }

  private synchroniserNiveauSurScore(): void {
    const score = this.form.getRawValue().scoreRisque;
    if (score == null) {
      return;
    }

    const niveau = this.niveauDepuisScore(Number(score));
    this.f.risqueNiveau.setValue(niveau, { emitEvent: false });
  }

  private calculerMensualiteEstimee(): number {
    if (!this.demande || !this.demande.dureeValeur || this.demande.dureeValeur <= 0) {
      return 0;
    }

    const dureeMois = this.convertirDureeEnMois(this.demande.dureeValeur, String(this.demande.dureeUnite));
    if (dureeMois <= 0) {
      return 0;
    }

    const taux = Number(this.demande.tauxInteret || 0) / 100;
    return (Number(this.demande.montantDemande || 0) * (1 + taux)) / dureeMois;
  }

  private convertirDureeEnMois(dureeValeur: number, dureeUnite: string): number {
    if (dureeUnite === 'JOUR') {
      return dureeValeur / 30;
    }
    if (dureeUnite === 'SEMAINE') {
      return dureeValeur / 4.3333;
    }
    return dureeValeur;
  }

  private niveauDepuisScore(score: number): NiveauRisque {
    const seuilMoyen = this.getParametreNombre('credit.risk.threshold.mediumMin', 40);
    const seuilFaible = this.getParametreNombre('credit.risk.threshold.lowMin', 70);

    if (score >= seuilFaible) {
      return 'FAIBLE';
    }
    if (score >= seuilMoyen) {
      return 'MOYEN';
    }
    return 'ELEVE';
  }

  private validationMetier(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = ((control as { getRawValue?: () => unknown }).getRawValue?.() ?? control.value) as {
        activiteVerifiee?: boolean;
        descriptionActivite?: string;
        recommandation?: RecommandationRisque;
        commentaire?: string;
        risqueNiveau?: NiveauRisque | null;
        scoreRisque?: number | null;
        scoreRisqueOverride?: boolean;
      };

      const commentaire = value.commentaire?.trim();
      const errors: ValidationErrors = {};

      if (value.activiteVerifiee && !value.descriptionActivite?.trim()) {
        errors['descriptionActiviteRequired'] = true;
      }

      if (
        (value.recommandation === 'FAVORABLE_AVEC_RESERVE' || value.recommandation === 'DEFAVORABLE')
        && !commentaire
      ) {
        errors['commentaireRequiredByRecommandation'] = true;
      }

      if (value.risqueNiveau === 'ELEVE' && !commentaire) {
        errors['commentaireRequiredByHighRisk'] = true;
      }

      if (value.scoreRisqueOverride && !commentaire) {
        errors['commentaireRequiredByManualScore'] = true;
      }

      if (value.scoreRisque == null) {
        errors['scoreNonCalcule'] = true;
      }

      if (value.scoreRisque != null && value.risqueNiveau) {
        const niveauAttendu = this.niveauDepuisScore(Number(value.scoreRisque));
        if (niveauAttendu !== value.risqueNiveau) {
          errors['incoherenceScoreNiveau'] = true;
        }
      }

      return Object.keys(errors).length > 0 ? errors : null;
    };
  }

  private getParametreNombre(cle: string, valeurParDefaut: number): number {
    const decimal = this.parametresMetierService.getDecimal(cle);
    if (decimal > 0) {
      return decimal;
    }

    const entier = this.parametresMetierService.getEntier(cle);
    if (entier > 0) {
      return entier;
    }

    return valeurParDefaut;
  }
}