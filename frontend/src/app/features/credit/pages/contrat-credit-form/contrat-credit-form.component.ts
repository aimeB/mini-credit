import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { finalize } from 'rxjs';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { CreditContratResponse } from '../../models/credit-contrat-response';
import { CreditService } from '../../services/credit.service';

@Component({
  selector: 'app-contrat-credit-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './contrat-credit-form.component.html'
})
export class ContratCreditFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly creditService = inject(CreditService);

  readonly creditId = Number(this.route.snapshot.paramMap.get('creditId'));

  contrat: CreditContratResponse | null = null;
  loading = false;
  errorMessage = '';

  globalGuidance: WorkflowGuidance = {
    title: 'Suivi du dossier crédit',
    message: 'Le contrat s’inscrit dans le cycle crédit entre décaissement, remboursement et clôture. Le dossier doit rester conforme et traçable pour le contrôle interne et l’audit.',
    currentStep: 'Suivi contractuel',
    nextStep: 'Remboursement puis clôture',
    expectedRole: 'Gestionnaire',
    expectedAction: 'Consulter les informations contractuelles du dossier',
    severity: 'info',
    canCurrentUserAct: true
  };

  ngOnInit(): void {
    this.loadContrat();
  }

  loadContrat(): void {
    if (!this.creditId) {
      this.errorMessage = 'Crédit introuvable.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.creditService.getContrat(this.creditId).pipe(
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: (contrat) => {
        this.contrat = contrat;
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = err?.error?.message || 'Impossible de charger le contrat de crédit.';
      }
    });
  }

  imprimer(): void {
    window.print();
  }

  valueOrDefault(value: string | number | null | undefined): string | number {
    if (typeof value === 'string') {
      return value.trim() ? value : 'Non renseigné';
    }

    return value ?? 'Non renseigné';
  }

  formatDuree(contrat: CreditContratResponse): string {
    if (!contrat.dureeValeur && !contrat.dureeUnite) {
      return 'Non renseigné';
    }

    return `${contrat.dureeValeur ?? ''} ${contrat.dureeUnite ?? ''}`.trim();
  }

  trackByEcheanceId(_: number, item: { id: number }): number {
    return item.id;
  }
}