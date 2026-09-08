import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { SiteResponse } from '../../../membres/models/site-response';
import { SiteService } from '../../../membres/services/site.service';
import { TransportSiteParametreRequest, TransportSiteParametreResponse } from '../../models/transport-site-parametre.model';
import { TransportSiteParametreService } from '../../services/transport-site-parametre.service';

@Component({
  selector: 'app-transport-site-parametres',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <main class="mc-page-wide">
      <section class="mc-page-hero flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p class="text-xs font-bold uppercase tracking-wide text-amber-100">Parametres internes</p>
          <h1 class="mc-page-title">Transport terrain par site</h1>
          <p class="mc-page-subtitle">Montant transport journalier par Agent Terrain, multiplié par les agents actifs du site et les jours calendaires de la période.</p>
        </div>
        <strong class="text-2xl font-bold">{{ actifsCount() }} actif(s)</strong>
      </section>

      <div class="mc-state mc-state-danger" *ngIf="error">{{ error }}</div>
      <div class="mc-state mc-state-success" *ngIf="success">{{ success }}</div>

      <section class="mc-panel">
        <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p class="text-xs font-bold uppercase tracking-wide text-amber-700">Configuration</p>
            <h2 class="text-lg font-bold text-slate-900">{{ editingId ? 'Modifier le montant' : 'Nouveau montant site' }}</h2>
          </div>
          <button type="button" class="mc-btn bg-gray-200 text-gray-700 hover:bg-gray-300" *ngIf="editingId" (click)="resetForm()">Annuler</button>
        </div>

        <form class="mc-filter-grid" (ngSubmit)="save()">
          <label>
            <span class="mc-field-label">Site</span>
            <select class="mc-select" name="siteId" [(ngModel)]="form.siteId" required>
              <option [ngValue]="null">Selectionner</option>
              <option *ngFor="let site of sitesActifs" [ngValue]="site.id">{{ site.nomSite }} · {{ site.nomAgence || 'Agence non renseignee' }}</option>
            </select>
          </label>
          <label>
            <span class="mc-field-label">Montant transport journalier par Agent Terrain</span>
            <input class="mc-input" name="montant" type="number" min="0" step="1" [(ngModel)]="form.montantTransportJournalierParAgent" required>
          </label>
          <label>
            <span class="mc-field-label">Debut validite</span>
            <input class="mc-input" name="dateDebut" type="date" [(ngModel)]="form.dateDebutValidite">
          </label>
          <label>
            <span class="mc-field-label">Fin validite</span>
            <input class="mc-input" name="dateFin" type="date" [(ngModel)]="form.dateFinValidite">
          </label>
          <label class="md:col-span-4">
            <span class="mc-state mc-state-warning mb-2">Ce montant sera multiplié par le nombre d’Agents Terrain actifs affectés au site et par le nombre de jours de la période.</span>
            <span class="mc-field-label">Commentaire</span>
            <textarea class="mc-textarea" name="commentaire" [(ngModel)]="form.commentaire" rows="3" required></textarea>
          </label>
          <label class="flex min-h-[42px] items-center gap-2 text-sm font-semibold text-slate-700">
            <input type="checkbox" name="actif" [(ngModel)]="form.actif">
            <span>Actif</span>
          </label>
          <button type="submit" class="mc-btn mc-btn-primary" [disabled]="saving">{{ saving ? 'Enregistrement...' : 'Enregistrer' }}</button>
        </form>
      </section>

      <section class="mc-panel">
        <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p class="text-xs font-bold uppercase tracking-wide text-amber-700">Historique</p>
            <h2 class="text-lg font-bold text-slate-900">Montants transport configures</h2>
          </div>
          <span>{{ parametres.length }} ligne(s)</span>
        </div>
        <div class="mc-table-wrap">
          <table class="mc-table min-w-[900px]">
            <thead>
              <tr>
                <th>Site</th>
                <th>Antenne</th>
                <th>Montant journalier / Agent Terrain</th>
                <th>Validite</th>
                <th>Statut</th>
                <th>Commentaire</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let parametre of parametres">
                <td>{{ parametre.siteNom || '-' }}</td>
                <td>{{ parametre.agenceNom || '-' }}</td>
                <td class="amount strong">{{ formatMoney(parametre.montantTransportJournalierParAgent) }}</td>
                <td>{{ parametre.dateDebutValidite }} → {{ parametre.dateFinValidite || 'en cours' }}</td>
                <td><span class="mc-badge" [class.off]="!parametre.actif">{{ parametre.actif ? 'Actif' : 'Inactif' }}</span></td>
                <td class="comment">{{ parametre.commentaire || '-' }}</td>
                <td class="mc-button-row">
                  <button type="button" class="mc-btn mc-btn-primary px-3 py-1.5 text-xs" (click)="edit(parametre)">Modifier</button>
                  <button type="button" class="mc-btn bg-red-600 px-3 py-1.5 text-xs text-white hover:bg-red-700" *ngIf="parametre.actif" (click)="deactivate(parametre)">Desactiver</button>
                </td>
              </tr>
              <tr *ngIf="parametres.length === 0">
                <td colspan="7" class="text-center text-slate-600">Aucun montant transport configure.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  `,
  styles: [`
    :host { display: block; min-height: 100vh; background: #f4f6f4; color: #17231e; }
    .page { display: grid; gap: 18px; padding: 28px; }
    .hero, .panel { border-radius: 8px; border: 1px solid #dde5df; box-shadow: 0 10px 28px rgba(23, 35, 30, 0.06); }
    .hero { display: flex; justify-content: space-between; gap: 18px; padding: 26px; background: linear-gradient(135deg, #143d2f 0%, #1d5d46 70%, #d5a441 100%); color: #fff; }
    .hero p { margin: 8px 0 0; color: rgba(255,255,255,.84); font-weight: 700; }
    .hero strong { align-self: center; font-size: 1.5rem; }
    .panel { background: #fff; padding: 18px; }
    .panel-heading { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 16px; }
    .eyebrow { margin: 0 0 6px; color: #c9972b; font-size: .74rem; font-weight: 900; text-transform: uppercase; letter-spacing: 0; }
    h1, h2 { margin: 0; letter-spacing: 0; }
    h1 { font-size: clamp(2rem, 4vw, 3rem); }
    h2 { font-size: 1.1rem; }
    .grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; align-items: end; }
    label { display: grid; gap: 6px; color: #4d5b55; font-size: .82rem; font-weight: 850; }
    .full { grid-column: 1 / -1; }
    .helper { color: #76560e; background: #fff6df; border-radius: 6px; padding: 8px 10px; font-weight: 800; }
    .check { display: flex; align-items: center; gap: 8px; min-height: 42px; }
    input, select, textarea { width: 100%; border: 1px solid #cfd9d3; border-radius: 6px; background: #fbfcfb; color: #17231e; font: inherit; font-weight: 650; }
    input, select { min-height: 42px; padding: 0 12px; }
    textarea { padding: 10px 12px; resize: vertical; }
    button { border: 0; border-radius: 6px; min-height: 36px; padding: 0 12px; font-weight: 850; cursor: pointer; }
    .primary { min-height: 42px; background: #c9972b; color: #17231e; }
    .secondary { background: #edf2ef; color: #33453d; }
    .inline { background: #1d5d46; color: #fff; }
    .danger { background: #8b1e10; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; min-width: 900px; border-collapse: collapse; }
    th, td { padding: 12px 10px; border-bottom: 1px solid #edf1ee; text-align: left; white-space: nowrap; }
    th { background: #f8faf8; color: #66746e; font-size: .75rem; font-weight: 900; text-transform: uppercase; }
    td { color: #24312c; font-size: .88rem; font-weight: 650; }
    .amount { text-align: right; font-variant-numeric: tabular-nums; }
    .strong { color: #143d2f; font-weight: 900; }
    .comment { max-width: 300px; white-space: normal; color: #56635d; }
    .actions { display: flex; gap: 8px; }
    .pill { display: inline-flex; align-items: center; min-height: 26px; padding: 0 10px; border-radius: 999px; background: #eef5f1; color: #1d5d46; font-weight: 900; }
    .pill.off { background: #f1f2f1; color: #6f7c76; }
    .error, .success, .empty { padding: 14px; border-radius: 6px; font-weight: 800; }
    .error { background: #ffe8e4; color: #8b1e10; border: 1px solid #ffc5ba; }
    .success { background: #e8f5ed; color: #1d5d46; border: 1px solid #b9dcc8; }
    .empty { text-align: center; color: #7b8782; }
    @media (max-width: 1000px) { .grid { grid-template-columns: 1fr; } .hero { flex-direction: column; } }
  `]
})
export class TransportSiteParametresComponent implements OnInit {
  private readonly service = inject(TransportSiteParametreService);
  private readonly siteService = inject(SiteService);

  sites: SiteResponse[] = [];
  parametres: TransportSiteParametreResponse[] = [];
  loading = false;
  saving = false;
  error = '';
  success = '';
  editingId: number | null = null;

  form: TransportSiteParametreRequest = this.emptyForm();

  get sitesActifs(): SiteResponse[] {
    return this.sites.filter(site => site.actif !== false);
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    forkJoin({ sites: this.siteService.getAll(), parametres: this.service.getAll() }).subscribe({
      next: ({ sites, parametres }) => {
        this.sites = sites ?? [];
        this.parametres = parametres ?? [];
        this.loading = false;
      },
      error: (error) => {
        this.error = error?.error?.message || 'Impossible de charger les paramètres transport.';
        this.loading = false;
      }
    });
  }

  save(): void {
    if (!this.form.siteId || this.form.montantTransportJournalierParAgent == null || !this.form.commentaire.trim()) {
      this.error = 'Site, montant et commentaire sont obligatoires.';
      return;
    }
    this.saving = true;
    this.error = '';
    this.service.save({ ...this.form, commentaire: this.form.commentaire.trim() }).subscribe({
      next: () => {
        this.success = 'Paramètre transport enregistré.';
        this.saving = false;
        this.resetForm();
        this.load();
      },
      error: (error) => {
        this.error = error?.error?.message || 'Impossible d enregistrer ce paramètre transport.';
        this.saving = false;
      }
    });
  }

  edit(parametre: TransportSiteParametreResponse): void {
    this.editingId = parametre.id;
    this.form = {
      siteId: parametre.siteId,
      montantTransportJournalierParAgent: parametre.montantTransportJournalierParAgent,
      actif: parametre.actif,
      dateDebutValidite: parametre.dateDebutValidite,
      dateFinValidite: parametre.dateFinValidite || null,
      commentaire: parametre.commentaire || 'Modification du montant transport site'
    };
  }

  deactivate(parametre: TransportSiteParametreResponse): void {
    const commentaire = prompt('Commentaire de désactivation') || '';
    if (!commentaire.trim()) return;
    this.service.deactivate(parametre.id, commentaire.trim()).subscribe({
      next: () => {
        this.success = 'Paramètre transport désactivé.';
        this.load();
      },
      error: (error) => this.error = error?.error?.message || 'Impossible de désactiver ce paramètre.'
    });
  }

  resetForm(): void {
    this.editingId = null;
    this.form = this.emptyForm();
  }

  actifsCount(): number {
    return this.parametres.filter(parametre => parametre.actif).length;
  }

  formatMoney(value: number | null | undefined): string {
    return new Intl.NumberFormat('fr-CD', { style: 'currency', currency: 'CDF', maximumFractionDigits: 0 }).format(Number(value || 0));
  }

  private emptyForm(): TransportSiteParametreRequest {
    return {
      siteId: null as unknown as number,
      montantTransportJournalierParAgent: 0,
      actif: true,
      dateDebutValidite: this.todayIso(),
      dateFinValidite: null,
      commentaire: ''
    };
  }

  private todayIso(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
