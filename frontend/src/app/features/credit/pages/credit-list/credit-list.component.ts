import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';

import { CreditService } from '../../services/credit.service';
import { CreditResponse } from '../../models/credit-response';
import { DecaissementCreditRequest } from '../../models/decaissement-credit-request';
import { MODE_PAIEMENT_OPTIONS } from '../../../../shared/enums/mode-paiement.enum';


import { SessionCaisseService } from '../../../caisse/services/session-caisse.service';
import { SessionCaisseResponse } from '../../../caisse/models/session-caisse-response';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-credit-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './credit-list.component.html'
})
export class CreditListComponent implements OnInit {
  private readonly creditService = inject(CreditService);
  private readonly sessionCaisseService = inject(SessionCaisseService);
  private readonly route = inject(ActivatedRoute);
  private readonly workflowMessageService = inject(WorkflowMessageService);
  private readonly authService = inject(AuthService);

  credits: CreditResponse[] = [];
  allCredits: CreditResponse[] = [];
  loading = false;
  errorMessage = '';
  filterStatut = '';
  guidanceByCreditId = new Map<number, WorkflowGuidance>();
  globalGuidance: WorkflowGuidance = {
    title: 'Suivi du dossier crédit',
    message: 'Cette page permet de suivre les crédits approuvés, décaissés et en remboursement. Aucun décaissement n’est autorisé sans approbation, garantie validée et dossier complet. Les retards doivent être suivis avec application des pénalités prévues.',
    currentStep: 'Suivi crédits actifs',
    nextStep: 'Décaissement, remboursement puis clôture',
    expectedRole: 'Caissier',
    expectedAction: 'Décaisser ou suivre les remboursements selon le statut',
    severity: 'info',
    canCurrentUserAct: true
  };

  decaissementModalOpen = false;
  decaissementSubmitting = false;
  decaissementErrorMessage = '';
  decaissementSuccessMessage = '';

  selectedCredit: CreditResponse | null = null;

  modePaiements = MODE_PAIEMENT_OPTIONS;
  sessionActive: SessionCaisseResponse | null = null;

  decaissementForm: DecaissementCreditRequest = {
    dateDecaissement: '',
    sessionCaisseId: 0,
    createdBy: 0,
    modePaiement: 'ESPECES',
    observation: ''
  };

  ngOnInit(): void {
    this.filterStatut = this.route.snapshot.queryParamMap.get('statut') || '';
    this.loadSessionActive();
    this.loadCredits();
  }

  loadCredits(): void {
    this.loading = true;
    this.errorMessage = '';

    const routeCreditId = this.getRouteCreditId();
    if (routeCreditId) {
      this.creditService.getById(routeCreditId).subscribe({
        next: (data) => {
          this.allCredits = data ? [data] : [];
          this.credits = data ? [data] : [];
          this.rebuildGuidance();
          this.loading = false;
          if (data) {
            this.ouvrirPopupDecaissement(data);
          }
        },
        error: (err) => {
          console.error(err);
          this.errorMessage = 'Impossible de charger le crédit à décaisser.';
          this.loading = false;
        }
      });
      return;
    }

    this.creditService.getCreditsADecaisser().subscribe({
      next: (data) => {
        this.allCredits = data ?? [];
        this.credits = this.filterStatut
          ? this.allCredits.filter(c => c.statut === this.filterStatut)
          : [...this.allCredits];
        this.rebuildGuidance();
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur chargement crédits approuvés', err);
        this.errorMessage = 'Impossible de charger les crédits approuvés.';
        this.loading = false;
      }
    });
  }

  loadSessionActive(): void {
    this.sessionCaisseService.getSessionActive().subscribe({
      next: (data: SessionCaisseResponse) => {
        this.sessionActive = data;
        if (this.decaissementModalOpen && this.decaissementForm.sessionCaisseId <= 0) {
          this.decaissementForm.sessionCaisseId = data.id;
        }
      },
      error: (err: unknown) => {
        console.error(err);
        this.sessionActive = null;
      }
    });
  }

  trackByCreditId(_: number, credit: CreditResponse): number {
    return credit.id;
  }

  canDecaisser(credit: CreditResponse): boolean {
    return this.authService.hasAnyRole(['CAISSIER']) && credit.statut === 'APPROUVE';
  }

  canRembourser(credit: CreditResponse): boolean {
    return credit.statut === 'DECAISSE'
      || credit.statut === 'EN_COURS'
      || credit.statut === 'EN_RETARD';
  }

