import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { RouterModule } from '@angular/router';

import { JournalCaisseFilter } from '../../models/journal-caisse-filter';
import { JournalCaisseResponse } from '../../models/journal-caisse-response';
import { OperationCaisseService } from '../../services/operation-caisse.service';
import { SOURCE_OPERATION_CAISSE_LABELS, SourceOperationCaisse } from '../../models/source-operation-caisse';
import { CategorieOperationCaisse } from '../../models/categorie-operation-caisse';
import { TypeOperationCaisse } from '../../models/type-operation-caisse';
import { Page } from '../../../../shared/models/page.model';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';
import { SessionCaisseResponse } from '../../models/session-caisse-response';
import { SessionCaisseService } from '../../services/session-caisse.service';

@Component({
  selector: 'app-journal-caisse',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './journal-caisse.component.html'
})
export class JournalCaisseComponent implements OnInit {
  private readonly operationCaisseService = inject(OperationCaisseService);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly workflowMessageService = inject(WorkflowMessageService);
  private readonly sessionCaisseService = inject(SessionCaisseService);

  readonly operations = signal<JournalCaisseResponse[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  journalGuidance: WorkflowGuidance | null = null;
  sessions: SessionCaisseResponse[] = [];
  selectedSession: SessionCaisseResponse | null = null;

  readonly page = signal(0);
  readonly size = signal(20);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  dateDebut = '';
  dateFin = '';
  typeOperation: '' | TypeOperationCaisse = '';
  caisseId = '';
  sessionCaisseId = '';
  siteId = '';
  utilisateurId = '';
  categorie: '' | CategorieOperationCaisse = '';
  source: '' | SourceOperationCaisse = '';
  referenceMetier = '';
  recetteId = '';
  depenseCaisseId = '';
  creditId = '';
  retraitEpargneId = '';
  operationEpargneId = '';

  readonly SourceOperationCaisse = SourceOperationCaisse;

  readonly typeOptions: TypeOperationCaisse[] = ['ENTREE', 'SORTIE'];

  readonly sourceOptions = Object.values(SourceOperationCaisse);

  readonly categorieOptions: CategorieOperationCaisse[] = [
    'COTISATION', 'EPARGNE', 'RETRAIT_EPARGNE', 'FRAIS_DEMANDE', 'DEPOT_GARANTIE',
    'DECAISSEMENT_CREDIT', 'REMBOURSEMENT_CREDIT', 'PENALITE_RETARD', 'APPROVISIONNEMENT',
    'DEPENSE', 'ENTREE_DIVERSE', 'SORTIE_DIVERSE', 'FRAIS_DEMANDE_CREDIT', 'FRAIS_RETRAIT_EPARGNE', 'DEPOT_GARANTIE_CREDIT'
  ];

  ngOnInit(): void {
    this.journalGuidance = this.workflowMessageService.getGuidance({
      module: 'CAISSE',
      status: 'CLOTUREE',
      currentRole: this.authService.getCurrentUser()?.role,
      nextStep: 'Audit et contrôle interne'
    });
    this.applyQueryFilters();
    this.loadSessions();
    this.rechercher();
  }

  rechercher(page: number = 0): void {
    this.page.set(page);
    this.loading.set(true);
    this.errorMessage.set('');

    this.operationCaisseService.getJournalCaisse(this.buildFilters(), this.page(), this.size(), 'dateOperation,desc').subscribe({
      next: (data: Page<JournalCaisseResponse>) => {
        this.operations.set(data?.content ?? []);
        this.totalElements.set(data?.totalElements ?? 0);
        this.totalPages.set(data?.totalPages ?? 0);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Erreur chargement journal caisse :', error);
        this.errorMessage.set(
          error?.error?.message || 'Impossible de charger le journal de caisse.'
        );
        this.loading.set(false);
      }
    });
  }

  readonly totalEntrees = computed(() => {
    return this.operations()
      .filter((op) => op.typeOperation === 'ENTREE')
      .reduce((sum, op) => sum + Number(op.montant ?? 0), 0);
  });

  readonly totalSorties = computed(() => {
    return this.operations()
      .filter((op) => op.typeOperation === 'SORTIE')
      .reduce((sum, op) => sum + Number(op.montant ?? 0), 0);
  });

  readonly soldeFiltre = computed(() => {
    return this.totalEntrees() - this.totalSorties();
  });

  get montantEntreesSynthese(): number {
    return this.selectedSession?.totalEntrees ?? this.totalEntrees();
  }

  get montantSortiesSynthese(): number {
    return this.selectedSession?.totalSorties ?? this.totalSorties();
  }

  get variationNetteSynthese(): number {
    return this.montantEntreesSynthese - this.montantSortiesSynthese;
  }

  get soldeInitialSynthese(): number | null {
    return this.selectedSession?.soldeOuverture ?? null;
  }

  get soldeFinalTheoriqueSynthese(): number | null {
    if (this.selectedSession?.soldeTheorique != null) {
      return this.selectedSession.soldeTheorique;
    }

    if (this.soldeInitialSynthese != null) {
      return this.soldeInitialSynthese + this.variationNetteSynthese;
    }

    return null;
  }

  readonly caisseOptions = computed(() => {
    const ids = this.operations()
      .map(op => op.caisseId)
      .filter((id): id is number => id != null);

    return [...new Set(ids)].sort((a, b) => a - b);
  });

  get journalTitle(): string {
    if (!this.selectedSession) {
      return 'Journal de caisse';
    }

    const date = this.formatDate(this.selectedSession.dateComptable);
    return `Journal de caisse - Session #${this.selectedSession.id}${date ? ' - ' + date : ''}`;
  }

  hasFiltresActifs(): boolean {
    return !!(
      this.dateDebut ||
      this.dateFin ||
      this.typeOperation ||
      this.caisseId ||
      this.sessionCaisseId ||
      this.siteId ||
      this.utilisateurId ||
      this.categorie ||
      this.source ||
      this.referenceMetier ||
      this.recetteId ||
      this.depenseCaisseId ||
      this.creditId ||
      this.retraitEpargneId ||
      this.operationEpargneId
    );
  }

  resetFilters(): void {
    this.dateDebut = '';
    this.dateFin = '';
    this.typeOperation = '';
    this.caisseId = '';
    this.sessionCaisseId = '';
    this.siteId = '';
    this.utilisateurId = '';
    this.categorie = '';
    this.source = '';
    this.referenceMetier = '';
    this.recetteId = '';
    this.depenseCaisseId = '';
    this.creditId = '';
    this.retraitEpargneId = '';
    this.operationEpargneId = '';
    this.selectedSession = null;
    this.rechercher(0);
  }

  onSessionFilterChanged(): void {
    this.selectedSession = this.findSessionById(this.sessionCaisseId);
  }

  trackByOperationId(_: number, operation: JournalCaisseResponse): number {
    return operation.operationId;
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.rechercher(this.page() + 1);
    }
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.rechercher(this.page() - 1);
    }
  }

  getReferenceLabel(operation: JournalCaisseResponse): string {
    if (operation.referenceMetier) return operation.referenceMetier;
    if (operation.recetteId) return `RECETTE-${operation.recetteId}`;
    if (operation.depenseCaisseId) return `DEPENSE-${operation.depenseCaisseId}`;
    if (operation.creditId) return `CREDIT-${operation.creditId}`;
    if (operation.retraitEpargneId) return `RETRAIT-${operation.retraitEpargneId}`;
    if (operation.operationEpargneId) return `OP_EPARGNE-${operation.operationEpargneId}`;
    return '-';
  }

  /** PATCH 7 — Libellé lisible de la source */
  getSourceLabel(operation: JournalCaisseResponse): string {
    if (!operation.source) return '-';
    return SOURCE_OPERATION_CAISSE_LABELS[operation.source as SourceOperationCaisse] ?? operation.source;
  }

  getTypeBadgeClass(type: TypeOperationCaisse): string {
    return type === 'ENTREE' ? 'badge badge-in' : 'badge badge-out';
  }

  getSessionBadgeClass(statutSession?: string): string {
    if (!statutSession) return 'bg-gray-100 text-gray-600';
    if (statutSession === 'OUVERTE') return 'bg-emerald-100 text-emerald-700';
    if (statutSession === 'PRE_CLOTUREE') return 'bg-amber-100 text-amber-700';
    if (statutSession === 'VALIDEE_CONTROLE') return 'bg-indigo-100 text-indigo-700';
    if (statutSession === 'CLOTUREE') return 'bg-slate-200 text-slate-700';
    return 'bg-gray-100 text-gray-600';
  }

  getRoleBadgeClass(role?: string): string {
    if (!role) return 'bg-gray-100 text-gray-600';
    if (role === 'CAISSIER') return 'bg-blue-100 text-blue-700';
    if (role === 'CHEF_BUREAU' || role === 'CHEF_BUREAU') return 'bg-purple-100 text-purple-700';
    if (role === 'CONTROLEUR') return 'bg-indigo-100 text-indigo-700';
    if (role === 'ADMIN') return 'bg-red-100 text-red-700';
    return 'bg-gray-100 text-gray-600';
  }

  truncate(value: string | null | undefined, max = 48): string {
    if (!value) return '-';
    return value.length > max ? `${value.slice(0, max)}...` : value;
  }

  formatCdf(value: number | null | undefined): string {
    if (value == null) return '0 CDF';
    return new Intl.NumberFormat('fr-CD').format(value) + ' CDF';
  }

  formatOptionalCdf(value: number | null | undefined): string {
    if (value == null) return '-';
    return this.formatCdf(value);
  }

  private buildFilters(): JournalCaisseFilter {
    return {
      sessionCaisseId: this.sessionCaisseId ? Number(this.sessionCaisseId) : undefined,
      caisseId: this.caisseId ? Number(this.caisseId) : undefined,
      siteId: this.siteId ? Number(this.siteId) : undefined,
      utilisateurId: this.utilisateurId ? Number(this.utilisateurId) : undefined,
      typeOperation: this.typeOperation || undefined,
      categorie: this.categorie || undefined,
      source: this.source || undefined,
      referenceMetier: this.referenceMetier || undefined,
      dateDebut: this.dateDebut || undefined,
      dateFin: this.dateFin || undefined,
      recetteId: this.recetteId ? Number(this.recetteId) : undefined,
      depenseCaisseId: this.depenseCaisseId ? Number(this.depenseCaisseId) : undefined,
      creditId: this.creditId ? Number(this.creditId) : undefined,
      retraitEpargneId: this.retraitEpargneId ? Number(this.retraitEpargneId) : undefined,
      operationEpargneId: this.operationEpargneId ? Number(this.operationEpargneId) : undefined
    };
  }

  private applyQueryFilters(): void {
    const params = this.route.snapshot.queryParamMap;
    this.dateDebut = params.get('dateDebut') || this.dateDebut;
    this.dateFin = params.get('dateFin') || this.dateFin;
    this.typeOperation = (params.get('typeOperation') as TypeOperationCaisse | null) || this.typeOperation;
    this.caisseId = params.get('caisseId') || this.caisseId;
    this.sessionCaisseId = params.get('sessionId') || params.get('sessionCaisseId') || this.sessionCaisseId;
    this.siteId = params.get('siteId') || this.siteId;
    this.utilisateurId = params.get('utilisateurId') || this.utilisateurId;
    this.categorie = (params.get('categorie') as CategorieOperationCaisse | null) || this.categorie;
    this.source = (params.get('source') as SourceOperationCaisse | null) || this.source;
    this.referenceMetier = params.get('referenceMetier') || this.referenceMetier;
    this.recetteId = params.get('recetteId') || this.recetteId;
    this.depenseCaisseId = params.get('depenseCaisseId') || this.depenseCaisseId;
    this.creditId = params.get('creditId') || this.creditId;
    this.retraitEpargneId = params.get('retraitEpargneId') || this.retraitEpargneId;
    this.operationEpargneId = params.get('operationEpargneId') || this.operationEpargneId;
  }

  private loadSessions(): void {
    this.sessionCaisseService.getAll().subscribe({
      next: (sessions) => {
        this.sessions = [...sessions].sort((a, b) => this.sessionSortValue(b) - this.sessionSortValue(a));
        this.selectedSession = this.findSessionById(this.sessionCaisseId);
      },
      error: () => {
        this.sessions = [];
        this.selectedSession = null;
      }
    });
  }

  private findSessionById(value: string): SessionCaisseResponse | null {
    const sessionId = Number(value);
    if (!sessionId) {
      return null;
    }
    return this.sessions.find((session) => session.id === sessionId) ?? null;
  }

  private formatDate(value: string | null | undefined): string {
    if (!value) {
      return '';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return new Intl.DateTimeFormat('fr-FR').format(date);
  }

  private sessionSortValue(session: SessionCaisseResponse): number {
    const value = session.dateComptable || session.dateCloture || session.dateOuverture || '';
    return value ? new Date(value).getTime() : 0;
  }
}