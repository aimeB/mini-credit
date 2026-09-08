import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Subject, forkJoin, of, takeUntil } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  WorkflowTaskItem,
  WorkflowTaskModule,
  WorkflowTaskPriority,
  WorkflowTaskStatus
} from '../../../../shared/models/workflow-task.model';
import { WorkflowTaskService } from '../../../../shared/services/workflow-task.service';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-mes-actions',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './mes-actions.component.html',
  styleUrl: './mes-actions.component.css'
})
export class MesActionsComponent implements OnInit, OnDestroy {
  private readonly workflowTaskService = inject(WorkflowTaskService);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly destroy$ = new Subject<void>();

  loading = false;
  error = '';
  actions: WorkflowTaskItem[] = [];
  supervisionActions: WorkflowTaskItem[] = [];
  activeView: 'mine' | 'supervision' = 'mine';

  statutFilter: WorkflowTaskStatus | '' = 'A_FAIRE';
  prioriteFilter: WorkflowTaskPriority | '' = '';
  moduleFilter: WorkflowTaskModule | '' = '';

  readonly statuts: Array<WorkflowTaskStatus | ''> = ['', 'A_FAIRE', 'EN_COURS', 'TERMINEE', 'ANNULEE'];
  readonly priorites: Array<WorkflowTaskPriority | ''> = ['', 'CRITIQUE', 'HAUTE', 'MOYENNE', 'BASSE'];
  readonly modules: Array<WorkflowTaskModule | ''> = ['', 'CAISSE', 'CREDIT', 'EPARGNE', 'RECETTE'];