  getCreditGuidance(credit: CreditResponse): WorkflowGuidance {
    return this.guidanceByCreditId.get(credit.id) || this.buildCreditGuidance(credit);
  }

  ouvrirPopupDecaissement(credit: CreditResponse): void {
    this.selectedCredit = credit;
    this.decaissementModalOpen = true;
    this.decaissementSubmitting = false;
    this.decaissementErrorMessage = '';
    this.decaissementSuccessMessage = '';

    this.decaissementForm = {
      dateDecaissement: this.getNowForDateTimeLocal(),
      sessionCaisseId: this.sessionActive?.id ?? 0,
      createdBy: this.getCurrentUserId() ?? 0,
      modePaiement: 'ESPECES',
      observation: ''
    };
  }

  fermerPopupDecaissement(): void {
    this.decaissementModalOpen = false;
    this.decaissementSubmitting = false;
    this.decaissementErrorMessage = '';
    this.decaissementSuccessMessage = '';
    this.selectedCredit = null;
  }

  confirmerDecaissement(): void {
    if (!this.selectedCredit) {
      this.decaissementErrorMessage = 'Veuillez sélectionner un crédit.';
      return;
    }

    if (!this.decaissementForm.dateDecaissement) {
      this.decaissementErrorMessage = 'Veuillez renseigner la date de décaissement.';
      return;
    }

    if (!this.sessionActive || !this.decaissementForm.sessionCaisseId || this.decaissementForm.sessionCaisseId <= 0) {
      this.decaissementErrorMessage = 'Aucune session de caisse active n\'est disponible.';
      return;
    }

    const currentUserId = this.getCurrentUserId();
    if (!currentUserId) {
      this.decaissementErrorMessage = 'Utilisateur connecté introuvable pour enregistrer le décaissement.';
      return;
    }

    this.decaissementSubmitting = true;
    this.decaissementErrorMessage = '';
    this.decaissementSuccessMessage = '';

    const payload: DecaissementCreditRequest = {
      dateDecaissement: this.decaissementForm.dateDecaissement,
      sessionCaisseId: Number(this.decaissementForm.sessionCaisseId),
      createdBy: currentUserId,
      modePaiement: this.decaissementForm.modePaiement,
      observation: this.decaissementForm.observation?.trim() || null
    };

    this.creditService.decaisserCredit(this.selectedCredit.id, payload).subscribe({
      next: (updatedCredit) => {
        this.credits = this.credits.map(c =>
          c.id === updatedCredit.id ? updatedCredit : c
        );
        this.rebuildGuidance();

        this.decaissementSubmitting = false;
        this.decaissementSuccessMessage = 'Le crédit a été décaissé avec succès.';

        setTimeout(() => {
          this.fermerPopupDecaissement();
        }, 800);
      },
      error: (err) => {
        console.error(err);
        this.decaissementSubmitting = false;
        this.decaissementErrorMessage =
          err?.error?.message || 'Impossible d\'enregistrer le décaissement.';
      }
    });
  }

  getSelectedCreditAgentLabel(): string {
    return this.selectedCredit?.agentTerrainNom?.trim() || '-- Aucun agent associé au dossier --';
  }

  private getNowForDateTimeLocal(): string {
    const now = new Date();
    const offset = now.getTimezoneOffset();
    const local = new Date(now.getTime() - offset * 60000);
    return local.toISOString().slice(0, 16);
  }

  private getRouteCreditId(): number | null {
    const value = Number(this.route.snapshot.paramMap?.get('creditId'));
    return value && !Number.isNaN(value) ? value : null;
  }

  private isDecaissementRoute(): boolean {
    return this.getRouteCreditId() !== null;
  }

  private getCurrentUserId(): number | null {
    return this.authService.getCurrentUser()?.id ?? null;
  }

  private rebuildGuidance(): void {
    this.guidanceByCreditId = new Map(this.credits.map(credit => [credit.id, this.buildCreditGuidance(credit)]));
  }

  private buildCreditGuidance(credit: CreditResponse): WorkflowGuidance {
    const authWithCurrentUser = this.authService as AuthService & {
      getCurrentUser?: () => { role?: string; permissions?: string[] } | null;
    };
    const user = authWithCurrentUser.getCurrentUser?.() || null;

    return this.workflowMessageService.getGuidance({
      module: 'CREDIT',
      status: credit.statut,
      currentRole: user?.role,
      permissions: user?.permissions
    });
  }
}