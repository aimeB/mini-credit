import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';
import { SessionCaisseResponse } from '../../models/session-caisse-response';
import { StatutSessionCaisse } from '../../models/statut-session-caisse';
import { SessionCaisseService } from '../../services/session-caisse.service';

@Component({
  selector: 'app-caisse-controle',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './caisse-controle.component.html'
})
export class CaisseControleComponent implements OnInit {
  sessionsFermees: SessionCaisseResponse[] = [];
  loading = false;
  error = '';
  noteControle = '';
  validatingSessionId: number | null = null;
  statutFiltre = '';
  controlGuidance: WorkflowGuidance | null = null;

  private readonly statutsVisiblesEnControle: StatutSessionCaisse[] = [
    'PRE_CLOTUREE',
    'VALIDEE_CONTROLE',
    'CLOTUREE'
  ];

  constructor(
    private sessionService: SessionCaisseService,
    private authService: AuthService,
    private route: ActivatedRoute,
    private workflowMessageService: WorkflowMessageService
  ) {}

  ngOnInit(): void {
    this.statutFiltre = this.route.snapshot.queryParamMap.get('statut') || '';
    const currentUser = (this.authService as { getCurrentUser?: () => { role?: string } | null }).getCurrentUser?.();
    this.controlGuidance = this.workflowMessageService.getGuidance({
      module: 'CAISSE',
      status: 'PRE_CLOTUREE',
      currentRole: currentUser?.role,
      expectedRole: 'CONTROLEUR',
      nextStep: 'Validation puis clôture'
    });
    this.loadSessionsFermees();
  }

  loadSessionsFermees(): void {
    this.loading = true;
    this.error = '';

    this.sessionService.getAll().subscribe({
      next: (sessions) => {
        this.sessionsFermees = (sessions ?? [])
          .filter((s) => this.isSessionVisibleEnControle(s.statut))
          .filter((s) => !this.statutFiltre || s.statut === this.statutFiltre)
          .sort((a, b) => this.compareSessionsControle(a, b));
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Impossible de charger les sessions à contrôler.';
        this.loading = false;
      }
    });
  }

  validerControle(session: SessionCaisseResponse): void {
    if (!this.canValiderControle()) {
      return;
    }

    this.validatingSessionId = session.id;

    this.sessionService.validerControle(session.id, {
      observation: this.noteControle?.trim() || undefined
    }).subscribe({
      next: () => {
        this.noteControle = '';
        this.validatingSessionId = null;
        this.loadSessionsFermees();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Erreur lors de la validation de contrôle.';
        this.validatingSessionId = null;
      }
    });
  }

  demanderCorrection(session: SessionCaisseResponse): void {
    this.noteControle = `Correction demandée pour session #${session.id} (${session.caisseCode}) : `;
  }

  formatCdf(value: number | null | undefined): string {
    if (value == null) {
      return '0 CDF';
    }
    return new Intl.NumberFormat('fr-CD').format(value) + ' CDF';
  }

  canValiderControle(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'CONTROLEUR']);
  }

  isSessionClosed(statut: StatutSessionCaisse | string | null | undefined): boolean {
    return statut === 'CLOTUREE';
  }

  private isSessionVisibleEnControle(statut: StatutSessionCaisse | string | null | undefined): boolean {
    return !!statut && this.statutsVisiblesEnControle.includes(statut as StatutSessionCaisse);
  }

  private compareSessionsControle(a: SessionCaisseResponse, b: SessionCaisseResponse): number {
    const priorityA = this.getControlePriority(a.statut);
    const priorityB = this.getControlePriority(b.statut);

    if (priorityA !== priorityB) {
      return priorityA - priorityB;
    }

    const dateA = (a.dateCloture || a.dateOuverture || '').toString();
    const dateB = (b.dateCloture || b.dateOuverture || '').toString();

    return dateB.localeCompare(dateA);
  }

  private getControlePriority(statut: StatutSessionCaisse | string | null | undefined): number {
    if (statut === 'PRE_CLOTUREE') return 0;
    if (statut === 'VALIDEE_CONTROLE') return 1;
    if (statut === 'CLOTUREE') return 2;
    return 99;
  }
}
