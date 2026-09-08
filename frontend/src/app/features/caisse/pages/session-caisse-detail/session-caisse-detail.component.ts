import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { SessionTimelineComponent } from '../../components/session-timeline/session-timeline.component';
import { SOURCE_OPERATION_CAISSE_LABELS, SourceOperationCaisse } from '../../models/source-operation-caisse';
import { SessionAuditEvent } from '../../models/session-audit.model';
import { SessionCaisseAnomalieResponse } from '../../models/session-caisse-anomalie-response';
import { SessionCaisseResponse } from '../../models/session-caisse-response';
import { SessionAuditService } from '../../services/session-audit.service';
import { SessionCaisseService } from '../../services/session-caisse.service';
import { PermissionCode } from '../../../../shared/enums/permission-code.enum';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';

@Component({
  selector: 'app-session-caisse-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, SessionTimelineComponent, WorkflowGuidanceBannerComponent],
  templateUrl: './session-caisse-detail.component.html'
})
export class SessionCaisseDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private sessionCaisseService = inject(SessionCaisseService);
  private sessionAuditService = inject(SessionAuditService);
  private authService = inject(AuthService);
  private workflowMessageService = inject(WorkflowMessageService);

  session?: SessionCaisseResponse;
  auditEvents: SessionAuditEvent[] = [];
  loading = false;
  error = '';
  message = '';
  validating = false;
  finalClosing = false;
  loadingTimeline = false;
  timelineError = '';
  timelinePage = 0;
  timelineSize = 50;
  hasNextTimelinePage = false;
  anomalies: SessionCaisseAnomalieResponse[] = [];
  loadingAnomalies = false;
  guidance: WorkflowGuidance | null = null;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error = 'Identifiant de session invalide.';
      return;
    }

    const navigationMessage = this.router.getCurrentNavigation()?.extras.state?.['successMessage'];
    const historyMessage = history.state?.successMessage;
    const successMessage = typeof navigationMessage === 'string' && navigationMessage.trim()
      ? navigationMessage
      : (typeof historyMessage === 'string' && historyMessage.trim() ? historyMessage : '');
    if (successMessage) {
      this.message = successMessage;
    }

    this.loadSession(id);
  }

  loadSession(id: number): void {
    this.loading = true;
    this.error = '';

    this.sessionCaisseService.getById(id).subscribe({
      next: (data) => {
        this.session = data;
        this.guidance = this.workflowMessageService.getGuidance({
          module: 'CAISSE',
          status: data.statut,
          currentRole: this.authService.getCurrentUser()?.role,
          permissions: this.authService.getCurrentUser()?.permissions,
          metadata: {
            ecart: data.ecartCaisse,
            expectedRole: data.statut === 'PRE_CLOTUREE' ? 'CONTROLEUR' : data.statut === 'VALIDEE_CONTROLE' ? 'CHEF_BUREAU' : 'CAISSIER'
          }
        });
        this.loading = false;
        this.loadAnomalies(id);
        if (this.canViewTimeline()) {
          this.loadTimeline(id);
        }
      },
      error: (err) => {
        this.error = err?.error?.message || 'Erreur lors du chargement de la session.';
        this.loading = false;
      }
    });
  }

  loadTimeline(sessionId: number): void {
    this.timelinePage = 0;
    this.auditEvents = [];
    this.loadTimelinePage(sessionId, this.timelinePage);
  }

  loadMoreTimeline(): void {
    if (!this.session || this.loadingTimeline || !this.hasNextTimelinePage) {
      return;
    }

    this.loadTimelinePage(this.session.id, this.timelinePage + 1);
  }

  private loadTimelinePage(sessionId: number, page: number): void {
    this.loadingTimeline = true;
    if (page === 0) {
      this.timelineError = '';
    }

    this.sessionAuditService.getSessionTimeline(sessionId, page, this.timelineSize).subscribe({
      next: (timelinePage) => {
        const mergedEvents = page === 0
          ? timelinePage.events
          : [...this.auditEvents, ...timelinePage.events];

        this.auditEvents = mergedEvents.sort(
          (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()
        );
        this.timelinePage = timelinePage.page;
        this.hasNextTimelinePage = timelinePage.hasNext;
        this.loadingTimeline = false;
      },
      error: (err) => {
        this.timelineError = err?.error?.message || 'Impossible de charger la timeline de session.';
        this.hasNextTimelinePage = false;
        this.loadingTimeline = false;
      }
    });
  }

  loadAnomalies(sessionId: number): void {
    this.loadingAnomalies = true;
    this.sessionCaisseService.getAnomaliesBySession(sessionId).subscribe({
      next: (data) => {
        this.anomalies = data;
        this.loadingAnomalies = false;
      },
      error: () => {
        this.loadingAnomalies = false;
      }
    });
  }

  formatCdf(value: number | null | undefined): string {
    if (value == null) {
      return '0 CDF';
    }

    return new Intl.NumberFormat('fr-CD').format(value) + ' CDF';
  }

  isSessionDuJour(): boolean {
    return !!this.session?.dateComptable && this.session.dateComptable === this.todayIsoDate();
  }

  isSessionAncienneOuverte(): boolean {
    return !!this.session && this.session.statut === 'OUVERTE' && !!this.session.dateComptable && this.session.dateComptable < this.todayIsoDate();
  }

  canAddOperations(): boolean {
    return !!this.session
      && this.session.statut === 'OUVERTE'
      && this.isSessionDuJour()
      && this.authService.hasAnyRole(['ADMIN', 'CHEF_BUREAU', 'RCI']);
  }

  canViewTimeline(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'RCI'])
      || this.authService.hasAnyPermission([
        PermissionCode.AUDIT_LOG_READ,
        PermissionCode.AUDIT_READ,
        PermissionCode.AUDIT_SECURITY_READ
      ]);
  }

  getSourceLabel(source?: SourceOperationCaisse): string {
    if (!source) return '-';
    return SOURCE_OPERATION_CAISSE_LABELS[source] ?? source;
  }

  getNatureMouvement(source?: SourceOperationCaisse): string {
    if (!source) return 'Non classé';

    if ([SourceOperationCaisse.MANUEL, SourceOperationCaisse.AJUSTEMENT, SourceOperationCaisse.APPROVISIONNEMENT, SourceOperationCaisse.AUTRE].includes(source)) {
      return 'Mouvement manuel';
    }
    if (source === SourceOperationCaisse.RECETTE_JOURNALIERE) {
      return 'Retour terrain';
    }
    if (source === SourceOperationCaisse.RETRAIT_EPARGNE) {
      return 'Mouvement retrait';
    }
    if ([SourceOperationCaisse.CREDIT_DECAISSEMENT, SourceOperationCaisse.CREDIT_REMBOURSEMENT].includes(source)) {
      return 'Mouvement crédit';
    }
    return 'Mouvement généré automatiquement';
  }

  canValidateControl(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'CONTROLEUR']);
  }

  canFinalClose(): boolean {
    return !!this.session
      && this.session.statut === 'VALIDEE_CONTROLE'
      && this.authService.hasAnyRole(['ADMIN', 'CHEF_BUREAU'])
      && this.authService.hasPermission(PermissionCode.SESSION_CAISSE_FINAL_CLOSE);
  }

  demanderAnnulation(): void {
    if (!this.session?.peutDemanderAnnulation) {
      return;
    }

    const motif = (window.prompt('Motif obligatoire pour la demande d\'annulation :', '') || '').trim();
    if (!motif) {
      this.error = 'MOTIF_OBLIGATOIRE';
      return;
    }

    const commentaire = (window.prompt('Commentaire (optionnel) :', '') || '').trim();
    this.sessionCaisseService.demanderAnnulation(this.session.id, {
      typeAnomalie: 'OUVERTURE_ERRONEE_SANS_MOUVEMENT',
      motif,
      commentaire: commentaire || undefined
    }).subscribe({
      next: () => {
        this.message = 'Demande d\'annulation enregistree.';
        this.loadAnomalies(this.session!.id);
        this.loadSession(this.session!.id);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Impossible de demander l\'annulation.';
      }
    });
  }

  validerAnnulation(): void {
    if (!this.session?.peutValiderAnnulation) {
      return;
    }

    const confirmation = window.confirm(`Valider l'annulation de la session #${this.session.id} ?`);
    if (!confirmation) {
      return;
    }

    const motifDecision = (window.prompt('Motif de validation (optionnel) :', '') || '').trim();
    this.sessionCaisseService.validerAnnulation(this.session.id, {
      decision: 'VALIDER',
      motifDecision: motifDecision || undefined
    }).subscribe({
      next: () => {
        this.message = 'Annulation validée.';
        this.loadAnomalies(this.session!.id);
        this.loadSession(this.session!.id);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Impossible de valider l\'annulation.';
      }
    });
  }

  annulerAdministrativement(): void {
    if (!this.session?.peutAnnulerAdministrativement) {
      return;
    }

    const motif = (window.prompt('Motif obligatoire d\'annulation administrative :', '') || '').trim();
    if (!motif) {
      this.error = 'MOTIF_OBLIGATOIRE';
      return;
    }

    const commentaire = (window.prompt('Commentaire (optionnel) :', '') || '').trim();
    this.sessionCaisseService.annulerAdministrativement(this.session.id, {
      typeAnomalie: 'CLOTURE_ERRONEE_SANS_MOUVEMENT',
      motif,
      commentaire: commentaire || undefined
    }).subscribe({
      next: (updated) => {
        this.session = updated;
        this.message = 'Session annulée administrativement.';
        this.loadAnomalies(this.session!.id);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Impossible d\'annuler administrativement.';
      }
    });
  }

  reouvrirControlee(): void {
    if (!this.session?.peutReouvrirControlee) {
      return;
    }

    const motif = (window.prompt('Motif obligatoire de reouverture controlee :', '') || '').trim();
    if (!motif) {
      this.error = 'MOTIF_OBLIGATOIRE';
      return;
    }

    const commentaire = (window.prompt('Commentaire (optionnel) :', '') || '').trim();
    this.sessionCaisseService.reouvrirControlee(this.session.id, {
      motif,
      commentaire: commentaire || undefined
    }).subscribe({
      next: (updated) => {
        this.session = updated;
        this.message = 'Session réouverte de manière contrôlée.';
        this.loadAnomalies(this.session!.id);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Impossible de réouvrir la session.';
      }
    });
  }

  validerControle(): void {
    if (!this.session || !this.canValidateControl() || this.session.statut !== 'PRE_CLOTUREE') {
      return;
    }

    this.validating = true;
    this.sessionCaisseService.validerControle(this.session.id).subscribe({
      next: (updated) => {
        this.session = updated;
        this.validating = false;
        this.loadTimeline(updated.id);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Impossible de valider le contrôle.';
        this.validating = false;
      }
    });
  }

  cloturerFinale(): void {
    if (!this.session || !this.canFinalClose() || this.session.statut !== 'VALIDEE_CONTROLE') {
      return;
    }

    const observation = window.prompt('Observation facultative pour la clôture finale :', '') ?? '';
    const confirmation = window.confirm(`Confirmer la clôture définitive de la session #${this.session.id} ?`);
    if (!confirmation) {
      return;
    }

    this.finalClosing = true;
    this.error = '';
    this.message = '';

    this.sessionCaisseService.cloturerFinale(this.session.id, {
      observation: observation.trim() || undefined
    }).subscribe({
      next: (updated) => {
        this.session = updated;
        this.message = 'Clôture finale effectuée avec succès.';
        this.finalClosing = false;
        this.loadTimeline(updated.id);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Impossible de clôturer définitivement la session.';
        this.finalClosing = false;
      }
    });
  }

  private todayIsoDate(): string {
    return new Date().toISOString().slice(0, 10);
  }
}