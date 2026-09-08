import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RapportCaisseService } from '../../services/rapport-caisse.service';
import { RapportCaisseDepenses } from '../../models/rapport-caisse.model';

@Component({
  selector: 'app-rapport-caisse-depenses',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rapport-caisse-depenses.component.html'
})
export class RapportCaisseDepensesComponent {
  dateDebut = new Date().toISOString().slice(0, 10);
  dateFin = new Date().toISOString().slice(0, 10);
  caisseId = '';
  siteId = '';
  loading = false;
  error = '';
  rapport: RapportCaisseDepenses | null = null;

  constructor(private rapportService: RapportCaisseService) {}

  rechercher(): void {
    this.loading = true;
    this.error = '';
    this.rapportService.getDepenses(
      this.dateDebut,
      this.dateFin,
      this.parseOpt(this.caisseId),
      this.parseOpt(this.siteId)
    ).subscribe({
      next: (res) => {
        this.rapport = res;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Erreur de chargement du rapport dépenses.';
        this.loading = false;
      }
    });
  }

  reinitialiser(): void {
    const today = new Date().toISOString().slice(0, 10);
    this.dateDebut = today;
    this.dateFin = today;
    this.caisseId = '';
    this.siteId = '';
    this.rapport = null;
    this.error = '';
  }

  exportCsv(): void {
    this.rapportService.exportDepensesCsv(
      this.dateDebut,
      this.dateFin,
      this.parseOpt(this.caisseId),
      this.parseOpt(this.siteId)
    ).subscribe({
      next: (blob) => this.download(blob, `rapport-caisse-depenses-${this.dateDebut}-${this.dateFin}.csv`),
      error: (err) => this.error = err?.error?.message || 'Erreur export CSV.'
    });
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
