import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';
import { DepenseCaisseService } from '../../services/depense-caisse.service';
import { DepenseCaisseResponse } from '../../models/depense-caisse-response';

@Component({
  selector: 'app-depense-caisse-validation',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './depense-caisse-validation.component.html'
})
export class DepenseCaisseValidationComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly depenseService = inject(DepenseCaisseService);
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly workflowMessageService = inject(WorkflowMessageService);

  depense?: DepenseCaisseResponse;
  readonly globalGuidance: WorkflowGuidance = {
    title: 'Suivi des dépenses de caisse',
    message: 'Cette page permet de suivre les dépenses d\'exploitation. Une dépense doit être demandée, validée par le rôle autorisé, puis payée par le Caissier. Une dépense en attente de validation ne doit pas être payée avant autorisation. Une dépense payée devient historisée et consultable pour le contrôle, les rapports et l\'audit.',
    currentStep: 'Traitement de la dépense',
    nextStep: 'Validation puis paiement',
    expectedRole: 'Chef de Bureau',
    expectedAction: 'Valider ou payer selon le statut',
    severity: 'info',
    canCurrentUserAct: true
  };
  guidance: WorkflowGuidance | null = null;
  loading = false;
  error = '';
  form = this.fb.group({
    commentaire: this.fb.control<string>('', []),
    commentaireValidation: this.fb.control<string>('')
  });

  get commentaireControl() {
    return this.form.controls.commentaire;
  }

  get commentaireValidationControl() {
    return this.form.controls.commentaireValidation;
  }

  get canValidateDepense(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'CHEF_BUREAU', 'CONTROLEUR'])
      && this.depense?.statut === 'EN_ATTENTE_VALIDATION';
  }

  get canPayDepense(): boolean {
    return this.authService.hasRole('CAISSIER')
      && this.depense?.statut === 'VALIDEE';
  }

  get canAnnulerDepense(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'CHEF_BUREAU', 'CONTROLEUR'])
      && !!this.depense
      && !['PAYEE', 'ANNULEE'].includes(this.depense.statut);
  }

  get showWaitingPaymentMessage(): boolean {
    return !!this.depense
      && this.depense.statut === 'VALIDEE'
      && !this.authService.hasRole('CAISSIER');
  }

  get showPayActionMessage(): boolean {
    return !!this.depense
      && this.depense.statut === 'VALIDEE'
      && this.authService.hasRole('CAISSIER');
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error = 'Identifiant de dépense invalide.';
      return;
    }

    this.depenseService.getById(id).subscribe({
      next: (data) => {
        this.depense = data;
        this.guidance = this.workflowMessageService.getGuidance({
          module: 'DEPENSE_CAISSE',
          status: data.statut,
          currentRole: this.authService.getCurrentUser()?.role,
          expectedRole: this.resolveExpectedRoleForStatus(data.statut)
        });
      },
      error: (err) => this.error = err?.error?.message || 'Impossible de charger la dépense.'
    });
  }

  valider(): void {
    if (!this.depense) return;
    this.loading = true;
    this.depenseService.valider(this.depense.id, { commentaireValidation: this.form.value.commentaireValidation || undefined }).pipe(finalize(() => this.loading = false)).subscribe({
      next: () => this.router.navigate(['/caisses/depenses']),
      error: (err) => this.error = err?.error?.message || 'Validation impossible.'
    });
  }

  rejeter(): void {
    const commentaire = (this.form.value.commentaire || '').trim();
    if (!commentaire) {
      this.error = 'Le commentaire est obligatoire pour un rejet.';
      return;
    }
    if (!this.depense) return;
    this.loading = true;
    this.depenseService.rejeter(this.depense.id, { commentaire }).pipe(finalize(() => this.loading = false)).subscribe({
      next: () => this.router.navigate(['/caisses/depenses']),
      error: (err) => this.error = err?.error?.message || 'Rejet impossible.'
    });
  }

  payer(): void {
    if (!this.depense) return;
    this.loading = true;
    this.depenseService.payer(this.depense.id, { commentaire: this.form.value.commentaireValidation || undefined }).pipe(finalize(() => this.loading = false)).subscribe({
      next: () => this.router.navigate(['/caisses/depenses']),
      error: (err) => this.error = err?.error?.message || 'Paiement impossible.'
    });
  }

  annuler(): void {
    if (!this.depense) return;
    this.loading = true;
    this.depenseService.annuler(this.depense.id, this.form.value.commentaire || undefined).pipe(finalize(() => this.loading = false)).subscribe({
      next: () => this.router.navigate(['/caisses/depenses']),
      error: (err) => this.error = err?.error?.message || 'Annulation impossible.'
    });
  }

  private resolveExpectedRoleForStatus(status: string): string | undefined {
    if (status === 'EN_ATTENTE_VALIDATION') {
      if (this.authService.hasAnyRole(['ADMIN', 'CHEF_BUREAU', 'CONTROLEUR'])) {
        return this.authService.getCurrentUser()?.role;
      }
      return 'CHEF_BUREAU / CONTROLEUR';
    }

    if (status === 'VALIDEE' || status === 'APPROUVEE') {
      return 'CAISSIER';
    }

    return undefined;
  }
}