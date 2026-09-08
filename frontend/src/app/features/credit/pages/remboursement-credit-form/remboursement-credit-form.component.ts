import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { catchError, finalize, of, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ModePaiement } from '../../../../shared/enums/mode-paiement.enum';
import { CreditService } from '../../services/credit.service';
import { RemboursementRequest } from '../../models/remboursement-request';
import { CreditResponse } from '../../models/credit-response';
import { EcheanceCreditResponse } from '../../models/echeance-credit-response';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';
import { AuthService } from '../../../../core/services/auth.service';
import { CompteEpargneResponse } from '../../../epargne/models/compte-epargne-response';
import { CompteEpargneService } from '../../../epargne/services/compte-epargne.service';

@Component({
  selector: 'app-remboursement-credit-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './remboursement-credit-form.component.html'
})
export class RemboursementCreditFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly creditService = inject(CreditService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly workflowMessageService = inject(WorkflowMessageService);
  private readonly authService = inject(AuthService);
  private readonly compteEpargneService = inject(CompteEpargneService);

  private readonly remboursementRolesAutorises = ['ADMIN', 'CHEF_BUREAU', 'CAISSIER'];

  readonly modePaiementOptions: ModePaiement[] = ['ESPECES', 'COMPTE_EPARGNE'];
  readonly creditId = Number(this.route.snapshot.paramMap.get('creditId'));

  credit: CreditResponse | null = null;
  echeances: EcheanceCreditResponse[] = [];
  echeanceCourante: EcheanceCreditResponse | null = null;
  compteEpargne: CompteEpargneResponse | null = null;
  compteEpargneErrorMessage = '';

  loading = false;
  errorMessage = '';
  echeancesErrorMessage = '';
  successMessage = '';
  guidance: WorkflowGuidance | null = null;
  globalGuidance: WorkflowGuidance = {
    title: 'Suivi du dossier crédit',
    message: 'Ce formulaire est réservé aux remboursements effectués directement au bureau. Les remboursements collectés sur terrain doivent passer par la collecte, le billetage et le contrôle.',
    currentStep: 'Remboursement direct au bureau',
    nextStep: 'Régularisation puis clôture',
    expectedRole: 'CAISSIER',
    expectedAction: 'Suivre les remboursements',
    severity: 'info',
    canCurrentUserAct: false,
    blockedReason: 'Le chargement du rôle utilisateur est en cours.'
  };

  readonly form = this.fb.group({
    membreId: [{ value: null as number | null, disabled: true }, [Validators.required]],
    datePaiement: ['', [Validators.required]],
    montantTotal: [0, [Validators.required, Validators.min(0.01)]],
    modePaiement: ['ESPECES' as ModePaiement, [Validators.required]],
    observation: ['']
  });

  ngOnInit(): void {
    this.initialiserDatePaiement();
    this.chargerCreditEtEcheances();
  }

  private initialiserDatePaiement(): void {
    const now = new Date();
    const localDateTime = new Date(now.getTime() - now.getTimezoneOffset() * 60000)
      .toISOString()
      .slice(0, 16);

    this.form.patchValue({
      datePaiement: localDateTime
    });
  }

  chargerCreditEtEcheances(): void {
    this.loading = true;
    this.errorMessage = '';
    this.echeancesErrorMessage = '';
    this.successMessage = '';

    this.creditService.getById(this.creditId).pipe(
      finalize(() => {
        this.loading = false;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (credit) => {
        this.initialiserCreditCharge(credit);
        this.chargerCompteEpargneActif(credit.membreId);
        this.chargerEcheances();
      },
      error: (err) => {
        console.error(err);
        this.credit = null;
        this.errorMessage = 'Impossible de charger le crédit.';
      }
    });
  }

  private initialiserCreditCharge(credit: CreditResponse): void {
    this.credit = credit;
    const authWithCurrentUser = this.authService as AuthService & {
      getCurrentUser?: () => { role?: string; permissions?: string[] } | null;
    };
    const user = authWithCurrentUser.getCurrentUser?.() || null;
    const workflowGuidance = this.workflowMessageService.getGuidance({
      module: 'CREDIT',
      status: credit.statut,
      currentRole: user?.role,
      permissions: user?.permissions
    });
    this.globalGuidance = this.creerGuidanceRemboursement(this.globalGuidance);
    this.guidance = this.creerGuidanceRemboursement(workflowGuidance);

    this.form.patchValue({ membreId: credit.membreId });
  }

  private chargerCompteEpargneActif(membreId: number): void {
    this.compteEpargne = null;
    this.compteEpargneErrorMessage = '';

    this.compteEpargneService.getActiveByMembre(membreId).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (compte) => {
        this.compteEpargne = compte;
      },
      error: (err) => {
        console.error(err);
        this.compteEpargneErrorMessage = 'Aucun compte épargne actif disponible pour ce membre.';
      }
    });
  }

  private creerGuidanceRemboursement(guidance: WorkflowGuidance): WorkflowGuidance {
    const canSubmit = this.canSubmitRemboursement;

    return {
      ...guidance,
      currentStep: 'Remboursement direct au bureau',
      message: canSubmit
        ? 'Ce formulaire est réservé aux remboursements effectués directement au bureau. Les remboursements collectés sur terrain doivent passer par la collecte, le billetage et le contrôle.'
        : 'Vous pouvez suivre le remboursement du crédit. Les remboursements collectés sur terrain doivent passer par la collecte, le billetage et le contrôle; le remboursement direct au bureau est réservé au Caissier.',
      expectedRole: 'CAISSIER',
      expectedAction: canSubmit ? 'Enregistrer un remboursement direct au bureau' : 'Suivre les remboursements',
      canCurrentUserAct: canSubmit,
      blockedReason: canSubmit
        ? undefined
        : 'Votre rôle ne doit pas enregistrer directement une opération financière de remboursement.'
    };
  }

  private chargerEcheances(): void {
    const datePaiement = this.form.getRawValue().datePaiement;
    const dateReference = datePaiement
      ? datePaiement.substring(0, 10)
      : this.todayAsDateOnly();

    this.creditService.appliquerPenalites(this.creditId, dateReference).pipe(
      catchError((err) => {
        console.error(err);
        return of(null);
      }),
      switchMap(() => this.creditService.getEcheancesByCreditId(this.creditId)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (echeances) => {
        this.echeances = echeances;
        this.echeanceCourante = null;
        this.echeancesErrorMessage = '';
        this.selectionnerEcheanceCourante();
      },
      error: (err) => {
        console.error(err);
        this.echeances = [];
        this.echeanceCourante = null;
        this.echeancesErrorMessage = 'Les échéances n’ont pas pu être chargées.';
      }
    });
  }

  private selectionnerEcheanceCourante(): void {
    if (!this.echeances.length) {
      this.echeanceCourante = null;
      this.form.patchValue({ montantTotal: 0 });
      return;
    }

    const echeanceEnRetard = this.echeances.find(
      e => e.statut === 'EN_RETARD' && Number(e.resteAPayer ?? 0) > 0
    );

    const echeanceAPayer = this.echeances.find(
      e => e.statut !== 'PAYE' && Number(e.resteAPayer ?? 0) > 0
    );

    this.echeanceCourante = echeanceEnRetard ?? echeanceAPayer ?? null;

    if (!this.echeanceCourante) {
      this.form.patchValue({ montantTotal: 0 });
      return;
    }

    this.form.patchValue({
      montantTotal: Number(this.echeanceCourante.resteAPayer ?? 0)
    });
  }

  submit(): void {
    if (this.form.invalid || !this.credit) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.compteEpargneIndisponible || this.soldeDisponibleInsuffisant) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();

    const request: RemboursementRequest = {
      echeanceId: this.echeanceCourante?.id ?? null,
      membreId: this.credit.membreId,
      datePaiement: `${raw.datePaiement}:00`,
      montantTotal: Number(raw.montantTotal),
      modePaiement: (raw.modePaiement ?? 'ESPECES') as ModePaiement,
      createdBy: this.authService.getCurrentUser()?.id ?? null,
      observation: raw.observation?.trim() || null
    };

    this.loading = true;
    this.errorMessage = '';
    this.echeancesErrorMessage = '';
    this.successMessage = '';

    this.creditService.enregistrerRemboursement(this.creditId, request).pipe(
      finalize(() => {
        this.loading = false;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => {
        this.successMessage = 'Remboursement enregistré';

        setTimeout(() => {
          this.router.navigate(['/credits/liste']);
        }, 800);
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = err?.error?.message || 'Erreur remboursement';
      }
    });
  }

  private todayAsDateOnly(): string {
    const now = new Date();
    return new Date(now.getTime() - now.getTimezoneOffset() * 60000)
      .toISOString()
      .slice(0, 10);
  }

  get totalDuReel(): number {
    return this.echeances.reduce((total, e) => total + Number(e.resteAPayer ?? 0), 0);
  }

  get totalDejaPaye(): number {
    return this.echeances.reduce((total, e) => total + Number(e.totalPaye ?? 0), 0);
  }

  get resteAPayer(): number {
    return this.totalDuReel > 0 ? this.totalDuReel : Number(this.credit?.encoursPrincipal ?? 0);
  }

  get montantPayeInvalide(): boolean {
    return Number(this.form.getRawValue().montantTotal ?? 0) <= 0;
  }

  get modeCompteEpargne(): boolean {
    return this.form.getRawValue().modePaiement === 'COMPTE_EPARGNE';
  }

  get soldeDisponibleInsuffisant(): boolean {
    if (!this.modeCompteEpargne) {
      return false;
    }

    return Number(this.form.getRawValue().montantTotal ?? 0) > Number(this.compteEpargne?.soldeDisponible ?? 0);
  }

  get compteEpargneIndisponible(): boolean {
    return this.modeCompteEpargne && !this.compteEpargne;
  }

  get canSubmitRemboursement(): boolean {
    return this.authService.hasAnyRole(this.remboursementRolesAutorises);
  }

  formatMontant(value: number | null | undefined): string {
    const amount = Number(value ?? 0);
    const devise = this.credit?.devise || 'CDF';

    return `${new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(amount)} ${devise}`;
  }

  getStatutEcheanceClasses(statut: string | null | undefined): string {
    switch (statut) {
      case 'PAYE':
        return 'bg-emerald-100 text-emerald-800';
      case 'EN_RETARD':
        return 'bg-red-100 text-red-800';
      case 'PARTIELLEMENT_PAYE':
        return 'bg-amber-100 text-amber-800';
      default:
        return 'bg-slate-100 text-slate-700';
    }
  }

  get f() {
    return this.form.controls;
  }
}