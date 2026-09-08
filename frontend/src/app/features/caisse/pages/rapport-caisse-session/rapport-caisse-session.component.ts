import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RapportCaisseService } from '../../services/rapport-caisse.service';
import { RapportCaisseSession } from '../../models/rapport-caisse.model';

@Component({
  selector: 'app-rapport-caisse-session',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rapport-caisse-session.component.html'
})
export class RapportCaisseSessionComponent {
  sessionId = '';
  loading = false;
  error = '';
  rapport: RapportCaisseSession | null = null;

  constructor(private rapportService: RapportCaisseService) {}

  rechercher(): void {
    this.error = '';
    this.rapport = null;
    const id = Number(this.sessionId);
    if (!id || id <= 0) {
      this.error = 'Session ID invalide.';
      return;
    }

    this.loading = true;
    this.rapportService.getSession(id).subscribe({
      next: (res) => {
        this.rapport = res;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Erreur de chargement du rapport session.';
        this.loading = false;
      }
    });
  }

  reinitialiser(): void {
    this.sessionId = '';
    this.error = '';
    this.rapport = null;
  }

  exportCsv(): void {
    const id = Number(this.sessionId);
    if (!id || id <= 0) {
      this.error = 'Session ID invalide.';
      return;
    }

    this.rapportService.exportSessionCsv(id).subscribe({
      next: (blob) => this.download(blob, `rapport-caisse-session-${id}.csv`),
      error: (err) => this.error = err?.error?.message || 'Erreur export CSV.'
    });
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
