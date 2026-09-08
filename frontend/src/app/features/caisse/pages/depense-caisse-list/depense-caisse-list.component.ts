import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';
import { DepenseCaisseBeneficiaireSalaire } from '../../models/depense-caisse-beneficiaire-salaire';
import { DepenseCaisseResponse } from '../../models/depense-caisse-response';
import { PaieEmployePreview } from '../../models/paie-employe-preview';
import { TYPE_PAIEMENT_PERSONNEL_OPTIONS, TypePaiementPersonnel } from '../../models/type-paiement-personnel';
import { DepenseCaisseService } from '../../services/depense-caisse.service';

@Component({
  selector: 'app-depense-caisse-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './depense-caisse-list.component.html'
})
export class DepenseCaisseListComponent implements OnInit {
  private readonly depenseService = inject(DepenseCaisseService);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly workflowMessageService = inject(WorkflowMessageService);

  readonly depenses = signal<DepenseCaisseResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  rattachementTarget: DepenseCaisseResponse | null = null;
  rattachementSaving = false;
  rattachementError = '';
  rattachementBeneficiaires: DepenseCaisseBeneficiaireSalaire[] = [];
  rattachementPreview?: PaieEmployePreview;
  rattachementForm = {
    employeId: null as number | null,
    periodePaie: this.currentPayrollPeriod(),
    typePaiementPersonnel: 'SALAIRE_PARTIEL' as TypePaiementPersonnel,
    motif: '',
    motifRetenue: '',
    motifPaiementPartiel: '',
    retenueDefinitive: false,
    commentaireCorrection: ''
  };
  readonly typePaiementOptions = TYPE_PAIEMENT_PERSONNEL_OPTIONS;
  readonly globalGuidance = computed<WorkflowGuidance>(() => {
    const hasValidatedUnpaid = this.depenses().some((depense) => this.isValidatedUnpaid(depense));
    return {
      title: 'Suivi des dépenses de caisse',
      message: 'Vous pouvez encoder une dépense, mais elle devra être validée avant paiement. Une dépense en attente de validation ne doit pas être payée avant autorisation. Une dépense payée devient historisée et consultable pour le contrôle, les rapports et l\'audit.',
      currentStep: 'Suivi des dépenses',
      nextStep: 'Validation puis paiement',
      expectedRole: hasValidatedUnpaid ? 'Caissier' : 'Chef de Bureau',
      expectedAction: hasValidatedUnpaid ? 'Payer les dépenses validées' : 'Suivre le référent de l’étape selon le statut de chaque dépense',
      severity: 'info',
      canCurrentUserAct: true
    };
  });

  readonly filtres = signal({ statut: '', caisseId: '', siteId: '', sessionCaisseId: '', dateDebut: '', dateFin: '' });
  readonly statusGuidance = computed(() => {
    const status = this.filtres().statut;
    if (!status) {
      return null;
    }

    return this.workflowMessageService.getGuidance({
      module: 'DEPENSE_CAISSE',
      status,
      currentRole: this.currentRole,
      expectedRole: this.resolveExpectedRoleForStatus(status)
    });
  });

  readonly filteredDepenses = computed(() => {
    const depenses = this.depenses();
    const f = this.filtres();
    return depenses.filter((depense) => {
      const matchesStatut = !f.statut || depense.statut === f.statut;
      const matchesCaisse = !f.caisseId || depense.caisseId === Number(f.caisseId);
      const matchesSite = !f.siteId || depense.siteId === Number(f.siteId);
      const matchesSession = !f.sessionCaisseId || depense.sessionCaisseId === Number(f.sessionCaisseId);
      const date = new Date(depense.dateDemande);
      const matchesDebut = !f.dateDebut || date >= new Date(`${f.dateDebut}T00:00:00`);
      const matchesFin = !f.dateFin || date <= new Date(`${f.dateFin}T23:59:59.999`);
      return matchesStatut && matchesCaisse && matchesSite && matchesSession && matchesDebut && matchesFin;
    });
  });

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    const statut = params.get('statut') || '';
    const caisseId = params.get('caisseId') || '';
    const siteId = params.get('siteId') || '';
    const sessionCaisseId = params.get('sessionCaisseId') || '';
    const dateDebut = params.get('dateDebut') || '';
    const dateFin = params.get('dateFin') || '';

