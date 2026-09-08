import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DemandeRetraitEpargneService } from '../../services/demande-retrait-epargne.service';
import { CompteEpargneService } from '../../services/compte-epargne.service';
import { CompteEpargneResponse } from '../../models/compte-epargne-response';
import { Page } from '../../../../shared/models/page.model';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-demande-retrait-epargne-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="mc-page-wide">
      <header class="mc-page-hero">
        <button type="button" class="mc-btn bg-white/10 text-white ring-1 ring-white/20 hover:bg-white/20" (click)="goBack()" [disabled]="isSubmitting">
          <span aria-hidden="true">←</span>
          Retour
        </button>
        <div>
          <h1 class="mc-page-title">Nouvelle demande de retrait épargne</h1>
          <p class="mc-page-subtitle">Le caissier enregistre la demande. Le contrôleur valide avant paiement.</p>
        </div>
      </header>

      <main class="grid gap-6 lg:grid-cols-[minmax(0,1fr)_380px]">
        <section class="mc-panel space-y-5">
          <div>
            <h2 class="mb-1 text-lg font-bold text-slate-900">Informations du retrait</h2>
            <p class="text-sm leading-6 text-slate-600">Saisissez le compte, le montant demandé et l'observation. La commission est calculée automatiquement.</p>
          </div>

          <form [formGroup]="form" (ngSubmit)="onSubmit()" class="grid gap-4 md:grid-cols-2">
            <div *ngIf="errorMessage" class="mc-state mc-state-danger md:col-span-2" role="alert">
              {{ errorMessage }}
            </div>

            <div class="mc-field-label md:col-span-2">
              <label for="compte">Compte épargne <span>*</span></label>
              <select id="compte" class="mc-select" formControlName="compteEpargneId" (change)="onCompteChange()">
                <option value="" selected>Choisir un compte épargne</option>
                <option *ngFor="let compte of comptes" [value]="compte.id">
                  {{ compte.numeroCompte }} - {{ compte.membreNomComplet }} ({{ compte.soldeDisponible | currency: 'CDF' }})
                </option>
              </select>
              <p class="text-xs text-slate-500">Sélectionnez le compte du membre présent au guichet.</p>
            </div>

            <div class="mc-field-label">
              <label for="montant">Montant demandé <span>*</span></label>
              <div class="flex rounded-lg border border-slate-300 bg-white shadow-sm focus-within:border-blue-500 focus-within:ring-2 focus-within:ring-blue-100">
                <input type="number" id="montant" class="w-full rounded-l-lg border-0 px-3 py-2 text-sm outline-none"
                       formControlName="montantDemande"
                       placeholder="Ex : 100 000"
                       step="1" min="1000">
                <span class="inline-flex items-center rounded-r-lg bg-slate-100 px-3 text-sm font-semibold text-slate-600">CDF</span>
              </div>
              <p class="text-xs text-slate-500">Montant qui sera remis au membre après validation et paiement.</p>
              <div class="text-sm text-red-700" *ngIf="form.get('montantDemande')?.hasError('required') && form.get('montantDemande')?.touched">
                Le montant est requis.
              </div>
              <div class="text-sm text-red-700" *ngIf="form.get('montantDemande')?.hasError('min')">
                Le montant minimum de retrait est de 1 000 CDF.
              </div>
            </div>

            <div class="mc-field-label">
              <label>Commission retrait</label>
              <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700">
                <div class="flex justify-between gap-3"><span>Taux</span><strong>{{ commissionEstimate.taux | number: '1.2-2' }} %</strong></div>
                <div class="flex justify-between gap-3"><span>Commission</span><strong>{{ commissionEstimate.commission | currency: 'CDF' }}</strong></div>
              </div>
              <p class="text-xs text-slate-500">Estimation frontend; le backend recalcule le montant définitif.</p>
            </div>

            <div *ngIf="selectedCompte && montantValue > 0" class="rounded-2xl border border-blue-200 bg-blue-50 p-4 md:col-span-2">
              <div class="mb-2 flex items-center justify-between text-sm font-semibold text-blue-900">
                <span>Utilisation du solde disponible</span>
                <strong>{{ retraitPercent.toFixed(0) }}%</strong>
              </div>
              <div class="h-2 overflow-hidden rounded-full bg-blue-100">
                <div class="h-full rounded-full bg-blue-600" [style.width.%]="retraitPercent"></div>
              </div>
            </div>

            <div class="mc-field-label md:col-span-2">
              <label for="observation">Observation</label>
              <textarea id="observation" class="mc-textarea" rows="4"
                        formControlName="observation"
                        placeholder="Ex : besoin urgent, remboursement dette, dépense familiale..."></textarea>
              <p class="text-xs text-slate-500">Facultatif, mais utile pour faciliter le contrôle.</p>
            </div>

            <div class="mc-button-row md:col-span-2">
              <button type="button" class="mc-btn mc-btn-dark" (click)="goBack()" [disabled]="isSubmitting">
                Annuler
              </button>
              <button type="submit" class="mc-btn mc-btn-success" [disabled]="!form.valid || isSubmitting || !canSubmitRetrait">
                {{ isSubmitting ? 'Création...' : 'Créer la demande' }}
              </button>
            </div>
          </form>
        </section>

        <aside class="space-y-6">
          <section class="mc-panel space-y-4" [class.empty]="!selectedCompte">
            <div class="flex items-start justify-between gap-3">
              <div>
                <h2 class="mb-1 text-lg font-bold text-slate-900">Compte sélectionné</h2>
              </div>
              <span class="mc-badge bg-emerald-100 text-emerald-800">Actif</span>
            </div>

            <ng-container *ngIf="selectedCompte; else noAccountSelected">
              <div class="grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-1">
                <div>
                  <span class="block text-slate-500">N° compte</span>
                  <strong class="text-slate-900">{{ selectedCompte.numeroCompte }}</strong>
                </div>
                <div>
                  <span class="block text-slate-500">Membre</span>
                  <strong class="text-slate-900">{{ selectedCompte.membreNomComplet }}</strong>
                </div>
              </div>

              <div class="grid gap-3">
                <div class="mc-kpi-card mc-kpi-card-success">
                  <span class="mc-kpi-label text-emerald-700">Solde disponible</span>
                  <strong class="mc-kpi-value block text-emerald-900">{{ selectedCompte.soldeDisponible | currency: 'CDF' }}</strong>
                </div>
                <div class="mc-kpi-card mc-kpi-card-warning">
                  <span class="mc-kpi-label text-amber-700">Solde bloqué</span>
                  <strong class="mc-kpi-value block text-amber-900">{{ selectedCompte.soldeBloque | currency: 'CDF' }}</strong>
                </div>
              </div>

              <div class="mc-state mc-state-info">
                <span class="block font-semibold">Montant que vous pouvez retirer</span>
                <strong>{{ montantRetirable | currency: 'CDF' }}</strong>
              </div>
            </ng-container>

            <ng-template #noAccountSelected>
              <p class="mc-state mc-state-info">Choisissez un compte pour afficher les soldes et le montant retirable.</p>
            </ng-template>
          </section>

          <section class="mc-panel space-y-4">
            <h2 class="mb-0 text-lg font-bold text-slate-900">Résumé de la demande</h2>
            <div class="space-y-3 text-sm">
              <div>
                <span class="text-slate-500">Montant demandé</span>
                <strong class="block text-slate-900">{{ montantValue | currency: 'CDF' }}</strong>
              </div>
              <div>
                <span class="text-slate-500">Taux commission</span>
                <strong class="block text-emerald-700">{{ commissionEstimate.taux | number: '1.2-2' }} %</strong>
              </div>
              <div>
                <span class="text-slate-500">Commission retrait</span>
                <strong class="block text-emerald-700">{{ commissionEstimate.commission | currency: 'CDF' }}</strong>
              </div>
              <div>
                <span class="text-slate-500">Total débité du compte épargne</span>
                <strong class="block text-red-700">{{ commissionEstimate.totalDebite | currency: 'CDF' }}</strong>
              </div>
              <div class="rounded-2xl border border-slate-200 bg-slate-50 p-3">
                <span class="text-slate-500">Montant remis au membre</span>
                <strong class="block text-slate-900">{{ montantValue | currency: 'CDF' }}</strong>
              </div>
            </div>
            <p class="text-sm leading-6 text-slate-600">Le montant demandé est payé au membre. Le compte épargne est débité du montant demandé plus la commission.</p>
          </section>

          <section class="mc-panel space-y-4">
            <h2 class="mb-0 text-lg font-bold text-slate-900">Étapes du retrait</h2>
            <ol class="space-y-2 text-sm text-slate-700">
              <li><span class="mc-badge mr-2 bg-slate-100 text-slate-700">1</span>Demande créée par le caissier</li>
              <li><span class="mc-badge mr-2 bg-slate-100 text-slate-700">2</span>Validation par le contrôleur</li>
              <li><span class="mc-badge mr-2 bg-slate-100 text-slate-700">3</span>Paiement par le caissier</li>
            </ol>
          </section>
        </aside>
      </main>
    </div>
  `,
  styles: [`
    .withdrawal-page {
      min-height: calc(100vh - 3.5rem);
      background: #f6f8fb;
      padding: 24px;
      color: #172033;
    }

    .page-header {
      max-width: 1200px;
      margin: 0 auto 24px;
      display: flex;
      align-items: flex-start;
      gap: 16px;
      padding: 0;
      border: 0;
      border-radius: 0;
      background: transparent;
      box-shadow: none;
    }

    .back-button,
    .btn-secondary,
    .btn-primary {
      border: 0;
      border-radius: 10px;
      font-weight: 700;
      transition: transform 160ms ease, box-shadow 160ms ease, background 160ms ease;
    }

    .back-button {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      min-height: 42px;
      padding: 0 14px;
      background: #ffffff;
      color: #334155;
      border: 1px solid #dbe4ee;
      box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
    }

    .back-button:hover:not(:disabled),
    .btn-secondary:hover:not(:disabled),
    .btn-primary:hover:not(:disabled) {
      transform: translateY(-1px);
    }

    h1,
    h2,
    p {
      letter-spacing: 0;
    }

    h1 {
      margin: 0;
      color: #0f172a;
      font-size: clamp(1.65rem, 2.4vw, 2.1rem);
      font-weight: 800;
      line-height: 1.15;
    }

    .subtitle {
      margin: 6px 0 0;
      color: #475569;
      font-size: 1rem;
    }

    .withdrawal-layout {
      max-width: 1200px;
      margin: 0 auto;
      display: grid;
      grid-template-columns: minmax(0, 65fr) minmax(320px, 35fr);
      gap: 24px;
      align-items: start;
    }

    .form-card,
    .account-card,
    .summary-card,
    .workflow-card {
      background: rgba(255, 255, 255, 0.94);
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
    }

    .form-card {
      padding: 24px;
    }

    .section-heading {
      margin-bottom: 24px;
      padding-bottom: 16px;
      border-bottom: 1px solid #e2e8f0;
    }

    .section-heading h2,
    .account-card h2,
    .summary-card h2,
    .workflow-card h2 {
      margin: 0;
      color: #111827;
      font-size: 1.15rem;
      font-weight: 800;
    }

    .section-heading p {
      margin: 3px 0 0;
      color: #64748b;
      font-size: 0.92rem;
    }

    .withdrawal-form {
      display: grid;
      gap: 20px;
    }

    .field-block {
      display: grid;
      gap: 7px;
    }

    label {
      color: #1f2937;
      font-size: 0.92rem;
      font-weight: 800;
    }

    label span {
      color: #dc2626;
    }

    .control {
      width: 100%;
      min-height: 52px;
      border: 1px solid #cbd5e1;
      border-radius: 10px;
      padding: 0 14px;
      background: #ffffff;
      color: #0f172a;
      font-size: 1rem;
      outline: none;
      transition: border-color 160ms ease, box-shadow 160ms ease;
    }

    .control:focus {
      border-color: #0f766e;
      box-shadow: 0 0 0 4px rgba(15, 118, 110, 0.12);
    }

    .control.ng-invalid.ng-touched {
      border-color: #dc2626;
      box-shadow: 0 0 0 4px rgba(220, 38, 38, 0.1);
    }

    .control-select {
      appearance: auto;
    }

    .textarea {
      min-height: 128px;
      padding-top: 12px;
      padding-bottom: 12px;
      resize: vertical;
    }

    .field-help,
    .summary-note,
    .empty-text {
      margin: 0;
      color: #64748b;
      font-size: 0.84rem;
      line-height: 1.45;
    }

    .field-error,
    .message-error {
      color: #b91c1c;
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 10px;
      padding: 9px 11px;
      font-size: 0.87rem;
      font-weight: 700;
    }

    .message {
      margin-bottom: 2px;
    }

    .money-input {
      width: 100%;
      display: flex;
      align-items: stretch;
      border: 1px solid #cbd5e1;
      border-radius: 10px;
      background: #ffffff;
      overflow: hidden;
      box-shadow: none;
    }

    .money-input.secondary {
      border-color: #cbd5e1;
      box-shadow: none;
    }

    .money-input .control {
      flex: 1 1 auto;
      min-width: 0;
      border: 0;
      border-radius: 0;
      min-height: 56px;
      font-size: 1.15rem;
      font-weight: 700;
    }

    .money-input .control:focus {
      box-shadow: none;
    }

    .money-input span {
      display: grid;
      place-items: center;
      flex: 0 0 72px;
      background: #f8fafc;
      color: #475569;
      font-weight: 800;
      border-left: 1px solid #e2e8f0;
    }

    .money-input.secondary span {
      background: #f8fafc;
      color: #475569;
      border-left-color: #e2e8f0;
    }

    .limit-meter {
      padding: 13px;
      border-radius: 12px;
      background: #f9fafb;
      border: 1px solid #e2e8f0;
    }

    .meter-label {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 8px;
      color: #475569;
      font-size: 0.86rem;
      font-weight: 700;
    }

    .meter-track {
      height: 10px;
      border-radius: 999px;
      background: #e2e8f0;
      overflow: hidden;
    }

    .meter-fill {
      height: 100%;
      border-radius: inherit;
      background: linear-gradient(90deg, #0f766e, #16a34a);
    }

    .form-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      padding-top: 4px;
    }

    .btn-secondary,
    .btn-primary {
      min-height: 48px;
      padding: 0 20px;
    }

    .btn-secondary {
      background: #ffffff;
      color: #334155;
      border: 1px solid #cbd5e1;
    }

    .btn-primary {
      background: #166534;
      color: #ffffff;
      box-shadow: 0 8px 18px rgba(22, 101, 52, 0.18);
    }

    .btn-primary:disabled,
    .btn-secondary:disabled,
    .back-button:disabled {
      opacity: 0.72;
      cursor: not-allowed;
      transform: none;
      box-shadow: none;
    }

    .side-panel {
      display: grid;
      gap: 16px;
      position: sticky;
      top: 76px;
    }

    .account-card,
    .summary-card,
    .workflow-card {
      padding: 20px;
    }

    .card-title-row {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 12px;
      margin-bottom: 16px;
    }

    .status-chip {
      padding: 5px 9px;
      border-radius: 999px;
      background: #dcfce7;
      color: #166534;
      font-size: 0.75rem;
      font-weight: 800;
    }

    .account-card.empty .status-chip {
      display: none;
    }

    .account-identity {
      display: grid;
      gap: 10px;
      margin-bottom: 16px;
      padding-bottom: 16px;
      border-bottom: 1px solid #e2e8f0;
    }

    .account-identity span {
      display: block;
      color: #64748b;
      font-size: 0.78rem;
      font-weight: 800;
      margin-bottom: 4px;
    }

    .account-identity strong {
      color: #0f172a;
      font-size: 0.98rem;
      font-weight: 800;
    }

    .account-metrics {
      display: grid;
      grid-template-columns: 1fr;
      gap: 10px;
    }

    .metric {
      padding: 12px;
      border-radius: 10px;
      background: #f9fafb;
      border: 1px solid #e2e8f0;
    }

    .metric span,
    .summary-lines span {
      display: block;
      color: #64748b;
      font-size: 0.78rem;
      font-weight: 800;
      margin-bottom: 5px;
    }

    .metric strong,
    .summary-lines strong {
      color: #0f172a;
      font-size: 1rem;
      font-weight: 800;
    }

    .metric-success {
      background: #f0fdf4;
      border-color: #bbf7d0;
    }

    .metric-success strong {
      color: #15803d;
    }

    .metric-warning {
      background: #fff7ed;
      border-color: #fed7aa;
    }

    .metric-warning strong {
      color: #c2410c;
    }

    .withdrawable-box {
      margin-top: 12px;
      padding: 16px;
      border-radius: 10px;
      background: #ecfdf5;
      color: #ffffff;
    }

    .withdrawable-box span {
      display: block;
      font-size: 0.8rem;
      font-weight: 800;
      color: #047857;
      margin-bottom: 6px;
    }

    .withdrawable-box strong {
      color: #065f46;
      font-size: 1.45rem;
      line-height: 1.1;
    }

    .summary-lines {
      display: grid;
      gap: 9px;
      margin: 14px 0 12px;
    }

    .summary-lines div {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 10px 0;
      border-bottom: 1px solid #e2e8f0;
    }

    .summary-lines .highlight {
      padding: 12px;
      border: 0;
      border-radius: 10px;
      background: #ecfdf5;
    }

    .summary-lines .highlight strong {
      color: #047857;
      font-size: 1.15rem;
    }

    .workflow-steps {
      display: grid;
      gap: 12px;
      margin: 16px 0 0;
      padding: 0;
      list-style: none;
    }

    .workflow-steps li {
      display: flex;
      align-items: center;
      gap: 10px;
      color: #334155;
      font-size: 0.92rem;
      font-weight: 700;
    }

    .workflow-steps span {
      width: 26px;
      height: 26px;
      display: inline-grid;
      place-items: center;
      flex: 0 0 26px;
      border-radius: 999px;
      background: #e0f2fe;
      color: #0369a1;
      font-size: 0.78rem;
      font-weight: 900;
    }

    @media (max-width: 1024px) {
      .withdrawal-layout {
        grid-template-columns: 1fr;
      }

      .side-panel {
        position: static;
        grid-template-columns: 1fr;
      }
    }

    @media (max-width: 720px) {
      .withdrawal-page {
        padding: 16px 12px 28px;
      }

      .page-header {
        align-items: flex-start;
        flex-direction: column;
      }

      .side-panel,
      .account-metrics {
        grid-template-columns: 1fr;
      }

      .form-actions {
        flex-direction: column-reverse;
      }

      .btn-primary,
      .btn-secondary {
        width: 100%;
      }

      .money-input .control {
        font-size: 1.05rem;
      }
    }
  `]
})
export class DemandeRetraitEpargneFormComponent implements OnInit {

  private fb = inject(FormBuilder);
  private demandeService = inject(DemandeRetraitEpargneService);
  private compteService = inject(CompteEpargneService);
  private router = inject(Router);
  private authService = inject(AuthService);

  form: FormGroup;
  comptes: CompteEpargneResponse[] = [];
  selectedCompte: CompteEpargneResponse | null = null;
  errorMessage = '';
  isSubmitting = false;

  constructor() {
    this.form = this.fb.group({
      compteEpargneId: ['', Validators.required],
      montantDemande: ['', [Validators.required, Validators.min(1000)]],
      observation: ['']
    });
  }

  ngOnInit(): void {
    this.loadComptes();
  }

  loadComptes(): void {
    const currentRole = this.normalizeRole(this.authService.getCurrentUser()?.role);
    const request$ = currentRole === 'MEMBER'
      ? this.compteService.getMesComptes()
      : (currentRole === 'CAISSIER' || currentRole === 'ADMIN')
        ? this.compteService.getComptesActifsPourRetraitGuichet()
        : this.compteService.getAll();

    request$.subscribe({
      next: (data: CompteEpargneResponse[] | Page<CompteEpargneResponse>) => {
        const comptes = this.normalizeComptesResponse(data);
        this.comptes = comptes.filter(c => c.soldeDisponible > 0);
        if (this.comptes.length === 0) {
          this.errorMessage = 'Aucun compte épargne avec solde disponible';
        }
      },
      error: (err: any) => {
        console.error('Erreur chargement comptes', err);
        this.errorMessage = 'Erreur lors du chargement des comptes';
      }
    });
  }

  private normalizeComptesResponse(data: CompteEpargneResponse[] | Page<CompteEpargneResponse> | null | undefined): CompteEpargneResponse[] {
    if (Array.isArray(data)) {
      return data;
    }

    if (data && Array.isArray(data.content)) {
      return data.content;
    }

    return [];
  }

  onCompteChange(): void {
    const compteId = this.form.get('compteEpargneId')?.value;
    if (compteId) {
      this.selectedCompte = this.comptes.find(c => c.id === Number(compteId)) || null;

      const montantControl = this.form.get('montantDemande');
      if (montantControl && this.selectedCompte) {
        montantControl.setValidators([
          Validators.required,
          Validators.min(1000)
        ]);
        montantControl.updateValueAndValidity();
      }
    }
  }

  get montantValue(): number {
    const value = this.form.get('montantDemande')?.value;
    return value ? Number(value) : 0;
  }

  get montantRetirable(): number {
    return this.selectedCompte?.soldeDisponible ?? 0;
  }

  get commissionEstimate(): { taux: number; commission: number; totalDebite: number } {
    return this.calculateCdfCommission(this.montantValue);
  }

  get canSubmitRetrait(): boolean {
    if (!this.selectedCompte || this.montantValue < 1000) {
      return false;
    }
    return this.commissionEstimate.totalDebite <= this.selectedCompte.soldeDisponible;
  }

  get retraitPercent(): number {
    if (!this.selectedCompte?.soldeDisponible || this.montantValue <= 0) {
      return 0;
    }
    return Math.min(100, (this.commissionEstimate.totalDebite / this.selectedCompte.soldeDisponible) * 100);
  }

  onSubmit(): void {
    if (!this.form.valid) {
      alert('Veuillez remplir tous les champs requis');
      return;
    }

    const { compteEpargneId, montantDemande, observation } = this.form.value;

    if (!this.selectedCompte) {
      alert('Compte épargne non sélectionné');
      return;
    }

    if (Number(montantDemande) < 1000) {
      alert('Le montant minimum de retrait est de 1 000 CDF.');
      return;
    }

    if (this.commissionEstimate.totalDebite > this.selectedCompte.soldeDisponible) {
      alert(`Total débité ${this.commissionEstimate.totalDebite} CDF supérieur au solde disponible de ${this.selectedCompte.soldeDisponible} CDF`);
      return;
    }

    if (confirm(`Créer une demande de retrait de ${montantDemande} CDF. Commission estimée: ${this.commissionEstimate.commission} CDF. Total débité: ${this.commissionEstimate.totalDebite} CDF.`)) {
      this.isSubmitting = true;
      this.demandeService.creerDemande(compteEpargneId, montantDemande, 0, observation)
        .subscribe({
          next: (result: any) => {
            const reference = result.referenceRetrait || `RET-${new Date().getFullYear()}-${String(result.id).padStart(4, '0')}`;
            alert(`Demande de retrait ${reference} enregistrée. Elle doit être validée par le Contrôleur avant paiement.`);
            this.router.navigate(['/epargne/demandes-retrait', result.id]);
          },
          error: (err: any) => {
            this.isSubmitting = false;
            this.errorMessage = 'Erreur: ' + (err.error?.message || err.message);
            console.error('Erreur création demande', err);
          }
        });
    }
  }

  goBack(): void {
    this.router.navigate(['/epargne/demandes-retrait']);
  }

  private normalizeRole(role: string | null | undefined): string {
    let normalized = (role ?? '').trim().toUpperCase();
    if (normalized.startsWith('ROLE_')) {
      normalized = normalized.slice(5);
    }
    return normalized;
  }

  private calculateCdfCommission(montant: number): { taux: number; commission: number; totalDebite: number } {
    if (!montant || montant < 1000) {
      return { taux: 0, commission: 0, totalDebite: montant || 0 };
    }

    const taux = this.resolveCdfTaux(montant);
    const commission = this.roundMoney((montant * taux) / 100);
    return {
      taux,
      commission,
      totalDebite: this.roundMoney(montant + commission)
    };
  }

  private resolveCdfTaux(montant: number): number {
    if (montant <= 10000) return 9;
    if (montant <= 24000) return 8;
    if (montant <= 34000) return 6;
    if (montant <= 49000) return 6.5;
    if (montant <= 69000) return 5.5;
    if (montant <= 94000) return 5;
    if (montant <= 100000) return 4.5;
    if (montant <= 200000) return 4;
    if (montant <= 400000) return 3.5;
    if (montant <= 600000) return 3;
    if (montant <= 800000) return 2.5;
    if (montant <= 1500000) return 2;
    return 1.4;
  }

  private roundMoney(value: number): number {
    return Math.round((value + Number.EPSILON) * 100) / 100;
  }
}
