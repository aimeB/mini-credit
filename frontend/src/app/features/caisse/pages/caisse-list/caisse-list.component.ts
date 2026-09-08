import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CaisseResponse } from '../../models/caisse-response';
import { CaisseService } from '../../services/caisse.service';
import { SessionCaisseResponse } from '../../models/session-caisse-response';
import { StatutSessionCaisse } from '../../models/statut-session-caisse';
import { SessionCaisseService } from '../../services/session-caisse.service';
import { AuthService } from '../../../../core/services/auth.service';
import { PermissionCode } from '../../../../shared/enums/permission-code.enum';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';

@Component({
  selector: 'app-caisse-list',
  standalone: true,
  imports: [CommonModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './caisse-list.component.html'
})
export class CaisseListComponent implements OnInit {
  private readonly openSessionStatuses: StatutSessionCaisse[] = ['OUVERTE'];

  private caisseService: CaisseService;
  private sessionService: SessionCaisseService;
  private authService: AuthService;
  private workflowMessageService: WorkflowMessageService;

  caisses: CaisseResponse[] = [];
  sessions: SessionCaisseResponse[] = [];
  loading = false;
  error = '';
  successMessage = '';
  finalClosingSessionIds = new Set<number>();
  globalGuidance: WorkflowGuidance | null = null;

  constructor(
    caisseService: CaisseService,
    sessionService: SessionCaisseService,
    authService: AuthService,
    workflowMessageService: WorkflowMessageService
  ) {
    this.caisseService = caisseService;
    this.sessionService = sessionService;
    this.authService = authService;
    this.workflowMessageService = workflowMessageService;
  }

  get nbCaisses(): number { return this.caisses.length; }
  get nbSessionsOuvertes(): number {
    return this.sessions.filter(s => this.isOpenSessionStatus(s.statut)).length;
  }
  get nbSessionsEnAttenteCloture(): number {
    return this.sessions.filter(s => this.isFinalClosePendingStatus(s.statut)).length;
  }
  get soldeTotalTheorique(): number {
    return this.caisses.reduce((sum, caisse) => sum + Number(caisse.soldeDisponibleActuel ?? 0), 0);
  }
  get isCaissier(): boolean { return this.authService.hasRole('CAISSIER'); }
  get canCreateCaisse(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'CHEF_BUREAU']);
  }
  get canInitializeMyCaisse(): boolean {
    return this.isCaissier && this.caisses.length === 0;
  }
  get canOpenSession(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'CHEF_BUREAU', 'CAISSIER']);
  }
  get canAccessControle(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'CONTROLEUR', 'RCI', 'CHEF_BUREAU']);
  }
  get canAccessRapportsCaisse(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'CAISSIER', 'CONTROLEUR', 'RCI', 'CHEF_BUREAU']);
  }
  canFinalCloseCaisseSession(caisse: CaisseResponse): boolean {
    const session = this.findSessionToFinalClose(caisse);

    return !!session
      && this.isFinalClosePendingStatus(session.statut)
      && this.authService.hasAnyRole(['ADMIN', 'CHEF_BUREAU'])
      && this.authService.hasPermission(PermissionCode.SESSION_CAISSE_FINAL_CLOSE);
  }
  get sessionsAnciennesOuvertes(): SessionCaisseResponse[] {
    return this.sessions.filter((session) => this.isSessionAncienneOuverte(session));
  }

  ngOnInit(): void {
    this.updateGuidance();
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';

    this.caisseService.getAccessibles().subscribe({
      next: (data) => {
        this.caisses = data;
        this.debugCaissePayload('caisses', data);
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        const backendMessage = err?.error?.message || err?.error?.error || err?.message;
        const status = err?.status ? ` (HTTP ${err.status})` : '';
        this.error = backendMessage ? `${backendMessage}${status}` : `Impossible de charger les caisses.${status}`;
        this.loading = false;
      }
    });

    this.sessionService.getAll().subscribe({
      next: (data) => {
        this.sessions = data;
        this.debugCaissePayload('sessions', data);
        this.updateGuidance();
      },
      error: () => {}
    });
  }

  getSessionOuverte(caisseId: number): SessionCaisseResponse | undefined {
    return this.sessions.find(s => s.caisseId === caisseId && this.isOpenSessionStatus(s.statut));
  }

  getSessionOuverteDuJour(caisseId: number): SessionCaisseResponse | undefined {
    return this.sessions.find((session) => session.caisseId === caisseId && this.isOpenSessionStatus(session.statut) && this.isSessionDuJour(session));
  }

  getSessionValideeControle(caisseId: number): SessionCaisseResponse | undefined {
    return this.sessions.find((session) => session.caisseId === caisseId && this.isFinalClosePendingStatus(session.statut));
  }

  getSessionEnCoursAffichage(caisse: CaisseResponse): SessionCaisseResponse | undefined {
    return this.getSessionOuverteDuJour(caisse.id)
      || this.getSessionAncienneOuverte(caisse.id)
      || this.getSessionValideeControle(caisse.id)
      || this.sessions.find((session) => session.caisseId === caisse.id && session.statut === 'PRE_CLOTUREE');
  }

  findSessionToFinalClose(caisse: CaisseResponse): SessionCaisseResponse | null {
    return this.sessions.find((session) => session.caisseId === caisse.id && this.isFinalClosePendingStatus(session.statut)) ?? null;
  }

  getSessionAncienneOuverte(caisseId: number): SessionCaisseResponse | undefined {
    return this.sessions.find((session) => session.caisseId === caisseId && this.isSessionAncienneOuverte(session));
  }

  getDerniereSessionCloturee(caisseId: number): SessionCaisseResponse | undefined {
    return this.sessions
      .filter((session) => session.caisseId === caisseId && (session.statut === 'CLOTUREE' || session.statut === 'ANNULEE' || session.statut === 'ANNULEE_ADMINISTRATIVEMENT'))
      .sort((a, b) => {
        const dateComptableA = a.dateComptable ? new Date(a.dateComptable).getTime() : 0;
        const dateComptableB = b.dateComptable ? new Date(b.dateComptable).getTime() : 0;
        if (dateComptableA !== dateComptableB) {
          return dateComptableB - dateComptableA;
        }

        const clotureA = a.dateCloture ? new Date(a.dateCloture).getTime() : 0;
        const clotureB = b.dateCloture ? new Date(b.dateCloture).getTime() : 0;
        if (clotureA !== clotureB) {
          return clotureB - clotureA;
        }

        return b.id - a.id;
      })[0];
  }

  getStatutSessionAffichage(caisse: CaisseResponse): string {
    const sessionDuJour = this.getSessionOuverteDuJour(caisse.id);
    if (sessionDuJour) {
      return "Session ouverte aujourd'hui";
    }

    const sessionAncienne = this.getSessionAncienneOuverte(caisse.id);
    if (sessionAncienne) {
      return 'Session ancienne ouverte';
    }

    if (caisse.statutSession === 'CLOTUREE') {
      return 'Dernière session clôturée';
    }

    const sessionAnnulee = this.sessions.find((session) =>
      session.caisseId === caisse.id && (session.statut === 'ANNULEE' || session.statut === 'ANNULEE_ADMINISTRATIVEMENT')
    );
    if (sessionAnnulee) {
      return 'Session annulée (historique)';
    }

    return 'Aucune';
  }

  getSoldeAffiche(caisse: CaisseResponse): number {
    if (caisse.soldeDisponibleActuel != null) {
      return Number(caisse.soldeDisponibleActuel);
    }

    const sessionOuverte = this.getSessionOuverteDuJour(caisse.id) || this.getSessionAncienneOuverte(caisse.id) || this.getSessionValideeControle(caisse.id);
    return Number(sessionOuverte?.soldeTheorique ?? 0);
  }

  isSessionDuJour(session: SessionCaisseResponse | undefined): boolean {
    if (!session?.dateComptable) {
      return false;
    }
    return session.dateComptable === this.todayIsoDate();
  }

  isSessionAncienneOuverte(session: SessionCaisseResponse | undefined): boolean {
    return !!session
      && session.statut === 'OUVERTE'
      && !!session.dateComptable
      && session.dateComptable < this.todayIsoDate();
  }

  getSessionStatusChipLabel(session: SessionCaisseResponse | undefined): string {
    if (!session) {
      return 'Session active';
    }

    if (session.statut === 'OUVERTE') {
      return 'Session ouverte';
    }

    if (session.statut === 'PRE_CLOTUREE') {
      return 'Session soumise au contrôle';
    }

    if (session.statut === 'VALIDEE_CONTROLE') {
      return 'Session validée en contrôle';
    }

    return `Session ${session.statut}`;
  }

  formatCdf(value: number | null | undefined): string {
    if (value == null) return '0 CDF';
    return new Intl.NumberFormat('fr-CD').format(value) + ' CDF';
  }

  formatDateTime(value: string | null | undefined): string {
    if (!value) {
      return '-';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date);
  }

  formatDate(value: string | null | undefined): string {
    if (!value) {
      return '-';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    }).format(date);
  }

  formatDateShort(value: string | null | undefined): string {
    return this.formatDate(value);
  }

  getSessionDisplayUser(session: SessionCaisseResponse | undefined): string {
    if (!session) {
      return '-';
    }

    return session.utilisateurNom || session.caissierResponsableNom || 'Utilisateur inconnu';
  }

  getSessionLink(session: SessionCaisseResponse | undefined): string | null {
    return session?.id ? `/caisses/session/${session.id}` : null;
  }

  isFinalClosing(sessionId: number): boolean {
    return this.finalClosingSessionIds.has(sessionId);
  }

  onFinalCloseCaisseSession(caisse: CaisseResponse): void {
    const session = this.findSessionToFinalClose(caisse);
    if (!session?.id) {
      this.error = 'Aucune session validée contrôle à clôturer pour cette caisse.';
      return;
    }

    if (!this.canFinalCloseCaisseSession(caisse) || this.isFinalClosing(session.id)) {
      return;
    }

    const observation = window.prompt('Observation facultative pour la clôture finale :', '') ?? '';
    const confirmation = window.confirm(`Confirmer la clôture définitive de la session #${session.id} ?`);
    if (!confirmation) {
      return;
    }

    this.error = '';
    this.successMessage = '';
    this.finalClosingSessionIds.add(session.id);

    this.sessionService.cloturerFinale(session.id, {
      observation: observation.trim() || undefined
    }).subscribe({
      next: () => {
        this.finalClosingSessionIds.delete(session.id);
        this.successMessage = 'Clôture finale effectuée avec succès.';
        this.load();
      },
      error: (err) => {
        this.finalClosingSessionIds.delete(session.id);
        this.error = err?.error?.message || 'Impossible de clôturer définitivement la session.';
      }
    });
  }

  onInitializeMyCaisse(): void {
    if (!this.canInitializeMyCaisse || this.loading) {
      return;
    }

    this.loading = true;
    this.error = '';
    this.successMessage = '';

    this.caisseService.initialiserMaCaisse().subscribe({
      next: () => {
        this.successMessage = 'Caisse initialisée avec succès. Vous pouvez maintenant ouvrir une session.';
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Impossible d\'initialiser votre caisse.';
        this.loading = false;
      }
    });
  }

  trackByCaisseId(index: number, caisse: CaisseResponse): number {
    return caisse.id;
  }

  private updateGuidance(): void {
    const status = this.resolveDashboardStatus();
    if (!status) {
      this.globalGuidance = null;
      return;
    }

    const metadata: Record<string, unknown> = {};
    if (status === 'OUVERTE' && this.sessionsAnciennesOuvertes.length > 0) {
      metadata['isAncienneOuverte'] = true;
    }

    this.globalGuidance = this.workflowMessageService.getGuidance({
      module: 'CAISSE',
      status,
      currentRole: this.resolveCurrentRole(),
      expectedRole: status === 'OUVERTE' ? 'CAISSIER' : undefined,
      metadata,
      nextStep: status === 'OUVERTE' ? 'Soumission au contrôle' : undefined
    });
  }

  private resolveDashboardStatus(): string | null {
    if (this.sessions.length === 0) {
      return null;
    }

    if (this.sessions.some((session) => session.statut === 'VALIDEE_CONTROLE')) {
      return 'VALIDEE_CONTROLE';
    }

    if (this.sessions.some((session) => session.statut === 'PRE_CLOTUREE')) {
      return 'PRE_CLOTUREE';
    }

    if (this.sessions.some((session) => session.statut === 'OUVERTE')) {
      return 'OUVERTE';
    }

    return this.sessions.some((session) => session.statut === 'CLOTUREE') ? 'CLOTUREE' : null;
  }

  private isOpenSessionStatus(statut: StatutSessionCaisse | undefined): boolean {
    return !!statut && this.openSessionStatuses.includes(statut);
  }

  private isFinalClosePendingStatus(statut: StatutSessionCaisse | undefined): boolean {
    return statut === 'VALIDEE_CONTROLE';
  }

  private debugCaissePayload(label: string, payload: unknown): void {
    if (localStorage.getItem('debug.caisseList') === 'true') {
      console.debug(`[CaisseListComponent] ${label}`, payload);
    }
  }

  private resolveCurrentRole(): string | undefined {
    const currentUser = (this.authService as { getCurrentUser?: () => { role?: string } | null }).getCurrentUser?.();
    return currentUser?.role || undefined;
  }

  private todayIsoDate(): string {
    return new Date().toISOString().slice(0, 10);
  }
}