  ngOnInit(): void {
    this.loadActions();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadActions(): void {
    this.loading = true;
    this.error = '';

    const supervision$ = this.canSuperviseActions()
      ? this.workflowTaskService.getSupervisionActions(this.statutFilter).pipe(catchError(() => of([])))
      : of([]);

    forkJoin({
      actions: this.workflowTaskService.getMyActions(this.statutFilter),
      supervisionActions: supervision$
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ actions, supervisionActions }) => {
          this.actions = actions;
          this.supervisionActions = supervisionActions;
          this.loading = false;
        },
        error: (err) => {
          this.error = err?.error?.message || 'Impossible de charger vos actions.';
          this.loading = false;
        }
      });
  }

  get filteredActions(): WorkflowTaskItem[] {
    return this.filterActions(this.actions);
  }

  get filteredSupervisionActions(): WorkflowTaskItem[] {
    return this.filterActions(this.supervisionActions);
  }

  get visibleActions(): WorkflowTaskItem[] {
    return this.activeView === 'supervision' ? this.filteredSupervisionActions : this.filteredActions;
  }

  private filterActions(actions: WorkflowTaskItem[]): WorkflowTaskItem[] {
    return actions.filter((action) => {
      if (this.prioriteFilter && action.priorite !== this.prioriteFilter) {
        return false;
      }
      if (this.moduleFilter && action.module !== this.moduleFilter) {
        return false;
      }
      return true;
    });
  }

  canSuperviseActions(): boolean {
    const currentRole = this.normalizeRole(this.authService.getCurrentUser()?.role);
    const permissions = this.authService.getCurrentUser()?.permissions ?? [];
    return ['ADMIN', 'RCI', 'COO', 'GERANT_GENERAL'].includes(currentRole)
      || permissions.includes('TASK_SUPERVISE')
      || permissions.includes('TASK_AUDIT');
  }

  canTreat(action: WorkflowTaskItem): boolean {
    return this.activeView === 'mine'
      && this.normalizeRole(action.roleDestinataire) === this.normalizeRole(this.authService.getCurrentUser()?.role);
  }

  marquerCommeVu(action: WorkflowTaskItem): void {
    if (!this.canTreat(action) || action.statut !== 'A_FAIRE') {
      return;
    }

    this.workflowTaskService.markAsViewed(action.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.replaceAction(updated);
        },
        error: () => {
          this.error = 'Impossible de marquer cette action comme vue.';
        }
      });
  }

  terminer(action: WorkflowTaskItem): void {
    if (!this.canTreat(action) || action.statut === 'TERMINEE' || action.statut === 'ANNULEE') {
      return;
    }

    const commentaire = (window.prompt('Commentaire de clôture (optionnel) :', '') || '').trim();
    this.workflowTaskService.complete(action.id, commentaire || undefined)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.replaceAction(updated);
        },
        error: () => {
          this.error = 'Impossible de terminer cette action.';
        }
      });
  }

  voirDossier(action: WorkflowTaskItem): void {
    const currentUser = this.authService.getCurrentUser();
    const targetUrl = this.resolveTargetUrl(action);
    console.debug('[MesActions] Voir dossier', {
      actionId: action.id,
      type: action.type,
      module: action.module,
      entityId: action.entityId,
      entityType: action.entityType,
      typeAction: action.typeAction,
      reference: action.referenceMetier,
      roleAttendu: action.roleAttendu,
      targetUrl,
      routeFromApi: action.route,
      currentUser,
      currentRole: currentUser?.role,
      currentPermissions: currentUser?.permissions ?? []
    });

    if (targetUrl) {
      this.router.navigateByUrl(targetUrl);
      return;
    }

    this.router.navigate(['/dashboard']);
  }

  private resolveTargetUrl(action: WorkflowTaskItem): string | null {
    if (action.module === 'CREDIT' && action.typeAction === 'DECAISSER_CREDIT' && action.entityId) {
      return `/credits/${action.entityId}/decaissement`;
    }

    if (action.module === 'CREDIT' && action.typeAction === 'APPROUVER_DEMANDE_CREDIT' && action.entityId) {
      return `/credits/demandes?demandeId=${action.entityId}`;
    }

    if (action.module === 'CAISSE' && action.typeAction === 'CONTROLER_SESSION_CAISSE' && action.entityId) {
      return `/caisses/sessions/${action.entityId}`;
    }

    if (action.route) {
      return action.route;
    }

    if (action.module === 'CAISSE' && action.entityType === 'DEPENSE_CAISSE' && action.entityId) {
      return '/caisses/depenses';
    }

    if (action.module === 'CAISSE' && action.entityType === 'SESSION_CAISSE' && action.entityId) {
      return `/caisses/sessions/${action.entityId}`;
    }

    if (action.module === 'EPARGNE' && action.entityType === 'RETRAIT_EPARGNE' && action.entityId) {
      return `/epargne/demandes-retrait/${action.entityId}`;
    }

    if (action.module === 'RECETTE' && action.entityType === 'RECETTE_TERRAIN' && action.entityId) {
      return `/recettes/${action.entityId}/valider`;
    }

    if (action.module === 'CREDIT' && action.entityType === 'DEMANDE_CREDIT' && action.entityId) {
      return `/credits/demandes/${action.entityId}`;
    }

    if (action.module === 'CREDIT' && action.entityType === 'CREDIT' && action.entityId) {
      return `/credits/${action.entityId}/contrat`;
    }

    return null;
  }

  getStatutBadgeClasses(statut: WorkflowTaskStatus): string {
    if (statut === 'A_FAIRE') return 'bg-amber-100 text-amber-800';
    if (statut === 'EN_COURS') return 'bg-blue-100 text-blue-800';
    if (statut === 'TERMINEE') return 'bg-emerald-100 text-emerald-800';
    return 'bg-slate-200 text-slate-700';
  }

  getPrioriteBadgeClasses(priorite: WorkflowTaskPriority): string {
    if (priorite === 'CRITIQUE') return 'bg-red-100 text-red-700';
    if (priorite === 'HAUTE') return 'bg-orange-100 text-orange-700';
    if (priorite === 'MOYENNE') return 'bg-indigo-100 text-indigo-700';
    return 'bg-slate-100 text-slate-700';
  }

  private replaceAction(updated: WorkflowTaskItem): void {
    this.actions = this.actions.map((item) => item.id === updated.id ? updated : item);
  }

  private normalizeRole(role: string | null | undefined): string {
    let normalized = (role ?? '').trim().toUpperCase();
    if (normalized.startsWith('ROLE_')) {
      normalized = normalized.slice(5);
    }
    return normalized;
  }
}
