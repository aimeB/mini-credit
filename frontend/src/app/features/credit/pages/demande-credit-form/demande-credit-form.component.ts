import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import { DemandeCreditService } from '../../services/demande-credit.service';
import { DUREE_UNITE_OPTIONS } from '../../../../shared/enums/duree-unite.enum';
import { PERIODICITE_REMBOURSEMENT_OPTIONS } from '../../../../shared/enums/periodicite-remboursement.enum';
import { DemandeCreditCreateRequest } from '../../models/demande-credit-create-request';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
export const TAUX_INTERET_PAR_DEFAUT = 9;


@Component({
  selector: 'app-demande-credit-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './demande-credit-form.component.html'
})
export class DemandeCreditFormComponent {

  
  private readonly fb = inject(FormBuilder);
  private readonly demandeCreditService = inject(DemandeCreditService);
  private readonly router = inject(Router);

  readonly dureeUniteOptions = DUREE_UNITE_OPTIONS;
  readonly periodiciteOptions = PERIODICITE_REMBOURSEMENT_OPTIONS;

  loading = false;
  errorMessage = '';
  successMessage = '';
  globalGuidance: WorkflowGuidance = {
    title: 'Suivi du dossier crédit',
    message: 'Cette page permet d’enregistrer une demande de crédit avant pré-analyse. Après soumission, le dossier passe par la pré-analyse du Gestionnaire, l’analyse du Contrôleur, la garantie, l’approbation puis le décaissement. Aucun décaissement n’est autorisé sans approbation, garantie validée et dossier complet.',
    currentStep: 'Demande enregistrée',
    nextStep: 'Pré-analyse Gestionnaire',
    expectedRole: 'Gestionnaire',
    expectedAction: 'Soumettre un dossier complet et cohérent',
    severity: 'info',
    canCurrentUserAct: true
  };

  readonly form = this.fb.group({
    membreId: [null as number | null, [Validators.required]],
    siteId: [null as number | null, [Validators.required]],
    agentId: [null as number | null],

    montantDemande: [null as number | null, [Validators.required, Validators.min(0.01)]],
    devise: ['USD', [Validators.required]],

    dureeValeur: [1, [Validators.required, Validators.min(1)]],
    dureeUnite: ['MOIS', [Validators.required]],
    periodiciteRemboursement: ['MENSUEL', [Validators.required]],

    tauxInteret: [TAUX_INTERET_PAR_DEFAUT, [Validators.required, Validators.min(0)]],
    objetCredit: ['', [Validators.required]],
    gagePropose: [''],
    activiteFinancee: [''],

    revenusEstimes: [0, [Validators.required, Validators.min(0)]],
    chargesEstimees: [0, [Validators.required, Validators.min(0)]],

    fraisDemande: [null as number | null, [Validators.min(0)]]
  });

  submit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();

    const request: DemandeCreditCreateRequest = {
      membreId: Number(raw.membreId),
      siteId: Number(raw.siteId),
      agentId: raw.agentId != null ? Number(raw.agentId) : null,

      montantDemande: Number(raw.montantDemande),
      devise: raw.devise ?? 'USD',

      dureeValeur: Number(raw.dureeValeur),
      dureeUnite: raw.dureeUnite as DemandeCreditCreateRequest['dureeUnite'],
      periodiciteRemboursement: raw.periodiciteRemboursement as DemandeCreditCreateRequest['periodiciteRemboursement'],

      tauxInteret: Number(raw.tauxInteret),

      objetCredit: raw.objetCredit ?? '',
  gagePropose: raw.gagePropose?.trim() || null,
      activiteFinancee: raw.activiteFinancee || null,

      revenusEstimes: Number(raw.revenusEstimes ?? 0),
      chargesEstimees: Number(raw.chargesEstimees ?? 0)
    };

    if (raw.fraisDemande !== null && raw.fraisDemande !== undefined) {
      request.fraisDemande = Number(raw.fraisDemande);
    }

    this.loading = true;

    this.demandeCreditService.create(request).subscribe({
      next: () => {
        this.loading = false;
        this.successMessage = 'Demande de crédit créée avec succès.';

        this.form.reset({
          membreId: null,
          siteId: null,
          agentId: null,

          montantDemande: null,
          devise: 'USD',

          dureeValeur: 1,
          dureeUnite: 'MOIS',
          periodiciteRemboursement: 'MENSUEL',

          tauxInteret: TAUX_INTERET_PAR_DEFAUT,
          objetCredit: '',
          gagePropose: '',
          activiteFinancee: '',

          revenusEstimes: 0,
          chargesEstimees: 0,

          fraisDemande: null
        });

        setTimeout(() => {
          this.router.navigate(['/credits/demandes']);
        }, 700);
      },
      error: (error) => {
        console.error('Erreur création demande de crédit :', error);
        this.loading = false;
        this.errorMessage = error?.error?.message || 'Impossible de créer la demande de crédit.';
      }
    });
  }

  get f() {
    return this.form.controls;
  }
}