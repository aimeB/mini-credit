import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { DemandeRetraitEpargneService } from '../../services/demande-retrait-epargne.service';
import { CompteEpargneService } from '../../services/compte-epargne.service';
import { DemandeRetraitEpargneResponse } from '../../models/demande-retrait-epargne';
import { StatutDemandeRetrait, STATUT_LABELS, STATUT_COLORS } from '../../models/statut-demande-retrait.enum';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';
import { TicketRecuService } from '../../services/ticket-recu.service';
import { TicketRecuResponse } from '../../models/ticket-recu.model';

/**
 * Composant pour afficher le détail d'une demande de retrait épargne (PHASE 6B.3)
 * 
 * Features:
 * - Affiche toutes les infos de la demande
 * - Actions Controleur: Valider, Refuser
 * - Actions Caissier: Payer (décaisser)
 * - Actions Membre: Annuler
 * - Affiche soldes (disponible, bloqué)
 * - Messages d'erreur clairs
 */
@Component({
  selector: 'app-demande-retrait-epargne-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="mc-page-wide" *ngIf="demande; else loadingState">
      <header class="mc-page-hero">
        <button type="button" class="mc-btn bg-white/10 text-white ring-1 ring-white/20 hover:bg-white/20" (click)="goBack()">
          <span aria-hidden="true">←</span>
          Retour aux retraits
        </button>

        <div class="mt-4 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <p class="text-xs font-bold uppercase tracking-wide text-slate-200">Demande de retrait épargne</p>
            <h1 class="mc-page-title">Demande de retrait {{ referenceRetrait }}</h1>
            <p class="mc-page-subtitle">
              {{ displayValue(demande.membreNom) }} · {{ displayValue(demande.compteEpargneNumero) }} · {{ getStatutLabel(demande.statut) }}
            </p>
          </div>

          <div class="mc-button-row">
            <span class="mc-badge bg-white text-slate-900">{{ getStatutLabel(demande.statut) }}</span>
            <span class="mc-badge bg-blue-100 text-blue-800">{{ workflowBadge }}</span>
          </div>
        </div>
      </header>

      <div *ngIf="validationMessage" class="mc-state" [ngClass]="validationMessageType === 'success' ? 'mc-state-success' : 'mc-state-danger'">
        {{ validationMessage }}
      </div>

      <main class="grid gap-6 lg:grid-cols-[minmax(0,1fr)_420px]">
        <section class="space-y-6">
          <section class="mc-panel space-y-5">
            <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <h2 class="mb-0 text-lg font-bold text-slate-900">Résumé financier</h2>
              <span class="mc-badge bg-slate-100 text-slate-700">{{ referenceRetrait }}</span>
            </div>

            <div class="mc-kpi-grid">
              <article class="mc-kpi-card">
                <span class="mc-kpi-label">Montant demandé</span>
                <strong class="mc-kpi-value block">{{ formatCdf(demande.montantDemande) }}</strong>
              </article>
              <article class="mc-kpi-card mc-kpi-card-success">
                <span class="mc-kpi-label text-emerald-700">Commission retrait</span>
                <strong class="mc-kpi-value block text-emerald-900">{{ formatCdf(demande.fraisRetrait || 0) }}</strong>
              </article>
              <article class="mc-kpi-card">
                <span class="mc-kpi-label">Taux commission</span>
                <strong class="mc-kpi-value block">{{ tauxCommissionRetrait | number: '1.2-2' }} %</strong>
              </article>
              <article class="mc-kpi-card mc-kpi-card-warning">
                <span class="mc-kpi-label text-amber-700">Total débité du compte</span>
                <strong class="mc-kpi-value block text-amber-900">{{ formatCdf(montantTotalDebite) }}</strong>
              </article>
              <article class="mc-kpi-card">
                <span class="mc-kpi-label">Montant remis au membre</span>
                <strong class="mc-kpi-value block">{{ formatCdf(montantRemisAuMembre) }}</strong>
              </article>
              <article class="mc-kpi-card mc-kpi-card-success">
                <span class="mc-kpi-label text-emerald-700">Solde disponible</span>
                <strong class="mc-kpi-value block text-emerald-900">{{ formatCdf(soldeDisponible) }}</strong>
              </article>
            </div>

            <div *ngIf="montantTotalDebite > soldeDisponible" class="mc-state mc-state-warning">
              Le total débité dépasse le solde disponible actuel.
            </div>
          </section>

          <section class="mc-panel space-y-5">
            <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <h2 class="mb-0 text-lg font-bold text-slate-900">Compte épargne</h2>
              <span class="mc-badge bg-slate-100 text-slate-700">Consultation des soldes</span>
            </div>

            <div class="grid gap-4 text-sm sm:grid-cols-2">
              <div>
                <span class="block text-slate-500">N° compte</span>
                <strong class="text-slate-900">{{ displayValue(demande.compteEpargneNumero) }}</strong>
              </div>
              <div>
                <span class="block text-slate-500">Membre</span>
                <strong class="text-slate-900">{{ displayValue(demande.membreNom) }}</strong>
              </div>
            </div>

            <div class="mc-kpi-grid">
              <div class="mc-kpi-card mc-kpi-card-success">
                <span class="mc-kpi-label text-emerald-700">Solde disponible</span>
                <strong class="mc-kpi-value block text-emerald-900">{{ formatCdf(soldeDisponible) }}</strong>
              </div>
              <div class="mc-kpi-card mc-kpi-card-warning">
                <span class="mc-kpi-label text-amber-700">Solde bloqué garantie</span>
                <strong class="mc-kpi-value block text-amber-900">{{ formatCdf(soldeBloque) }}</strong>
              </div>
              <div class="mc-kpi-card">
                <span class="mc-kpi-label">Montant maximum retirable</span>
                <strong class="mc-kpi-value block">{{ formatCdf(montantMaximumRetirable) }}</strong>
              </div>
            </div>
          </section>
        </section>

        <aside class="space-y-6">
          <section class="mc-panel space-y-4">
            <div class="flex items-center justify-between gap-3">
              <h2 class="mb-0 text-lg font-bold text-slate-900">Workflow</h2>
              <span class="mc-badge bg-blue-100 text-blue-800">{{ workflowBadge }}</span>
            </div>

            <div class="grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-1">
              <article>
                <span class="block text-slate-500">Étape actuelle</span>
                <strong class="text-slate-900">{{ currentStepLabel }}</strong>
              </article>
              <article>
                <span class="block text-slate-500">Rôle attendu</span>
                <strong class="text-slate-900">{{ expectedRoleLabel }}</strong>
              </article>
              <article>
                <span class="block text-slate-500">Étape suivante</span>
                <strong class="text-slate-900">{{ nextStepLabel }}</strong>
              </article>
              <article>
                <span class="block text-slate-500">Action attendue</span>
                <strong class="text-slate-900">{{ expectedActionLabel }}</strong>
              </article>
            </div>
          </section>

          <section class="mc-panel space-y-4">
            <div>
              <h2 class="mb-0 text-lg font-bold text-slate-900">Dates et traçabilité</h2>
            </div>

            <dl class="space-y-3 text-sm">
              <div>
                <dt class="text-slate-500">Date création</dt>
                <dd class="font-semibold text-slate-900">{{ formatDate(demande.dateDemande || demande.createdAt) }}</dd>
              </div>
              <div>
                <dt class="text-slate-500">Créé par</dt>
                <dd class="font-semibold text-slate-900">Non renseigné</dd>
              </div>
              <div>
                <dt class="text-slate-500">Validé par</dt>
                <dd class="font-semibold text-slate-900">{{ displayValue(demande.valideParNom) }}</dd>
              </div>
              <div>
                <dt class="text-slate-500">Date validation</dt>
                <dd class="font-semibold text-slate-900">{{ formatDate(demande.dateValidation) }}</dd>
              </div>
              <div>
                <dt class="text-slate-500">Payé par</dt>
                <dd class="font-semibold text-slate-900">{{ demande.operationCaisseSortieId ? 'Caissier' : 'Non renseigné' }}</dd>
              </div>
              <div>
                <dt class="text-slate-500">Date paiement</dt>
                <dd class="font-semibold text-slate-900">Non renseigné</dd>
              </div>
              <div>
                <dt class="text-slate-500">Observation</dt>
                <dd class="font-semibold text-slate-900">{{ displayValue(demande.observation) }}</dd>
              </div>
            </dl>

            <div *ngIf="demande.operationCaisseSortieId || demande.operationCaisseFraisId" class="mc-state mc-state-info">
              <p><span class="font-semibold">Opération sortie</span> {{ demande.operationCaisseSortieId || 'Non créée' }}</p>
              <p><span class="font-semibold">Opération frais</span> {{ demande.operationCaisseFraisId || 'Non créée' }}</p>
            </div>
          </section>

          <section *ngIf="demande.statut === 'REJETEE'" class="mc-state mc-state-danger">
            <h2 class="mb-1 text-lg font-bold text-slate-900">Demande rejetée</h2>
            <p>{{ displayValue(demande.motifRejet) }}</p>
          </section>

          <section *ngIf="demande.statut === 'VALIDEE'" class="mc-state mc-state-warning">
            <h2 class="mb-1 text-lg font-bold text-slate-900">En attente paiement caissier</h2>
            <p>La demande est validée. Le caissier peut procéder au paiement.</p>
          </section>

          <section *ngIf="demande.statut === 'DECAISSEE'" class="mc-state mc-state-success">
            <h2 class="mb-1 text-lg font-bold text-slate-900">Retrait payé</h2>
            <p>Les fonds ont été remis au membre et le retrait est historisé.</p>
          </section>

          <section *ngIf="demande.statut === 'DECAISSEE'" class="mc-panel space-y-4">
            <div class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <h2 class="mb-0 text-lg font-bold text-slate-900">Ticket reçu</h2>
                <p class="mt-1 text-sm leading-6 text-slate-600">Preuve imprimable liée au paiement du retrait.</p>
              </div>
              <span *ngIf="ticketRecu" class="mc-badge bg-slate-100 text-slate-700">{{ ticketRecu.statut }}</span>
            </div>

            <div *ngIf="ticketLoading" class="mc-state mc-state-info">Recherche du ticket reçu...</div>

            <div *ngIf="ticketRecu; else noTicketRetrait" class="space-y-4">
              <div class="rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm">
                <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <span class="block text-slate-500">Numéro ticket</span>
                    <strong class="text-slate-900">{{ ticketRecu.numeroTicket }}</strong>
                  </div>
                  <div>
                    <span class="block text-slate-500">Code vérification</span>
                    <strong class="text-slate-900">{{ ticketRecu.codeVerification }}</strong>
                  </div>
                </div>
                <div class="mt-3 grid gap-3 sm:grid-cols-2">
                  <div><span class="block text-slate-500">Montant remis</span><strong>{{ formatCdf(ticketRecu.montantRemisMembre || ticketRecu.montantPrincipal) }}</strong></div>
                  <div><span class="block text-slate-500">Total débité</span><strong>{{ formatCdf(ticketRecu.montantTotalDebite || 0) }}</strong></div>
                </div>
              </div>

              <div class="mc-button-row">
                <button type="button" class="mc-btn mc-btn-primary" (click)="imprimerTicket()" [disabled]="!canPrintTicket()">Imprimer le ticket</button>
                <button type="button" class="mc-btn mc-btn-dark" (click)="genererDuplicata()" [disabled]="!canDuplicataTicket()">Générer duplicata</button>
              </div>
            </div>

            <ng-template #noTicketRetrait>
              <div class="mc-state mc-state-warning" *ngIf="!ticketLoading">Aucun ticket reçu trouvé pour cette demande.</div>
            </ng-template>
          </section>

          <section class="mc-panel space-y-4">
            <div>
              <h2 class="mb-0 text-lg font-bold text-slate-900">Décision du contrôleur</h2>
            </div>
            <p class="text-sm leading-6 text-slate-600">
              Après vérification du compte, du solde disponible et des garanties bloquées, le contrôleur peut valider ou rejeter la demande.
            </p>

            <div *ngIf="isControleur && canValidate(); else controllerActionsUnavailable" class="space-y-4">
              <div class="mc-button-row">
                <button type="button" class="mc-btn mc-btn-success" (click)="validerDemande()">Valider la demande</button>
                <button type="button" class="mc-btn bg-rose-600 text-white hover:bg-rose-700" (click)="showRejectForm = true" *ngIf="!showRejectForm">Rejeter la demande</button>
              </div>

              <div *ngIf="showRejectForm" class="space-y-3 rounded-2xl border border-rose-200 bg-rose-50 p-4">
                <label for="rejectReason" class="mc-field-label">Motif du rejet *</label>
                <textarea id="rejectReason" rows="4" [(ngModel)]="rejectReason" class="mc-textarea" placeholder="Ex : solde insuffisant, incohérence de compte..."></textarea>
                <div class="mc-button-row">
                  <button type="button" class="mc-btn bg-rose-600 text-white hover:bg-rose-700" (click)="rejeterDemande()">Confirmer le rejet</button>
                  <button type="button" class="mc-btn mc-btn-dark" (click)="showRejectForm = false">Annuler</button>
                </div>
              </div>
            </div>

            <ng-template #controllerActionsUnavailable>
              <div class="mc-state mc-state-info">
                {{ controllerDecisionMessage }}
              </div>
            </ng-template>
          </section>

          <section *ngIf="isCaissier && canDisburse()" class="mc-panel space-y-4">
            <h2 class="mb-0 text-lg font-bold text-slate-900">Paiement caissier</h2>
            <p class="text-sm leading-6 text-slate-600">Cette demande est validée. Payez le montant demandé au membre; le compte sera débité du total avec commission.</p>
            <button type="button" class="mc-btn mc-btn-success" (click)="decaisserRetrait()">
              Effectuer le paiement · {{ formatCdf(demande.montantDemande) }} payé · {{ formatCdf(montantTotalDebite) }} débité
            </button>
          </section>

          <section *ngIf="(isMembre || isAdmin) && canCancel()" class="mc-panel space-y-4">
            <h2 class="mb-0 text-lg font-bold text-slate-900">Annulation</h2>
            <p class="text-sm leading-6 text-slate-600">Cette action est irréversible si elle est confirmée.</p>
            <button type="button" class="mc-btn bg-rose-600 text-white hover:bg-rose-700" (click)="annulerDemande()">Annuler cette demande</button>
          </section>
        </aside>
      </main>
    </div>

    <ng-template #loadingState>
      <div class="mc-page-wide">
      <div class="mc-state mc-state-info text-center">
        <span>Chargement de la demande...</span>
      </div>
      </div>
    </ng-template>
  `,
  styles: [`
    .withdrawal-detail-page {
      min-height: calc(100vh - 3.5rem);
      padding: 24px;
      background: #f6f8fb;
      color: #172033;
    }

    .detail-hero,
    .detail-layout,
    .feedback {
      max-width: 1200px;
      margin-left: auto;
      margin-right: auto;
    }

    .detail-hero {
      margin-bottom: 24px;
    }

    .back-button,
    .btn-approve,
    .btn-reject,
    .btn-muted,
    .btn-pay,
    .btn-cancel {
      border: 0;
      border-radius: 10px;
      font-weight: 800;
      letter-spacing: 0;
      transition: transform 160ms ease, box-shadow 160ms ease, background 160ms ease;
    }

    .back-button {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      min-height: 42px;
      margin-bottom: 16px;
      padding: 0 14px;
      background: #ffffff;
      color: #334155;
      border: 1px solid #dbe4ee;
      box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
    }

    button:hover:not(:disabled) {
      transform: translateY(-1px);
    }

    .hero-content {
      display: flex;
      justify-content: space-between;
      gap: 24px;
      padding: 24px;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      background: #ffffff;
      box-shadow: 0 10px 30px rgba(15, 23, 42, 0.07);
    }

    .eyebrow {
      margin: 0 0 6px;
      color: #0f766e;
      font-size: 0.78rem;
      font-weight: 900;
      text-transform: uppercase;
    }

    h1,
    h2,
    p {
      letter-spacing: 0;
    }

    h1 {
      margin: 0;
      color: #0f172a;
      font-size: clamp(1.6rem, 2.4vw, 2.2rem);
      font-weight: 900;
      line-height: 1.15;
    }

    .subtitle {
      margin: 8px 0 0;
      color: #64748b;
      font-size: 0.98rem;
    }

    .hero-badges {
      display: flex;
      flex-wrap: wrap;
      justify-content: flex-end;
      align-content: flex-start;
      gap: 10px;
    }

    .status-badge,
    .workflow-badge,
    .panel-heading span {
      display: inline-flex;
      align-items: center;
      min-height: 30px;
      padding: 0 10px;
      border-radius: 999px;
      font-size: 0.78rem;
      font-weight: 900;
      white-space: nowrap;
    }

    .workflow-badge {
      background: #eef6ff;
      color: #1d4ed8;
      border: 1px solid #bfdbfe;
    }

    .status-created,
    .status-pending {
      background: #fff7ed;
      color: #c2410c;
      border: 1px solid #fed7aa;
    }

    .status-approved {
      background: #eff6ff;
      color: #1d4ed8;
      border: 1px solid #bfdbfe;
    }

    .status-rejected {
      background: #fef2f2;
      color: #b91c1c;
      border: 1px solid #fecaca;
    }

    .status-paid {
      background: #ecfdf5;
      color: #047857;
      border: 1px solid #bbf7d0;
    }

    .status-cancelled {
      background: #f1f5f9;
      color: #475569;
      border: 1px solid #cbd5e1;
    }

    .feedback {
      margin-bottom: 16px;
      padding: 13px 16px;
      border-radius: 12px;
      font-weight: 800;
    }

    .feedback-success {
      background: #ecfdf5;
      color: #047857;
      border: 1px solid #bbf7d0;
    }

    .feedback-error {
      background: #fef2f2;
      color: #b91c1c;
      border: 1px solid #fecaca;
    }

    .detail-layout {
      display: grid;
      grid-template-columns: minmax(0, 1.35fr) minmax(340px, 0.65fr);
      gap: 24px;
      align-items: start;
    }

    .main-column,
    .side-column {
      display: grid;
      gap: 24px;
    }

    .panel {
      padding: 22px;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      background: #ffffff;
      box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
    }

    .panel-heading {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 16px;
      margin-bottom: 18px;
    }

    .panel-heading h2,
    .state-panel h2,
    .cashier-panel h2,
    .cancel-panel h2 {
      margin: 0;
      color: #111827;
      font-size: 1.08rem;
      font-weight: 900;
    }

    .panel-heading span {
      background: #f8fafc;
      color: #64748b;
      border: 1px solid #e2e8f0;
    }

    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 14px;
    }

    .kpi-card,
    .balance-card,
    .workflow-grid article {
      padding: 16px;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      background: #f9fafb;
    }

    .kpi-card span,
    .balance-card span,
    .workflow-grid span,
    .account-identity span {
      display: block;
      margin-bottom: 7px;
      color: #64748b;
      font-size: 0.78rem;
      font-weight: 900;
      text-transform: uppercase;
    }

    .kpi-card strong,
    .balance-card strong {
      color: #0f172a;
      font-size: clamp(1.25rem, 2vw, 1.65rem);
      font-weight: 900;
      line-height: 1.15;
    }

    .kpi-card.primary {
      background: #eff6ff;
      border-color: #bfdbfe;
    }

    .kpi-card.primary strong {
      color: #1d4ed8;
    }

    .kpi-card.success,
    .kpi-card.available,
    .balance-card.green {
      background: #ecfdf5;
      border-color: #bbf7d0;
    }

    .kpi-card.success strong,
    .kpi-card.available strong,
    .balance-card.green strong {
      color: #047857;
    }

    .soft-warning {
      margin-top: 16px;
      padding: 12px 14px;
      border-radius: 10px;
      background: #fffbeb;
      color: #92400e;
      border: 1px solid #fde68a;
      font-weight: 800;
    }

    .account-identity {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 14px;
      margin-bottom: 14px;
    }

    .account-identity div {
      padding: 16px;
      border-radius: 12px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
    }

    .account-identity strong {
      color: #0f172a;
      font-size: 1rem;
      font-weight: 900;
    }

    .balance-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 14px;
    }

    .balance-card.amber {
      background: #fff7ed;
      border-color: #fed7aa;
    }

    .balance-card.amber strong {
      color: #c2410c;
    }

    .balance-card.neutral strong {
      color: #334155;
    }

    .workflow-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 12px;
    }

    .workflow-grid strong {
      color: #0f172a;
      font-size: 0.96rem;
      font-weight: 900;
      line-height: 1.25;
    }

    .trace-list {
      display: grid;
      gap: 0;
      margin: 0;
    }

    .trace-list div {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      padding: 12px 0;
      border-bottom: 1px solid #eef2f7;
    }

    .trace-list div:last-child {
      border-bottom: 0;
    }

    .trace-list dt {
      color: #64748b;
      font-size: 0.84rem;
      font-weight: 900;
    }

    .trace-list dd {
      margin: 0;
      color: #0f172a;
      font-size: 0.88rem;
      font-weight: 800;
      text-align: right;
    }

    .operation-box {
      margin-top: 14px;
      padding: 14px;
      border-radius: 10px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
    }

    .operation-box p {
      display: flex;
      justify-content: space-between;
      margin: 0 0 8px;
      color: #0f172a;
      font-weight: 800;
    }

    .operation-box p:last-child {
      margin-bottom: 0;
    }

    .operation-box span {
      color: #64748b;
    }

    .state-panel,
    .cashier-panel,
    .cancel-panel {
      display: grid;
      gap: 10px;
    }

    .state-panel p,
    .cashier-panel p,
    .cancel-panel p,
    .decision-panel p {
      margin: 0;
      color: #64748b;
      line-height: 1.5;
    }

    .state-panel.rejected {
      background: #fef2f2;
      border-color: #fecaca;
    }

    .state-panel.validated {
      background: #eff6ff;
      border-color: #bfdbfe;
    }

    .state-panel.paid {
      background: #ecfdf5;
      border-color: #bbf7d0;
    }

    .decision-panel {
      display: grid;
      gap: 14px;
    }

    .decision-actions,
    .reject-form,
    .reject-actions {
      display: grid;
      gap: 10px;
    }

    .btn-approve,
    .btn-reject,
    .btn-pay,
    .btn-cancel,
    .btn-muted {
      min-height: 46px;
      padding: 0 16px;
    }

    .btn-approve {
      background: #047857;
      color: #ffffff;
      box-shadow: 0 8px 18px rgba(4, 120, 87, 0.18);
    }

    .btn-reject,
    .btn-cancel {
      background: #dc2626;
      color: #ffffff;
      box-shadow: 0 8px 18px rgba(220, 38, 38, 0.16);
    }

    .btn-pay {
      background: #1d4ed8;
      color: #ffffff;
      box-shadow: 0 8px 18px rgba(29, 78, 216, 0.16);
    }

    .btn-muted {
      background: #e2e8f0;
      color: #334155;
    }

    .decision-disabled {
      padding: 12px 14px;
      border-radius: 10px;
      background: #f8fafc;
      color: #64748b;
      border: 1px solid #e2e8f0;
      font-weight: 800;
    }

    .reject-form {
      margin-top: 2px;
      padding: 14px;
      border-radius: 12px;
      background: #fff7ed;
      border: 1px solid #fed7aa;
    }

    .reject-form label {
      color: #7c2d12;
      font-size: 0.86rem;
      font-weight: 900;
    }

    .reject-form textarea {
      width: 100%;
      border: 1px solid #fdba74;
      border-radius: 10px;
      padding: 12px;
      resize: vertical;
      outline: none;
    }

    .reject-form textarea:focus {
      border-color: #ea580c;
      box-shadow: 0 0 0 4px rgba(234, 88, 12, 0.12);
    }

    .loading-state {
      min-height: 360px;
      display: grid;
      place-items: center;
      gap: 12px;
      color: #64748b;
      font-weight: 800;
    }

    @media (max-width: 1024px) {
      .detail-layout,
      .balance-grid {
        grid-template-columns: 1fr;
      }
    }

    @media (max-width: 720px) {
      .withdrawal-detail-page {
        padding: 16px 12px 28px;
      }

      .hero-content,
      .panel-heading,
      .trace-list div {
        flex-direction: column;
      }

      .hero-badges {
        justify-content: flex-start;
      }

      .kpi-grid,
      .account-identity,
      .workflow-grid {
        grid-template-columns: 1fr;
      }

      .trace-list dd {
        text-align: left;
      }
    }
  `]
})
export class DemandeRetraitEpargneDetailComponent implements OnInit {

  private service = inject(DemandeRetraitEpargneService);
  private compteService = inject(CompteEpargneService);
  private authService = inject(AuthService);
  private workflowMessageService = inject(WorkflowMessageService);
  private ticketRecuService = inject(TicketRecuService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  demande: DemandeRetraitEpargneResponse | null = null;
  soldeDisponible = 0;
  soldeBloque = 0;

  showRejectForm = false;
  rejectReason = '';
  validationMessage = '';
  validationMessageType: 'success' | 'error' = 'success';
  guidance: WorkflowGuidance | null = null;
  ticketRecu: TicketRecuResponse | null = null;
  ticketLoading = false;
  private currentUserRole?: string;
  private currentUserPermissions?: string[];

  isControleur = false;
  isCaissier = false;
  isMembre = false;
  isAdmin = false;

  ngOnInit(): void {
    this.checkRoles();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadDemande(Number(id));
    }
  }

  checkRoles(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      const role = user.role.toUpperCase();
      this.currentUserRole = user.role;
      this.currentUserPermissions = user.permissions || [];
      this.isControleur = role.includes('CONTROLEUR');
      this.isCaissier = role.includes('CAISSIER');
      this.isMembre = role.includes('MEMBER');
      this.isAdmin = role.includes('ADMIN');
    }
  }

  loadDemande(id: number): void {
    this.service.getById(id).subscribe({
      next: (data: DemandeRetraitEpargneResponse) => {
        this.demande = data;
        this.soldeDisponible = data.soldeDisponible ?? this.soldeDisponible;
        this.soldeBloque = data.soldeBloque ?? this.soldeBloque;
        this.refreshGuidance();
        this.chargerTicketsRetrait(data);
        if (data.compteEpargneId && (data.soldeDisponible == null || data.soldeBloque == null)) {
          this.loadCompteInfo(data.compteEpargneId);
        }
      },
      error: (err: any) => {
        console.error('Erreur lors du chargement', err);
        alert('Erreur: Demande non trouvée');
        this.goBack();
      }
    });
  }

  loadCompteInfo(compteId: number): void {
    if (!compteId) {
      return;
    }

    this.compteService.getById(compteId).subscribe({
      next: (compte: any) => {
        this.soldeDisponible = compte.soldeDisponible;
        this.soldeBloque = compte.soldeBloque;
        this.refreshGuidance();
      },
      error: (err: any) => console.error('Erreur chargement compte', err)
    });
  }

  private refreshGuidance(): void {
    if (!this.demande) {
      return;
    }

    this.guidance = this.workflowMessageService.getGuidance({
      module: 'RETRAIT_EPARGNE',
      status: this.resolveGuidanceStatus(this.demande.statut),
      currentRole: this.currentUserRole,
      permissions: this.currentUserPermissions,
      metadata: {
        motifRejet: this.demande.motifRejet,
        montantDemande: this.demande.montantDemande,
        soldeDisponible: this.soldeDisponible,
        soldeBloque: this.soldeBloque,
        montantTotalDebite: this.montantTotalDebite
      }
    });
  }

  private resolveGuidanceStatus(status?: string): string {
    return status || 'DEMANDE';
  }

  get montantRemisAuMembre(): number {
    return this.demande?.montantRemisAuMembre ?? this.demande?.montantDemande ?? 0;
  }

  get tauxCommissionRetrait(): number {
    return Number(this.demande?.tauxCommissionRetrait ?? 0);
  }

  get montantTotalDebite(): number {
    if (!this.demande) {
      return 0;
    }
    return Number(this.demande.montantTotalDebite ?? ((this.demande.montantDemande || 0) + (this.demande.fraisRetrait || 0)));
  }

  get referenceRetrait(): string {
    if (!this.demande) {
      return '';
    }
    if (this.demande.referenceRetrait) {
      return this.demande.referenceRetrait;
    }
    const date = this.demande.dateDemande || this.demande.createdAt;
    const year = date ? new Date(date).getFullYear() : new Date().getFullYear();
    return `RET-${year}-${String(this.demande.id).padStart(4, '0')}`;
  }

  get montantMaximumRetirable(): number {
    return this.soldeDisponible;
  }

  get workflowBadge(): string {
    switch (this.demande?.statut) {
      case StatutDemandeRetrait.CREEE:
      case StatutDemandeRetrait.EN_ATTENTE_VALIDATION:
        return 'Contrôle requis';
      case StatutDemandeRetrait.VALIDEE:
        return 'Paiement requis';
      case StatutDemandeRetrait.DECAISSEE:
        return 'Workflow terminé';
      case StatutDemandeRetrait.REJETEE:
        return 'Workflow rejeté';
      case StatutDemandeRetrait.ANNULEE:
        return 'Workflow annulé';
      default:
        return 'Suivi workflow';
    }
  }

  get currentStepLabel(): string {
    switch (this.demande?.statut) {
      case StatutDemandeRetrait.CREEE:
      case StatutDemandeRetrait.EN_ATTENTE_VALIDATION:
        return 'Validation contrôleur';
      case StatutDemandeRetrait.VALIDEE:
        return 'Paiement caissier';
      case StatutDemandeRetrait.DECAISSEE:
        return 'Retrait payé';
      case StatutDemandeRetrait.REJETEE:
        return 'Demande rejetée';
      case StatutDemandeRetrait.ANNULEE:
        return 'Demande annulée';
      default:
        return 'Non renseigné';
    }
  }

  get expectedRoleLabel(): string {
    switch (this.demande?.statut) {
      case StatutDemandeRetrait.CREEE:
      case StatutDemandeRetrait.EN_ATTENTE_VALIDATION:
        return 'Contrôleur';
      case StatutDemandeRetrait.VALIDEE:
        return 'Caissier';
      case StatutDemandeRetrait.DECAISSEE:
      case StatutDemandeRetrait.REJETEE:
      case StatutDemandeRetrait.ANNULEE:
        return 'Aucun';
      default:
        return 'Non renseigné';
    }
  }

  get nextStepLabel(): string {
    switch (this.demande?.statut) {
      case StatutDemandeRetrait.CREEE:
      case StatutDemandeRetrait.EN_ATTENTE_VALIDATION:
        return 'Validation ou rejet';
      case StatutDemandeRetrait.VALIDEE:
        return 'Décaissement';
      case StatutDemandeRetrait.DECAISSEE:
      case StatutDemandeRetrait.REJETEE:
      case StatutDemandeRetrait.ANNULEE:
        return 'Aucune';
      default:
        return 'Non renseigné';
    }
  }

  get expectedActionLabel(): string {
    switch (this.demande?.statut) {
      case StatutDemandeRetrait.CREEE:
      case StatutDemandeRetrait.EN_ATTENTE_VALIDATION:
        return 'Décider la demande';
      case StatutDemandeRetrait.VALIDEE:
        return 'Effectuer le paiement';
      case StatutDemandeRetrait.DECAISSEE:
        return 'Consulter l’historique';
      case StatutDemandeRetrait.REJETEE:
        return 'Consulter le motif';
      case StatutDemandeRetrait.ANNULEE:
        return 'Consulter l’annulation';
      default:
        return 'Non renseigné';
    }
  }

  get controllerDecisionMessage(): string {
    switch (this.demande?.statut) {
      case StatutDemandeRetrait.VALIDEE:
        return 'Demande déjà validée. Le paiement est attendu côté caissier.';
      case StatutDemandeRetrait.DECAISSEE:
        return 'Demande déjà payée. Aucune décision supplémentaire requise.';
      case StatutDemandeRetrait.REJETEE:
        return 'Demande rejetée. Le motif est affiché dans la traçabilité.';
      case StatutDemandeRetrait.ANNULEE:
        return 'Demande annulée. Aucune décision contrôleur disponible.';
      default:
        return 'Aucune action contrôleur disponible pour votre profil.';
    }
  }

  displayValue(value: string | number | null | undefined): string {
    if (value === null || value === undefined || value === '') {
      return 'Non renseigné';
    }
    return String(value);
  }

  formatCdf(value: number | null | undefined): string {
    const amount = Number(value || 0);
    const formatted = new Intl.NumberFormat('fr-FR', {
      maximumFractionDigits: 0
    }).format(amount).replace(/[\u00a0\u202f]/g, ' ');
    return `${formatted} CDF`;
  }

  formatDate(value: string | Date | null | undefined): string {
    if (!value) {
      return 'Non renseigné';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return 'Non renseigné';
    }

    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date);
  }

  statusClass(statut: StatutDemandeRetrait | string): string {
    switch (statut) {
      case StatutDemandeRetrait.CREEE:
        return 'status-created';
      case StatutDemandeRetrait.EN_ATTENTE_VALIDATION:
        return 'status-pending';
      case StatutDemandeRetrait.VALIDEE:
        return 'status-approved';
      case StatutDemandeRetrait.REJETEE:
        return 'status-rejected';
      case StatutDemandeRetrait.DECAISSEE:
        return 'status-paid';
      case StatutDemandeRetrait.ANNULEE:
        return 'status-cancelled';
      default:
        return 'status-created';
    }
  }

  canValidate(): boolean {
    return this.demande?.statut === StatutDemandeRetrait.CREEE ||
           this.demande?.statut === StatutDemandeRetrait.EN_ATTENTE_VALIDATION;
  }

  canDisburse(): boolean {
    return this.demande?.statut === StatutDemandeRetrait.VALIDEE;
  }

  canCancel(): boolean {
    return this.demande?.statut !== StatutDemandeRetrait.DECAISSEE &&
           this.demande?.statut !== StatutDemandeRetrait.REJETEE;
  }

  canPrintTicket(): boolean {
    return this.authService.hasPermission('TICKET_RECU_PRINT');
  }

  canDuplicataTicket(): boolean {
    return this.authService.hasPermission('TICKET_RECU_DUPLICATA');
  }

  imprimerTicket(): void {
    if (!this.ticketRecu) {
      return;
    }

    this.ticketRecuService.getPrintableHtml(this.ticketRecu.id).subscribe({
      next: html => {
        this.openPrintableHtml(html);
        this.ticketRecuService.marquerImpression(this.ticketRecu!.id).subscribe({
          next: ticket => this.ticketRecu = ticket,
          error: (err: any) => {
            this.validationMessage = 'Erreur impression ticket: ' + (err.error?.message || err.message);
            this.validationMessageType = 'error';
          }
        });
      },
      error: (err: any) => {
        this.validationMessage = 'Erreur préparation ticket: ' + (err.error?.message || err.message);
        this.validationMessageType = 'error';
      }
    });
  }

  private openPrintableHtml(html: string): void {
    const printWindow = window.open('', '_blank', 'noopener,noreferrer');
    if (!printWindow) {
      this.validationMessage = 'Impossible d’ouvrir la fenêtre d’impression.';
      this.validationMessageType = 'error';
      return;
    }
    printWindow.document.open();
    printWindow.document.write(html);
    printWindow.document.close();
    printWindow.focus();
    printWindow.print();
  }

  genererDuplicata(): void {
    if (!this.ticketRecu) {
      return;
    }

    const motif = prompt('Motif du duplicata');
    if (!motif?.trim()) {
      return;
    }

    this.ticketRecuService.genererDuplicata(this.ticketRecu.id, motif.trim()).subscribe({
      next: duplicata => {
        this.ticketRecu = duplicata;
        this.validationMessage = 'Duplicata généré. Vous pouvez l’imprimer.';
        this.validationMessageType = 'success';
      },
      error: (err: any) => {
        this.validationMessage = 'Erreur duplicata ticket: ' + (err.error?.message || err.message);
        this.validationMessageType = 'error';
      }
    });
  }

  private chargerTicketsRetrait(demande: DemandeRetraitEpargneResponse): void {
    this.ticketRecu = null;

    if (demande.statut !== StatutDemandeRetrait.DECAISSEE) {
      return;
    }

    this.ticketLoading = true;
    this.ticketRecuService.getByDemandeRetrait(demande.id).subscribe({
      next: tickets => {
        this.ticketRecu = tickets.find(ticket => ticket.typeTicket !== 'DUPLICATA') ?? tickets[0] ?? null;
        this.ticketLoading = false;
      },
      error: () => {
        this.ticketRecu = null;
        this.ticketLoading = false;
      }
    });
  }

  validerDemande(): void {
    if (!this.demande) return;

    if (this.montantTotalDebite > this.soldeDisponible) {
      alert('Solde insuffisant pour valider ce retrait: le total débité dépasse le solde disponible.');
      return;
    }

    if (confirm(`Valider le retrait de ${this.demande.montantDemande} CDF? Commission: ${this.demande.fraisRetrait || 0} CDF. Total débité: ${this.montantTotalDebite} CDF.`)) {
      this.service.validerDemande(this.demande.id).subscribe({
        next: () => {
          this.validationMessage = '✓ Demande validée avec succès';
          this.validationMessageType = 'success';
          setTimeout(() => this.loadDemande(this.demande!.id), 1500);
        },
        error: (err: any) => {
          this.validationMessage = '❌ Erreur: ' + (err.error?.message || err.message);
          this.validationMessageType = 'error';
        }
      });
    }
  }

  rejeterDemande(): void {
    if (!this.rejectReason.trim()) {
      alert('Veuillez entrer un motif de rejet');
      return;
    }

    if (this.demande && confirm('Êtes-vous sûr de rejeter cette demande?')) {
      this.service.rejeterDemande(this.demande.id, this.rejectReason).subscribe({
        next: () => {
          this.validationMessage = '✓ Demande rejetée';
          this.showRejectForm = false;
          this.rejectReason = '';
          setTimeout(() => this.loadDemande(this.demande!.id), 1500);
        },
        error: (err: any) => {
          this.validationMessage = '❌ Erreur: ' + (err.error?.message || err.message);
          this.validationMessageType = 'error';
        }
      });
    }
  }

  decaisserRetrait(): void {
    if (!this.demande) return;

    if (confirm(`Payer le retrait de ${this.demande.montantDemande} CDF; commission ${this.demande.fraisRetrait || 0} CDF; total débité ${this.montantTotalDebite} CDF.`)) {
      this.service.decaisserRetrait(this.demande.id).subscribe({
        next: () => {
          this.validationMessage = '✓ Paiement effectué avec succès! Les fonds ont été versés.';
          this.validationMessageType = 'success';
          setTimeout(() => this.loadDemande(this.demande!.id), 1500);
        },
        error: (err: any) => {
          this.validationMessage = '❌ Erreur de paiement: ' + (err.error?.message || err.message);
          this.validationMessageType = 'error';
        }
      });
    }
  }

  annulerDemande(): void {
    if (!this.demande) return;

    if (confirm('Êtes-vous sûr d\'annuler cette demande? Cette action est irréversible.')) {
      this.service.annulerDemande(this.demande.id).subscribe({
        next: () => {
          alert('Demande annulée');
          this.goBack();
        },
        error: (err: any) => {
          alert('Erreur: ' + (err.error?.message || err.message));
        }
      });
    }
  }

  getStatutLabel(statut: StatutDemandeRetrait | string): string {
    return STATUT_LABELS[statut as StatutDemandeRetrait] || statut;
  }

  getStatusColor(statut: StatutDemandeRetrait | string): string {
    return STATUT_COLORS[statut as StatutDemandeRetrait] || 'secondary';
  }

  goBack(): void {
    this.router.navigate(['/epargne/demandes-retrait']);
  }
}
