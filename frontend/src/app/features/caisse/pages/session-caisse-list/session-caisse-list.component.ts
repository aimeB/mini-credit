import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SessionCaisseResponse } from '../../models/session-caisse-response';
import { SessionCaisseService } from '../../services/session-caisse.service';

@Component({
  selector: 'app-session-caisse-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './session-caisse-list.component.html'
})
export class SessionCaisseListComponent implements OnInit {
  private readonly sessionCaisseService = inject(SessionCaisseService);

  sessions: SessionCaisseResponse[] = [];
  loading = false;
  error = '';

  ngOnInit(): void {
    this.loadSessions();
  }

  loadSessions(): void {
    this.loading = true;
    this.error = '';

    this.sessionCaisseService.getAll().subscribe({
      next: (sessions) => {
        this.sessions = [...sessions].sort((a, b) => this.sessionSortValue(b) - this.sessionSortValue(a));
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Impossible de charger les sessions de caisse.';
        this.sessions = [];
        this.loading = false;
      }
    });
  }

  getSoldeCloture(session: SessionCaisseResponse): number | null {
    return session.soldePhysique ?? session.soldeTheorique ?? null;
  }

  getEcart(session: SessionCaisseResponse): number | null {
    return session.ecartCaisse ?? session.ecart ?? null;
  }

  getSessionUser(session: SessionCaisseResponse): string {
    return session.utilisateurNom || session.caissierResponsableNom || '-';
  }

  getStatusClass(statut: string | undefined): string {
    if (statut === 'CLOTUREE') return 'bg-slate-200 text-slate-700';
    if (statut === 'VALIDEE_CONTROLE') return 'bg-indigo-100 text-indigo-700';
    if (statut === 'PRE_CLOTUREE') return 'bg-amber-100 text-amber-700';
    if (statut === 'OUVERTE') return 'bg-emerald-100 text-emerald-700';
    return 'bg-gray-100 text-gray-600';
  }

  formatCdf(value: number | null | undefined): string {
    if (value == null) return '-';
    return new Intl.NumberFormat('fr-CD').format(value) + ' CDF';
  }

  trackBySessionId(_: number, session: SessionCaisseResponse): number {
    return session.id;
  }

  private sessionSortValue(session: SessionCaisseResponse): number {
    const value = session.dateComptable || session.dateCloture || session.dateOuverture || '';
    return value ? new Date(value).getTime() : 0;
  }
}
