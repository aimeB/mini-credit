import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { RapportCaisseService } from '../../services/rapport-caisse.service';
import { RapportCaisseJournalier } from '../../models/rapport-caisse.model';
import { OperationCaisseService } from '../../services/operation-caisse.service';
import { JournalCaisseFilter } from '../../models/journal-caisse-filter';
import { JournalCaisseResponse } from '../../models/journal-caisse-response';
import { SOURCE_OPERATION_CAISSE_LABELS, SourceOperationCaisse } from '../../models/source-operation-caisse';
import { TypeOperationCaisse } from '../../models/type-operation-caisse';
import { Page } from '../../../../shared/models/page.model';

@Component({
  selector: 'app-rapport-caisse-journalier',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './rapport-caisse-journalier.component.html'
})
export class RapportCaisseJournalierComponent {
  date = new Date().toISOString().slice(0, 10);
  caisseId = '';
  siteId = '';
  loading = false;
  loadingTransactions = false;
  error = '';
  transactionsError = '';
  hasSearched = false;

  rapport: RapportCaisseJournalier | null = null;
  transactions: JournalCaisseResponse[] = [];

  constructor(
    private rapportService: RapportCaisseService,
    private operationCaisseService: OperationCaisseService
  ) {}

  rechercher(): void {
    const caisseId = this.parseOpt(this.caisseId);
    const siteId = this.parseOpt(this.siteId);

    this.hasSearched = true;
    this.loading = true;
    this.loadingTransactions = true;
    this.error = '';
    this.transactionsError = '';
    this.rapport = null;
    this.transactions = [];

    this.rapportService.getJournalier(
      this.date,
      caisseId,
      siteId
    ).subscribe({
      next: (res) => {
        this.rapport = res;
        this.loading = false;
        this.chargerTransactions(caisseId, siteId);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Erreur de chargement du rapport journalier.';
        this.loading = false;
        this.loadingTransactions = false;
      }
    });
  }

  reinitialiser(): void {
    this.date = new Date().toISOString().slice(0, 10);
    this.caisseId = '';
    this.siteId = '';
    this.hasSearched = false;
    this.loadingTransactions = false;
    this.rapport = null;
    this.transactions = [];
    this.error = '';
    this.transactionsError = '';
  }

  exportCsv(): void {
    if (!this.rapport) {
      this.error = 'Veuillez d\'abord rechercher le rapport avant export.';
      return;
    }

    const content = this.buildCsvContent();
    const blob = new Blob(['\uFEFF' + content], { type: 'text/csv;charset=utf-8;' });
    this.download(blob, `rapport-caisse-journalier-${this.date}.csv`);
  }

  get totalEntreesTransactions(): number {
    return this.sumByType('ENTREE');
  }

  get totalSortiesTransactions(): number {
    return this.sumByType('SORTIE');
  }

  get ecartEntrees(): number {
    if (!this.rapport) return 0;
    return (this.totalEntreesTransactions || 0) - (this.rapport.totalEntrees || 0);
  }

  get ecartSorties(): number {
    if (!this.rapport) return 0;
    return (this.totalSortiesTransactions || 0) - (this.rapport.totalSorties || 0);
  }

  get soldeDisponibleControle(): number {
    return this.rapport?.soldePhysiqueTotal ?? this.rapport?.soldeTheoriqueTotal ?? 0;
  }

  get soldeExact(): boolean {
    if (!this.rapport) return false;
    return this.isZero(this.rapport.ecartTotal) && this.isZero(this.ecartEntrees) && this.isZero(this.ecartSorties);
  }

  get continuiteCaisseAssuree(): boolean {
    return !!this.rapport && this.rapport.nombreSessions > 0 && this.rapport.nombreSessionsNonCloturees === 0;
  }

  get tracabiliteComplete(): boolean {
    return this.transactions.length > 0 && this.transactions.every((tx) =>
      !!tx.dateOperation
      && tx.sessionCaisseId != null
      && tx.utilisateurId != null
      && !!tx.typeOperation
      && !!tx.source
      && tx.soldeApresOperation != null
    );
  }

