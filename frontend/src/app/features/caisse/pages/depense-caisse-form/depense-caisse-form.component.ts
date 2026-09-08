import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { finalize } from 'rxjs';
import { CaisseService } from '../../services/caisse.service';
import { SessionCaisseService } from '../../services/session-caisse.service';
import { DepenseCaisseService } from '../../services/depense-caisse.service';
import { DepenseCaisseCategorie } from '../../models/depense-caisse-categorie';
import { DepenseCaisseBeneficiaireSalaire } from '../../models/depense-caisse-beneficiaire-salaire';
import { PaieEmployePreview } from '../../models/paie-employe-preview';
import { CaisseResponse } from '../../models/caisse-response';
import { SessionCaisseResponse } from '../../models/session-caisse-response';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';
import { TYPE_PAIEMENT_PERSONNEL_OPTIONS, TypePaiementPersonnel } from '../../models/type-paiement-personnel';
import { TransportSiteParametreResponse } from '../../../parametres/models/transport-site-parametre.model';
import { TransportSiteParametreService } from '../../../parametres/services/transport-site-parametre.service';

interface DepenseCategorieOption {
  value: DepenseCaisseCategorie;
  label: string;
}

@Component({
  selector: 'app-depense-caisse-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './depense-caisse-form.component.html',
  styleUrls: ['./depense-caisse-form.component.css']
})
export class DepenseCaisseFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly caisseService = inject(CaisseService);
  private readonly sessionService = inject(SessionCaisseService);
  private readonly depenseService = inject(DepenseCaisseService);
  private readonly authService = inject(AuthService);
  private readonly workflowMessageService = inject(WorkflowMessageService);
  private readonly transportSiteParametreService = inject(TransportSiteParametreService);

  loading = false;
  loadingContext = false;
  loadingBeneficiaires = false;
  loadingPaiePreview = false;
  error = '';
  caisses: CaisseResponse[] = [];
  beneficiairesSalaire: DepenseCaisseBeneficiaireSalaire[] = [];
  transportParametres: TransportSiteParametreResponse[] = [];
  paiePreview?: PaieEmployePreview;
  session?: SessionCaisseResponse;
  creationGuidance: WorkflowGuidance = {
    title: 'Suivi des dépenses de caisse',
    message: 'Vous pouvez encoder une dépense, mais elle devra être validée avant paiement. La création ne génère aucune sortie caisse; le mouvement réel sera créé uniquement au paiement validé.',
    currentStep: 'Demande dépense',
    nextStep: 'Validation / autorisation',
    expectedRole: 'Chef de Bureau',
    expectedAction: 'Enregistrer puis soumettre la dépense pour validation',
    severity: 'info',
    canCurrentUserAct: true
  };

  readonly categorieOptions: DepenseCategorieOption[] = [
    { value: 'FOURNITURE_BUREAU', label: 'Fourniture bureau' },
    { value: 'TRANSPORT', label: 'Transport' },
    { value: 'COMMUNICATION', label: 'Communication' },
    { value: 'MATERIEL', label: 'Matériel' },
    { value: 'LOYER', label: 'Loyer' },
    { value: 'ENTRETIEN', label: 'Entretien' },
    { value: 'SALAIRE', label: 'Salaire' },
    { value: 'AUTRE', label: 'Autre dépense' },
  ];

  readonly typePaiementOptions = TYPE_PAIEMENT_PERSONNEL_OPTIONS;

  form = this.fb.group({
    caisseId: this.fb.control<number | null>(null, Validators.required),
    categorie: this.fb.control<DepenseCaisseCategorie | null>('FOURNITURE_BUREAU', Validators.required),
    montant: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    devise: this.fb.control<string>('CDF', Validators.required),
    motif: this.fb.control<string>('', Validators.required),
    beneficiaire: this.fb.control<string>(''),
    beneficiaireId: this.fb.control<number | null>(null),
    employeId: this.fb.control<number | null>(null),
    periodePaie: this.fb.control<string>(this.currentPayrollPeriod()),
    typePaiementPersonnel: this.fb.control<TypePaiementPersonnel>('SALAIRE_COMPLET'),
    primeMotivationManuelle: this.fb.control<number>(0, [Validators.min(0)]),
    motifPrimeMotivationManuelle: this.fb.control<string>(''),
    motifEcartRemuneration: this.fb.control<string>(''),
    motifRetenue: this.fb.control<string>(''),
    motifPaiementPartiel: this.fb.control<string>(''),
    commentairePaie: this.fb.control<string>(''),
    retenueDefinitive: this.fb.control<boolean>(false),
    justificatifUrl: this.fb.control<string>('')
  });

  ngOnInit(): void {
    const currentRole = this.authService.getCurrentUser()?.role;
    this.creationGuidance = this.workflowMessageService.getGuidance({
      module: 'DEPENSE_CAISSE',
      status: 'BROUILLON',
      currentRole,
      expectedRole: currentRole
    });
    this.creationGuidance = {
      ...this.creationGuidance,
      message: 'Vous pouvez encoder une dépense, mais elle devra être validée avant paiement. La création ne génère aucune sortie caisse; le mouvement réel sera créé uniquement au paiement validé.'
    };

    const sessionId = Number(this.route.snapshot.queryParamMap.get('sessionId'));
    this.form.get('caisseId')?.valueChanges.subscribe((caisseId) => {
      if (this.session) {
        return;
      }
      const caisse = this.caisses.find((item) => item.id === caisseId);
      if (caisse?.devise) {
        this.form.patchValue({ devise: caisse.devise }, { emitEvent: false });
      }
      this.refreshBeneficiairesSalaire();
    });
    this.form.get('categorie')?.valueChanges.subscribe(() => {
      this.paiePreview = undefined;
      this.form.patchValue({ beneficiaire: '', beneficiaireId: null, employeId: null, primeMotivationManuelle: 0, motifPrimeMotivationManuelle: '', motifEcartRemuneration: '' }, { emitEvent: false });
      this.applySalaireValidators();
      this.refreshBeneficiairesSalaire();
      this.ensureTransportParametresLoaded();
    });
    this.form.get('beneficiaireId')?.valueChanges.subscribe((beneficiaireId) => this.onBeneficiaireSelected(beneficiaireId));
    this.form.get('periodePaie')?.valueChanges.subscribe(() => this.refreshPaiePreview());
    this.form.get('primeMotivationManuelle')?.valueChanges.subscribe(() => {
      this.refreshMontantFromPreview();
      this.applySalaireValidators();
    });
    this.form.get('montant')?.valueChanges.subscribe(() => this.applySalaireValidators());
    this.form.get('typePaiementPersonnel')?.valueChanges.subscribe(() => this.applySalaireValidators());
    if (sessionId) {
      this.loadSession(sessionId);
    } else {
      this.loadCaisses();
    }
  }

  loadCaisses(): void {
    this.loadingContext = true;
    this.caisseService.getAccessibles().subscribe({
      next: (data) => {
        this.caisses = data ?? [];
        this.loadingContext = false;
      },
      error: () => {
        this.error = 'Impossible de charger les caisses.';
        this.loadingContext = false;
      }
    });
  }

  loadSession(sessionId: number): void {
    this.loadingContext = true;
    this.sessionService.getById(sessionId).subscribe({
      next: (session) => {
        this.session = session;
        this.form.patchValue({ caisseId: session.caisseId, devise: session.devise || 'CDF' });
        this.loadingContext = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Impossible de charger la session de caisse.';
        this.loadingContext = false;
      }
    });
  }

  submit(action: 'brouillon' | 'soumettre'): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const beneficiaireSalaire = this.selectedBeneficiaireSalaire;
    const payload = {
      caisseId: value.caisseId as number,
      categorie: value.categorie as DepenseCaisseCategorie,
      montant: Number(value.montant),
      devise: value.devise || 'CDF',
      motif: value.motif || '',
      beneficiaire: this.isSalaireCategorie ? beneficiaireSalaire?.affichage : value.beneficiaire || undefined,
      beneficiaireId: this.isSalaireCategorie ? beneficiaireSalaire?.id : undefined,
      employeId: this.isSalaireCategorie ? (beneficiaireSalaire?.employeId || beneficiaireSalaire?.id) : undefined,
      periodePaie: this.isSalaireCategorie ? value.periodePaie || undefined : undefined,
      typePaiementPersonnel: this.isSalaireCategorie ? value.typePaiementPersonnel || 'SALAIRE' : undefined,
      montantRemunerationReference: this.isSalaireCategorie ? this.remunerationReference : undefined,
      montantEcartRemuneration: this.isSalaireCategorie ? this.remunerationEcart : undefined,
      motifEcartRemuneration: this.isSalaireCategorie ? value.motifEcartRemuneration || undefined : undefined,
      naturePaiementPaie: this.isSalaireCategorie ? value.typePaiementPersonnel || undefined : undefined,
      montantSalaireDu: this.isSalaireCategorie ? this.remunerationReference : undefined,
      motifRetenue: this.isSalaireCategorie ? value.motifRetenue || undefined : undefined,
      motifPaiementPartiel: this.isSalaireCategorie ? value.motifPaiementPartiel || undefined : undefined,
      commentairePaie: this.isSalaireCategorie ? value.commentairePaie || undefined : undefined,
      retenueDefinitive: this.isSalaireCategorie ? !!value.retenueDefinitive : undefined,
      primeMotivationManuelle: this.isSalaireCategorie ? Number(value.primeMotivationManuelle || 0) : undefined,
      motifPrimeMotivationManuelle: this.isSalaireCategorie ? value.motifPrimeMotivationManuelle || undefined : undefined,
      beneficiaireNom: this.isSalaireCategorie || this.isTransportCategorie ? beneficiaireSalaire?.nom : undefined,
      beneficiaireRole: this.isSalaireCategorie || this.isTransportCategorie ? beneficiaireSalaire?.role : undefined,
      beneficiaireAgence: this.isSalaireCategorie || this.isTransportCategorie ? beneficiaireSalaire?.agenceNom : undefined,
      justificatifUrl: value.justificatifUrl || undefined
    };

    this.loading = true;
    this.error = '';
    this.depenseService.creer(payload).pipe(finalize(() => this.loading = false)).subscribe({
      next: (depense) => {
        if (action === 'soumettre') {
          this.depenseService.soumettre(depense.id, { commentaire: 'Soumission depuis le formulaire' }).subscribe({
            next: () => this.router.navigate(['/caisses/depenses']),
            error: (err) => this.error = err?.error?.message || 'Dépense créée mais soumission impossible.'
          });
          return;
        }

        this.router.navigate(['/caisses/depenses']);
      },
      error: (err) => this.error = err?.error?.message || 'Impossible de créer la dépense.'
    });
  }

  cancel(): void {
    this.router.navigate(['/caisses/depenses']);
  }

  get selectedCaisse(): CaisseResponse | undefined {
    const caisseId = this.form.get('caisseId')?.value;
    return this.caisses.find((item) => item.id === caisseId);
  }

  get isSalaireCategorie(): boolean {
    return this.form.get('categorie')?.value === 'SALAIRE';
  }

  get isTransportCategorie(): boolean {
    return this.form.get('categorie')?.value === 'TRANSPORT';
  }

  get selectedBeneficiaireSalaire(): DepenseCaisseBeneficiaireSalaire | undefined {
    const beneficiaireId = this.form.get('beneficiaireId')?.value;
    return this.beneficiairesSalaire.find((beneficiaire) => beneficiaire.id === beneficiaireId);
  }

  get beneficiairesAgentTerrain(): DepenseCaisseBeneficiaireSalaire[] {
    return this.beneficiairesSalaire.filter((beneficiaire) => (beneficiaire.role || '').includes('AGENT_TERRAIN'));
  }

  get selectedTransportSiteId(): number | undefined {
    return this.session?.siteId || this.selectedCaisse?.siteId;
  }

  get transportParametreSite(): TransportSiteParametreResponse | undefined {
    const siteId = this.selectedTransportSiteId;
    return this.transportParametres.find((parametre) => parametre.actif && parametre.siteId === siteId);
  }

  get transportJournalierPropose(): number {
    return this.transportParametreSite?.montantTransportJournalierParAgent || 0;
  }

  get remunerationReference(): number {
    return this.paiePreview ? this.paieTotalPreview : this.selectedBeneficiaireSalaire?.totalRemuneration ?? 0;
  }

  get remunerationEcart(): number {
    return this.isSalaireCategorie ? this.montantValue - this.remunerationReference : 0;
  }

  get hasRemunerationEcart(): boolean {
    return this.isSalaireCategorie && !!this.selectedBeneficiaireSalaire && Math.abs(this.remunerationEcart) > 0.0001;
  }

  get selectedPaymentType(): TypePaiementPersonnel {
    return this.form.get('typePaiementPersonnel')?.value || 'SALAIRE_COMPLET';
  }

  get isUnderpaidSalary(): boolean {
    return this.hasRemunerationEcart && this.remunerationEcart < 0;
  }

  get isOverpaidSalary(): boolean {
    return this.hasRemunerationEcart && this.remunerationEcart > 0;
  }

  get expectedRemainingAfterPayment(): number {
    if (this.selectedPaymentType === 'RETENUE_SALAIRE' && this.form.get('retenueDefinitive')?.value) {
      return 0;
    }
    return Math.max(this.remunerationReference - this.montantValue, 0);
  }

  get isAgentTerrainPaie(): boolean {
    return this.paiePreview?.modeCalcul === 'AGENT_TERRAIN_AUTOMATIQUE';
  }

  get isPersonnelBureauPaie(): boolean {
    return this.paiePreview?.modeCalcul === 'PERSONNEL_BUREAU_MANUEL';
  }

  get primeMotivationValue(): number {
    return Number(this.form.get('primeMotivationManuelle')?.value || 0);
  }

  get paieTotalPreview(): number {
    if (!this.paiePreview) {
      return 0;
    }
    return this.isPersonnelBureauPaie
      ? (this.paiePreview.salaireBase || 0) + this.primeMotivationValue
      : this.paiePreview.totalAPayer;
  }

  get deviseDisplay(): string {
    return this.session?.devise || this.selectedCaisse?.devise || this.form.get('devise')?.value || 'CDF';
  }

  get caisseCodeDisplay(): string {
    return this.session?.caisseCode || this.selectedCaisse?.codeCaisse || 'Caisse non sélectionnée';
  }

  get caisseLibelleDisplay(): string {
    return this.selectedCaisse?.libelle || 'Dépense sur caisse';
  }

  get agenceDisplay(): string {
    return this.session?.antenneNom || this.selectedCaisse?.agenceNom || this.selectedCaisse?.antenneNom || 'Agence non renseignée';
  }

  get siteDisplay(): string {
    return this.session?.siteNom || this.selectedCaisse?.siteNom || 'Site non renseigné';
  }

  get soldeActuel(): number {
    return this.session?.soldeTheorique ?? 0;
  }

  get montantValue(): number {
    const raw = this.form.get('montant')?.value;
    return typeof raw === 'number' ? raw : Number(raw ?? 0);
  }

  get soldeApresDepense(): number {
    return this.soldeActuel - this.montantValue;
  }

  get hasInsufficientBalance(): boolean {
    return !!this.session && this.montantValue > 0 && this.montantValue > this.soldeActuel;
  }

  get canSubmitForm(): boolean {
    return !this.loading
      && this.form.valid
      && !this.hasInsufficientBalance
      && (!this.isSalaireCategorie || (!!this.form.get('beneficiaireId')?.value && !!this.form.get('periodePaie')?.value));
  }

  get submitHelpText(): string {
    if (!this.form.get('caisseId')?.value) {
      return 'Sélectionnez une caisse pour continuer.';
    }
    if (!this.form.get('montant')?.value || this.form.get('montant')?.invalid) {
      return 'Renseignez un montant valide pour enregistrer la dépense.';
    }
    if (!this.form.get('motif')?.value?.trim()) {
      return 'Renseignez le motif de la dépense pour enregistrer.';
    }
    if (this.isSalaireCategorie && !this.form.get('beneficiaireId')?.value) {
      return 'Sélectionnez l’employé concerné pour enregistrer.';
    }
    if (this.isSalaireCategorie && !this.form.get('periodePaie')?.value) {
      return 'Renseignez la période de paie.';
    }
    if (this.hasRemunerationEcart && !this.form.get('motifEcartRemuneration')?.value?.trim()) {
      return 'Qualifiez et justifiez le paiement si le montant diffère de la paie calculée.';
    }
    if (this.selectedPaymentType === 'SALAIRE_PARTIEL' && !this.form.get('motifPaiementPartiel')?.value?.trim()) {
      return 'Renseignez le motif du paiement partiel.';
    }
    if (this.selectedPaymentType === 'RETENUE_SALAIRE' && !this.form.get('motifRetenue')?.value?.trim()) {
      return 'Renseignez le motif de retenue.';
    }
    if (this.isSalaireCategorie && this.primeMotivationValue > 0 && !this.form.get('motifPrimeMotivationManuelle')?.value?.trim()) {
      return 'Renseignez le motif de la prime de motivation.';
    }
    if (this.hasInsufficientBalance) {
      return 'Le montant dépasse le solde théorique disponible sur cette session.';
    }
    return '';
  }

  get selectedCategorieLabel(): string {
    const current = this.form.get('categorie')?.value;
    return this.categorieOptions.find((categorie) => categorie.value === current)?.label || '—';
  }

  formatMoney(value: number | null | undefined): string {
    const safeValue = value ?? 0;
    return `${new Intl.NumberFormat('fr-FR').format(safeValue)} ${this.deviseDisplay}`;
  }

  private refreshBeneficiairesSalaire(): void {
    if (!this.isSalaireCategorie && !this.isTransportCategorie) {
      this.beneficiairesSalaire = [];
      this.paiePreview = undefined;
      return;
    }

    const caisseId = this.form.get('caisseId')?.value;
    if (!caisseId) {
      this.beneficiairesSalaire = [];
      this.paiePreview = undefined;
      return;
    }

    this.loadingBeneficiaires = true;
    this.depenseService.getBeneficiairesSalaire(caisseId)
      .pipe(finalize(() => this.loadingBeneficiaires = false))
      .subscribe({
        next: (beneficiaires) => this.beneficiairesSalaire = beneficiaires ?? [],
        error: (err) => {
          this.beneficiairesSalaire = [];
          this.error = err?.error?.message || 'Impossible de charger les bénéficiaires salaire.';
        }
      });
  }

  private onBeneficiaireSelected(beneficiaireId: number | null): void {
    if ((!this.isSalaireCategorie && !this.isTransportCategorie) || !beneficiaireId) {
      return;
    }
    const beneficiaire = this.beneficiairesSalaire.find((item) => item.id === beneficiaireId);
    if (!beneficiaire) {
      return;
    }
    this.form.patchValue({
      employeId: beneficiaire.employeId || beneficiaire.id,
      montant: this.isTransportCategorie ? this.transportJournalierPropose || this.form.get('montant')?.value : null,
      beneficiaire: this.isTransportCategorie ? beneficiaire.affichage : this.form.get('beneficiaire')?.value,
      primeMotivationManuelle: 0,
      motifPrimeMotivationManuelle: '',
      motifEcartRemuneration: ''
    }, { emitEvent: false });
    if (this.isTransportCategorie) {
      this.refreshTransportMontantJournalier();
      return;
    }
    this.refreshPaiePreview();
    this.applySalaireValidators();
  }

  private refreshTransportMontantJournalier(): void {
    if (!this.isTransportCategorie || !this.transportJournalierPropose) {
      return;
    }
    this.form.patchValue({ montant: this.transportJournalierPropose }, { emitEvent: false });
  }

  private ensureTransportParametresLoaded(): void {
    if (!this.isTransportCategorie) {
      return;
    }
    if (this.transportParametres.length > 0) {
      this.refreshTransportMontantJournalier();
      return;
    }
    this.loadTransportParametres();
  }

  private loadTransportParametres(): void {
    this.transportSiteParametreService.getAll(true).subscribe({
      next: (parametres) => {
        this.transportParametres = parametres ?? [];
        this.refreshTransportMontantJournalier();
      },
      error: () => {
        this.transportParametres = [];
      }
    });
  }

  private refreshPaiePreview(): void {
    if (!this.isSalaireCategorie) {
      this.paiePreview = undefined;
      return;
    }
    const employeId = this.form.get('employeId')?.value;
    const periodePaie = this.form.get('periodePaie')?.value;
    if (!employeId || !periodePaie || !/^\d{4}-\d{2}$/.test(periodePaie)) {
      this.paiePreview = undefined;
      return;
    }

    this.loadingPaiePreview = true;
    this.depenseService.getPaiePreview(employeId, periodePaie)
      .pipe(finalize(() => this.loadingPaiePreview = false))
      .subscribe({
        next: (preview) => {
          this.paiePreview = preview;
          if (preview.modeCalcul === 'AGENT_TERRAIN_AUTOMATIQUE') {
            this.form.patchValue({ primeMotivationManuelle: 0, motifPrimeMotivationManuelle: '' }, { emitEvent: false });
          }
          this.refreshMontantFromPreview();
          this.applySalaireValidators();
        },
        error: (err) => {
          this.paiePreview = undefined;
          this.error = err?.error?.message || 'Impossible de prévisualiser la paie.';
        }
      });
  }

  private refreshMontantFromPreview(): void {
    if (!this.paiePreview) {
      return;
    }
    this.form.patchValue({ montant: this.paieTotalPreview }, { emitEvent: false });
  }

  private applySalaireValidators(): void {
    const employeCtrl = this.form.get('beneficiaireId');
    const periodeCtrl = this.form.get('periodePaie');
    const primeMotivationCtrl = this.form.get('primeMotivationManuelle');
    const motifPrimeMotivationCtrl = this.form.get('motifPrimeMotivationManuelle');
    const motifEcartCtrl = this.form.get('motifEcartRemuneration');

    if (!this.isSalaireCategorie) {
      employeCtrl?.clearValidators();
      periodeCtrl?.clearValidators();
      primeMotivationCtrl?.clearValidators();
      motifPrimeMotivationCtrl?.clearValidators();
      motifEcartCtrl?.clearValidators();
    } else {
      employeCtrl?.setValidators([Validators.required]);
      periodeCtrl?.setValidators([Validators.required, Validators.pattern(/^\d{4}-\d{2}$/)]);
      primeMotivationCtrl?.setValidators([Validators.min(0)]);
      motifPrimeMotivationCtrl?.setValidators(this.isPersonnelBureauPaie && this.primeMotivationValue > 0 ? [Validators.required] : []);
      motifEcartCtrl?.setValidators(this.hasRemunerationEcart ? [Validators.required] : []);
      this.form.get('motifPaiementPartiel')?.setValidators(this.selectedPaymentType === 'SALAIRE_PARTIEL' ? [Validators.required] : []);
      this.form.get('motifRetenue')?.setValidators(this.selectedPaymentType === 'RETENUE_SALAIRE' ? [Validators.required] : []);
    }

    employeCtrl?.updateValueAndValidity({ emitEvent: false });
    periodeCtrl?.updateValueAndValidity({ emitEvent: false });
    primeMotivationCtrl?.updateValueAndValidity({ emitEvent: false });
    motifPrimeMotivationCtrl?.updateValueAndValidity({ emitEvent: false });
    motifEcartCtrl?.updateValueAndValidity({ emitEvent: false });
    this.form.get('motifPaiementPartiel')?.updateValueAndValidity({ emitEvent: false });
    this.form.get('motifRetenue')?.updateValueAndValidity({ emitEvent: false });
  }

  private currentPayrollPeriod(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  }
}