    this.filtres.set({ statut, caisseId, siteId, sessionCaisseId, dateDebut, dateFin });

    const hasQueryFilters = !!(statut || caisseId || siteId || sessionCaisseId || dateDebut || dateFin);
    if (hasQueryFilters) {
      this.applyFilters();
      return;
    }

    this.load();
  }

  get canCreate(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'CAISSIER', 'CHEF_BUREAU']);
  }

  get currentRole(): string | undefined {
    return this.authService.getCurrentUser()?.role;
  }

  get canValidate(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'CHEF_BUREAU', 'CONTROLEUR']);
  }

  get canPay(): boolean {
    return this.authService.hasRole('CAISSIER');
  }

  canValidateDepense(depense: DepenseCaisseResponse): boolean {
    return this.canValidate && depense.statut === 'EN_ATTENTE_VALIDATION';
  }

  canPayDepense(depense: DepenseCaisseResponse): boolean {
    return this.canPay && this.isValidatedUnpaid(depense);
  }

  canOpenDetail(depense: DepenseCaisseResponse): boolean {
    return this.canValidateDepense(depense) || this.canPayDepense(depense);
  }

  canAttachPayroll(depense: DepenseCaisseResponse): boolean {
    return depense.categorie === 'SALAIRE' && depense.statut === 'PAYEE' && (!depense.employeId || !depense.periodePaie || !depense.typePaiementPersonnel);
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.depenseService.getAll().subscribe({
      next: (data) => {
        this.depenses.set(data ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Impossible de charger les dépenses caisse.');
        this.loading.set(false);
      }
    });
  }

  setFiltre(key: 'statut' | 'caisseId' | 'siteId' | 'sessionCaisseId' | 'dateDebut' | 'dateFin', value: string): void {
    this.filtres.update((current) => ({ ...current, [key]: value }));
  }

  applyFilters(): void {
    const f = this.filtres();
    this.loading.set(true);
    this.depenseService.getAll({
      statut: f.statut || undefined,
      caisseId: f.caisseId ? Number(f.caisseId) : undefined,
      siteId: f.siteId ? Number(f.siteId) : undefined,
      sessionCaisseId: f.sessionCaisseId ? Number(f.sessionCaisseId) : undefined,
      dateDebut: f.dateDebut || undefined,
      dateFin: f.dateFin || undefined
    }).subscribe({
      next: (data) => {
        this.depenses.set(data ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Impossible de filtrer les dépenses caisse.');
        this.loading.set(false);
      }
    });
  }

  formatCdf(value: number | null | undefined): string {
    if (value == null) return '0 CDF';
    return new Intl.NumberFormat('fr-CD').format(value) + ' CDF';
  }

  trackById(_: number, depense: DepenseCaisseResponse): number {
    return depense.id;
  }

  openRattachement(depense: DepenseCaisseResponse): void {
    this.rattachementTarget = depense;
    this.rattachementError = '';
    this.rattachementPreview = undefined;
    this.rattachementForm = {
      employeId: depense.employeId || null,
      periodePaie: depense.periodePaie || this.currentPayrollPeriod(),
      typePaiementPersonnel: depense.typePaiementPersonnel || 'SALAIRE_PARTIEL',
      motif: depense.motifEcartRemuneration || '',
      motifRetenue: depense.motifRetenue || '',
      motifPaiementPartiel: depense.motifPaiementPartiel || '',
      retenueDefinitive: false,
      commentaireCorrection: depense.commentairePaie || ''
    };
    this.depenseService.getBeneficiairesSalaire(depense.caisseId).subscribe({
      next: (beneficiaires) => {
        this.rattachementBeneficiaires = beneficiaires ?? [];
        this.refreshRattachementPreview();
      },
      error: (err) => this.rattachementError = err?.error?.message || 'Impossible de charger les employés.'
    });
  }

  closeRattachement(): void {
    this.rattachementTarget = null;
    this.rattachementPreview = undefined;
    this.rattachementError = '';
  }

  refreshRattachementPreview(): void {
    if (!this.rattachementForm.employeId || !/^\d{4}-\d{2}$/.test(this.rattachementForm.periodePaie)) {
      this.rattachementPreview = undefined;
      return;
    }
    this.depenseService.getPaiePreview(this.rattachementForm.employeId, this.rattachementForm.periodePaie).subscribe({
      next: (preview) => this.rattachementPreview = preview,
      error: (err) => this.rattachementError = err?.error?.message || 'Impossible de prévisualiser la paie.'
    });
  }

  submitRattachement(): void {
    if (!this.rattachementTarget || !this.rattachementForm.employeId || !this.rattachementForm.periodePaie || !this.rattachementForm.commentaireCorrection.trim()) {
      this.rattachementError = 'Employé, période et commentaire de correction sont obligatoires.';
      return;
    }
    if (this.rattachementForm.typePaiementPersonnel === 'RETENUE_SALAIRE' && !this.rattachementForm.motifRetenue.trim()) {
      this.rattachementError = 'Le motif de retenue est obligatoire.';
      return;
    }
    if (this.rattachementForm.typePaiementPersonnel === 'SALAIRE_PARTIEL' && !this.rattachementForm.motifPaiementPartiel.trim()) {
      this.rattachementError = 'Le motif de paiement partiel est obligatoire.';
      return;
    }
    if (this.rattachementForm.typePaiementPersonnel === 'AVANCE_SALAIRE' && !this.rattachementForm.motif.trim()) {
      this.rattachementError = 'Le motif de l’avance est obligatoire.';
      return;
    }
    this.rattachementSaving = true;
    this.rattachementError = '';
    this.depenseService.rattacherPaie(this.rattachementTarget.id, {
      employeId: this.rattachementForm.employeId,
      periodePaie: this.rattachementForm.periodePaie,
      typePaiementPersonnel: this.rattachementForm.typePaiementPersonnel,
      motif: this.rattachementForm.motif || undefined,
      motifRetenue: this.rattachementForm.motifRetenue || undefined,
      motifPaiementPartiel: this.rattachementForm.motifPaiementPartiel || undefined,
      retenueDefinitive: this.rattachementForm.retenueDefinitive,
      commentaireCorrection: this.rattachementForm.commentaireCorrection
    }).subscribe({
      next: () => {
        this.rattachementSaving = false;
        this.closeRattachement();
        this.load();
      },
      error: (err) => {
        this.rattachementSaving = false;
        this.rattachementError = err?.error?.message || 'Rattachement paie impossible.';
      }
    });
  }

  rattachementDifference(): number {
    return (this.rattachementTarget?.montant || 0) - (this.rattachementPreview?.totalAPayer || 0);
  }

  rattachementReste(): number {
    if (this.rattachementForm.typePaiementPersonnel === 'RETENUE_SALAIRE' && this.rattachementForm.retenueDefinitive) {
      return 0;
    }
    return Math.max((this.rattachementPreview?.totalAPayer || 0) - (this.rattachementTarget?.montant || 0), 0);
  }

  private resolveExpectedRoleForStatus(status: string): string | undefined {
    if (status === 'EN_ATTENTE_VALIDATION') {
      return this.canValidate ? this.currentRole : 'CHEF_BUREAU';
    }

    if (status === 'VALIDEE' || status === 'APPROUVEE') {
      return 'CAISSIER';
    }

    return undefined;
  }

  private isValidatedUnpaid(depense: DepenseCaisseResponse): boolean {
    return depense.statut === 'VALIDEE'
      && !depense.operationCaisseId
      && !depense.payeParId
      && !depense.datePaiement;
  }

  private currentPayrollPeriod(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  }
}