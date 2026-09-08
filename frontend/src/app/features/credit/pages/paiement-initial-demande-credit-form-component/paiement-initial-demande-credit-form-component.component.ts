import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { PaiementInitialDemandeCreditService } from '../../services/paiement-initial-demande-credit.service';
import { DemandeCreditService } from '../../services/demande-credit.service';
import { DemandeCreditResponse } from '../../models/demande-credit-response';
import { PaiementInitialDemandeCreditRequest } from '../../models/paiement-initial-demande-credit-request';
import { MODE_PAIEMENT_OPTIONS } from '../../../../shared/enums/mode-paiement.enum';
import { AuthService } from '../../../../core/services/auth.service';
import { SessionCaisseService } from '../../../caisse/services/session-caisse.service';
import { SessionCaisseResponse } from '../../../caisse/models/session-caisse-response';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';

@Component({
  selector: 'app-paiement-initial-demande-credit-form-component',
  imports: [CommonModule, ReactiveFormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './paiement-initial-demande-credit-form-component.component.html'
})
export class PaiementInitialDemandeCreditFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly paiementInitialService = inject(PaiementInitialDemandeCreditService);
  private readonly demandeCreditService = inject(DemandeCreditService);
  private readonly authService = inject(AuthService);
  private readonly sessionCaisseService = inject(SessionCaisseService);
  private readonly workflowMessageService = inject(WorkflowMessageService);

  readonly modePaiementOptions = MODE_PAIEMENT_OPTIONS;

  demandeId = Number(this.route.snapshot.paramMap.get('id'));
  demande: DemandeCreditResponse | null = null;
  sessionActive: SessionCaisseResponse | null = null;

  loading = false;
  loadingSession = false;
  submitting = false;
  errorMessage = '';
  sessionErrorMessage = '';
  successMessage = '';
  guidance: WorkflowGuidance | null = null;
  returnUrl = this.resolveReturnUrl();
  currentDateTime = this.getCurrentDateTime();

  private readonly statutsFraisDemandeAutorises = new Set([
    'SOUMISE',
    'EN_ANALYSE',
    'ANALYSE_TERRAIN_VALIDEE',
    'VALIDATION_CHEF',
    'VALIDATION_CONTROLEUR',
    'APPROUVEE'
  ]);

  private buildBlockedDisbursementMessage(status?: string | null): string {
    const currentStatus = (status || this.demande?.statut || 'INCONNU').toUpperCase();
    return `Paiement initial indisponible au statut ${currentStatus}. Les frais de demande sont encaissables avant analyse, mais la garantie reste réservée au workflow après approbation.`;
  }

  readonly form = this.fb.group({
    modePaiement: ['ESPECES' as PaiementInitialDemandeCreditRequest['modePaiement'], [Validators.required]],
    fraisPayes: [0, [Validators.required, Validators.min(0.01)]],
    observation: ['']
  });

  ngOnInit(): void {
    if (!this.demandeId || Number.isNaN(this.demandeId)) {
      this.errorMessage = 'Identifiant de demande invalide.';
      return;
    }

    this.chargerSessionActive();
    this.chargerDemande();
  }
  private getCurrentDateTime(): string {
    const now = new Date();
    return now.toISOString().slice(0, 16); // Format: YYYY-MM-DDTHH:mm
  }

  chargerSessionActive(): void {
    this.loadingSession = true;
    this.sessionErrorMessage = '';

    this.sessionCaisseService.getSessionActive().subscribe({
      next: (session) => {
        this.sessionActive = session;
        this.loadingSession = false;
      },
      error: (error) => {
        console.error('Erreur chargement session caisse active :', error);
        this.sessionActive = null;
        this.sessionErrorMessage = error?.error?.message || 'Aucune session caisse ouverte. Ouvrez une session caisse avant d’encaisser les frais.';
        this.loadingSession = false;
      }
    });
  }

  chargerDemande(): void {
    this.loading = true;
    this.errorMessage = '';

    this.demandeCreditService.getById(this.demandeId).subscribe({
      next: (data) => {
        this.demande = data;
        this.guidance = this.workflowMessageService.getGuidance({
          module: 'CREDIT',
          status: data.statut,
          currentRole: this.authService.getCurrentUser()?.role,
          permissions: this.authService.getCurrentUser()?.permissions,
          metadata: {
            expectedRole: 'CAISSIER'
          }
        });
        this.form.controls.fraisPayes.setValue(this.fraisDemandeRestant, { emitEvent: false });
        if (!this.canPayFraisDemande) {
          this.errorMessage = this.buildBlockedDisbursementMessage(data.statut);
          this.form.disable();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur chargement demande :', error);
        this.errorMessage = error?.error?.message || 'Impossible de charger la demande.';
        this.loading = false;
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

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const fraisPayes = Number(raw.fraisPayes ?? 0);

    if (!this.sessionActive) {
      this.sessionErrorMessage = 'Aucune session caisse ouverte. Ouvrez une session caisse avant d’encaisser les frais.';
      return;
    }

    if (fraisPayes > 0 && !this.canPayFraisDemande) {
      this.errorMessage = this.buildBlockedDisbursementMessage(this.demande?.statut);
      return;
    }

    if (fraisPayes <= 0) {
      this.errorMessage = 'Le montant à encaisser doit être supérieur à zéro.';
      return;
    }

    if (fraisPayes > this.fraisDemandeRestant) {
      this.errorMessage = 'Le montant à encaisser dépasse le reste frais à payer.';
      return;
    }

    const request: PaiementInitialDemandeCreditRequest = {
      modePaiement: raw.modePaiement!,
      fraisPayes,
      observation: raw.observation || undefined
    };

    this.submitting = true;

    this.paiementInitialService.enregistrerPaiementInitial(this.demandeId, request).subscribe({
      next: () => {
        this.submitting = false;
        this.successMessage = 'Paiement initial enregistré avec succès.';

        setTimeout(() => {
          this.router.navigateByUrl(this.returnUrl);
        }, 700);
      },
      error: (error) => {
        console.error('Erreur paiement initial :', error);
        this.submitting = false;
        this.errorMessage = error?.error?.message || 'Impossible d’enregistrer le paiement initial.';
      }
    });
  }

  get f() {
    return this.form.controls;
  }

  get canPayFraisDemande(): boolean {
    return this.statutsFraisDemandeAutorises.has((this.demande?.statut || '').toUpperCase());
  }

  get canPayDepotGarantie(): boolean {
    return (this.demande?.statut || '').toUpperCase() === 'APPROUVEE';
  }

  get currentUserName(): string {
    return this.authService.getCurrentUser()?.nomComplet || this.sessionActive?.utilisateurNom || '-';
  }

  get canSubmit(): boolean {
    return !!this.sessionActive
      && !this.loadingSession
      && !this.submitting
      && this.canPayFraisDemande
      && this.fraisDemandeRestant > 0
      && this.form.valid;
  }

  get fraisDemandeRestant(): number {
    if (!this.demande) {
      return 0;
    }
    return Math.max(Number(this.demande.fraisDemande ?? 0) - Number(this.demande.fraisDemandePayes ?? 0), 0);
  }

  private resolveReturnUrl(): string {
    const rawReturnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/credits/demandes';
    return rawReturnUrl.startsWith('/') ? rawReturnUrl : '/credits/demandes';
  }
}
