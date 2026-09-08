import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DashboardControleInterneService } from '../../services/dashboard-controle-interne.service';
import { AuthService } from '../../../../core/services/auth.service';
import { PermissionCode } from '../../../../shared/enums/permission-code.enum';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';
import {
  AlerteDashboardResponse,
  DashboardControleInterneResponse
} from '../../models/dashboard-controle-interne.model';

@Component({
  selector: 'app-dashboard-controle-interne',
  standalone: true,
  imports: [CommonModule, FormsModule, WorkflowGuidanceBannerComponent],
  templateUrl: './dashboard-controle-interne.component.html'
})
export class DashboardControleInterneComponent implements OnInit, OnDestroy {
  dashboard: DashboardControleInterneResponse | null = null;
  loading = false;
  error: string | null = null;

  dateDebut = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0];
  dateFin = new Date().toISOString().split('T')[0];
  siteId?: number;
  caisseId?: number;

  private destroy$ = new Subject<void>();
  private subscriptions = new Subscription();

  constructor(
    private dashboardService: DashboardControleInterneService,
    private router: Router,
    private authService: AuthService,
    private workflowMessageService: WorkflowMessageService
  ) {}

  get roleGuidance(): WorkflowGuidance {
    const role = (this.authService.getCurrentUser()?.role || '').toUpperCase();
    const status = this.resolveRoleStatus(role);
    return this.workflowMessageService.getGuidance({
      module: 'DASHBOARD_ROLE',
      status,
      currentRole: role,
      expectedRole: role,
      metadata: { dashboardType: 'CONTROLE_INTERNE', expectedRole: role }
    });
  }

  ngOnInit(): void {
    if (!this.canReadDashboard()) {
      this.router.navigate(['/access-denied'], {
        queryParams: {
          title: 'Accès non autorisé',
          detail: 'Permission DASHBOARD_CONTROLE_INTERNE_READ requise'
        }
      });
      return;
    }

    this.loadDashboard();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.subscriptions.unsubscribe();
  }

  loadDashboard(): void {
    this.loading = true;
    this.error = null;

    this.subscriptions.add(
      this.dashboardService.getDashboard({
        dateDebut: this.dateDebut,
        dateFin: this.dateFin,
        siteId: this.siteId,
        caisseId: this.caisseId
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.dashboard = response;
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Erreur lors du chargement du dashboard contrôle interne';
          this.loading = false;
          console.error(err);
        }
      })
    );
  }

  resetFilters(): void {
    this.dateDebut = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0];
    this.dateFin = new Date().toISOString().split('T')[0];
    this.siteId = undefined;
    this.caisseId = undefined;
    this.loadDashboard();
  }

  openAction(alerte: AlerteDashboardResponse): void {
    const route = this.resolveAlerteRoute(alerte);
    if (!route) {
      return;
    }
    this.router.navigateByUrl(route);
  }

  openCard(module: 'CAISSE' | 'DEPENSES' | 'RECETTES' | 'RETRAITS' | 'CREDITS' | 'AUDIT'): void {
    const route = this.getCardRoute(module);
    this.router.navigateByUrl(route);
  }

  resolveAlerteRoute(alerte: AlerteDashboardResponse): string | null {
    return alerte.routeFrontend || alerte.actionUrl || alerte.action?.route || null;
  }

  getCardRoute(module: 'CAISSE' | 'DEPENSES' | 'RECETTES' | 'RETRAITS' | 'CREDITS' | 'AUDIT'): string {
    const dateDebut = this.dateDebut;
    const dateFin = this.dateFin;
    const dateParams = `dateDebut=${dateDebut}&dateFin=${dateFin}`;

    switch (module) {
      case 'CAISSE':
        return `/caisses/controle?${dateParams}`;
      case 'DEPENSES':
        return `/caisses/depenses?${dateParams}`;
      case 'RECETTES':
        return `/recettes?${dateParams}`;
      case 'RETRAITS':
        return `/epargne/demandes-retrait?${dateParams}`;
      case 'CREDITS':
        return `/credits/liste`;
      case 'AUDIT':
        return `/audit/logs?${dateParams}`;
      default:
        return '/dashboard/controle-interne';
    }
  }

  getAlerteClass(niveau: string): string {
    const normalized = (niveau || '').toUpperCase();
    if (normalized === 'CRITIQUE' || normalized === 'CRITICAL') {
      return 'border-red-200 bg-red-50 text-red-800';
    }
    if (normalized === 'WARNING') {
      return 'border-amber-200 bg-amber-50 text-amber-800';
    }
    return 'border-slate-200 bg-slate-50 text-slate-700';
  }

  private resolveRoleStatus(role: string): string {
    if (role === 'CHEF_BUREAU') return 'CHEF_BUREAU';
    if (role === 'ADMIN') return 'GERANT_GENERAL';
    return role || 'TRANSVERSE';
  }

  private canReadDashboard(): boolean {
    const user = this.authService.getCurrentUser();
    const permissions = user?.permissions ?? [];

    if (permissions.length === 0) {
      return false;
    }

    return this.authService.hasPermission(PermissionCode.DASHBOARD_CONTROLE_INTERNE_READ);
  }
}