  get controleInterneSousAlerte(): boolean {
    return !!this.rapport && (
      this.rapport.nombreSessionsNonCloturees > 0
      || this.rapport.nombreEcarts > 0
      || !this.soldeExact
      || (this.hasSearched && !this.loadingTransactions && !this.tracabiliteComplete)
    );
  }

  get caissiersResponsables(): string {
    const noms = Array.from(new Set(
      this.transactions
        .filter((tx) => tx.roleUtilisateur === 'CAISSIER')
        .map((tx) => tx.utilisateurNom || (tx.utilisateurId != null ? `Utilisateur #${tx.utilisateurId}` : ''))
        .filter((nom) => !!nom)
    ));

    return noms.length > 0 ? noms.join(', ') : '-';
  }

  get journalQueryParams(): Record<string, string> {
    const params: Record<string, string> = {
      dateDebut: this.date,
      dateFin: this.date
    };

    const caisseId = this.parseOpt(this.caisseId);
    const siteId = this.parseOpt(this.siteId);

    if (caisseId != null) params['caisseId'] = String(caisseId);
    if (siteId != null) params['siteId'] = String(siteId);

    return params;
  }

  getReferenceLabel(operation: JournalCaisseResponse): string {
    if (operation.referenceMetier) return operation.referenceMetier;
    if (operation.recetteId) return `RECETTE-${operation.recetteId}`;
    if (operation.depenseCaisseId) return `DEPENSE-${operation.depenseCaisseId}`;
    if (operation.creditId) return `CREDIT-${operation.creditId}`;
    if (operation.retraitEpargneId) return `RETRAIT-${operation.retraitEpargneId}`;
    if (operation.operationEpargneId) return `OP_EPARGNE-${operation.operationEpargneId}`;
    return '';
  }

  getSourceLabel(operation: JournalCaisseResponse): string {
    if (!operation.source) return '';
    return SOURCE_OPERATION_CAISSE_LABELS[operation.source as SourceOperationCaisse] ?? operation.source;
  }

  formatCdf(value: number | null | undefined): string {
    if (value == null) return '0 CDF';
    return new Intl.NumberFormat('fr-CD').format(value) + ' CDF';
  }

  formatDate(dateIso: string | null | undefined): string {
    if (!dateIso) return '-';
    const d = new Date(dateIso);
    if (Number.isNaN(d.getTime())) return '-';
    return d.toLocaleDateString('fr-FR');
  }

  formatTime(dateIso: string | null | undefined): string {
    if (!dateIso) return '-';
    const d = new Date(dateIso);
    if (Number.isNaN(d.getTime())) return '-';
    return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }

  private chargerTransactions(caisseId?: number, siteId?: number): void {
    const filters: JournalCaisseFilter = {
      dateDebut: this.date,
      dateFin: this.date,
      caisseId,
      siteId
    };

    this.operationCaisseService.getJournalCaisse(filters, 0, 2000, 'dateOperation,asc').subscribe({
      next: (page: Page<JournalCaisseResponse>) => {
        this.transactions = page?.content ?? [];
        this.loadingTransactions = false;
      },
      error: (err) => {
        this.transactions = [];
        this.transactionsError = err?.error?.message || 'Erreur de chargement du détail des transactions.';
        this.loadingTransactions = false;
      }
    });
  }

  private sumByType(type: TypeOperationCaisse): number {
    return this.transactions
      .filter((tx) => tx.typeOperation === type)
      .reduce((sum, tx) => sum + Number(tx.montant ?? 0), 0);
  }

  private buildCsvContent(): string {
    const lines: string[] = [];

    lines.push('Section;Valeur');
    lines.push(`Date;${this.escapeCsv(this.rapport?.date ?? this.date)}`);
    lines.push(`Sessions;${this.rapport?.nombreSessions ?? 0}`);
    lines.push(`Sessions non cloturees;${this.rapport?.nombreSessionsNonCloturees ?? 0}`);
    lines.push(`Total entrees;${this.rapport?.totalEntrees ?? 0}`);
    lines.push(`Total sorties;${this.rapport?.totalSorties ?? 0}`);
    lines.push(`Solde theorique total;${this.rapport?.soldeTheoriqueTotal ?? 0}`);
    lines.push(`Solde physique total;${this.rapport?.soldePhysiqueTotal ?? 0}`);
    lines.push(`Ecart total;${this.rapport?.ecartTotal ?? 0}`);
    lines.push(`Nombre depenses;${this.rapport?.nombreDepenses ?? 0}`);
    lines.push(`Montant depenses;${this.rapport?.montantDepenses ?? 0}`);
    lines.push(`Nombre ecarts;${this.rapport?.nombreEcarts ?? 0}`);
    lines.push(`Montant ecarts;${this.rapport?.montantEcarts ?? 0}`);
    lines.push('Total entrees (detail transactions);' + this.totalEntreesTransactions);
    lines.push('Total sorties (detail transactions);' + this.totalSortiesTransactions);
    lines.push('Ecart entrees (detail - synthese);' + this.ecartEntrees);
    lines.push('Ecart sorties (detail - synthese);' + this.ecartSorties);
    lines.push('Continuite caisse;' + (this.continuiteCaisseAssuree ? 'OK' : 'ALERTE'));
    lines.push('Tracabilite;' + (this.tracabiliteComplete ? 'OK' : 'ALERTE'));
    lines.push('Controle interne;' + (this.controleInterneSousAlerte ? 'ALERTE' : 'OK'));
    lines.push('Audit;' + (this.transactions.length > 0 ? 'Journal detaille disponible' : 'Aucune transaction detaillee'));
    lines.push('Responsabilite caissier;' + this.escapeCsv(this.caissiersResponsables));
    lines.push('Exactitude solde disponible;' + (this.soldeExact ? 'OK' : 'ALERTE'));
    lines.push('Solde disponible controle;' + this.soldeDisponibleControle);
    lines.push('');

    lines.push('Transactions');
    lines.push(
      [
        'Date',
        'Heure',
        'Caisse',
        'Session',
        'Type',
        'Categorie',
        'Source',
        'Montant',
        'Solde apres',
        'Utilisateur',
        'Role',
        'Site',
        'Reference metier',
        'Commentaire'
      ].join(';')
    );

    this.transactions.forEach((tx) => {
      lines.push(
        [
          this.escapeCsv(this.formatDate(tx.dateOperation)),
          this.escapeCsv(this.formatTime(tx.dateOperation)),
          this.escapeCsv(tx.caisseLibelle || (tx.caisseId != null ? `Caisse #${tx.caisseId}` : '-')),
          this.escapeCsv(tx.sessionCaisseId != null ? `Session #${tx.sessionCaisseId}` : '-'),
          this.escapeCsv(tx.typeOperation || '-'),
          this.escapeCsv(tx.categorie || '-'),
          this.escapeCsv(this.getSourceLabel(tx) || '-'),
          this.escapeCsv(String(tx.montant ?? 0)),
          this.escapeCsv(String(tx.soldeApresOperation ?? 0)),
          this.escapeCsv(tx.utilisateurNom || (tx.utilisateurId != null ? `Utilisateur #${tx.utilisateurId}` : '-')),
          this.escapeCsv(tx.roleUtilisateur || '-'),
          this.escapeCsv(tx.siteLibelle || (tx.siteId != null ? `Site #${tx.siteId}` : '-')),
          this.escapeCsv(this.getReferenceLabel(tx) || '-'),
          this.escapeCsv(tx.commentaire || '-')
        ].join(';')
      );
    });

    return lines.join('\n');
  }

  private escapeCsv(value: string): string {
    const escaped = value.replace(/"/g, '""');
    return `"${escaped}"`;
  }

  private isZero(value: number | null | undefined): boolean {
    return Math.abs(Number(value ?? 0)) < 0.01;
  }

  private parseOpt(value: string): number | undefined {
    const n = Number(value);
    return Number.isFinite(n) && n > 0 ? n : undefined;
  }

  private download(blob: Blob, fileName: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
