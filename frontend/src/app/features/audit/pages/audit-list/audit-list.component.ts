import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuditService } from '../../services/audit.service';
import { AuditLog, AuditLogFilters, AuditLogPage } from '../../models/audit-log.model';
import { ActionAudit, AuditModule, SeveriteLog } from '../../models/audit.enum';
import { AuthService } from '../../../../core/services/auth.service';
import { PermissionCode } from '../../../../shared/enums/permission-code.enum';

@Component({
  selector: 'app-audit-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './audit-list.component.html',
  styleUrls: ['./audit-list.component.css']
})
export class AuditListComponent implements OnInit, OnDestroy {
  logs: AuditLog[] = [];
  loading = false;
  error: string | null = null;
  page = 0;
  pageSize = 25;
  totalElements = 0;
  totalPages = 0;

  filters: AuditLogFilters = {
    dateDebut: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0],
    dateFin: new Date().toISOString().split('T')[0],
    module: '',
    action: '',
    severity: '',
    success: undefined,
    userId: undefined,
    siteId: undefined,
    caisseId: undefined,
    sessionCaisseId: undefined,
    entityType: '',
    entityId: undefined,
    referenceMetier: ''
  };

  readonly moduleOptions = Object.values(AuditModule);
  readonly severityOptions = Object.values(SeveriteLog);
  readonly actionOptions = Object.values(ActionAudit);

  private destroy$ = new Subject<void>();
  private subscriptions = new Subscription();

  constructor(
    private auditService: AuditService,
    private authService: AuthService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.applyQueryFilters();
    this.loadLogs();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.subscriptions.unsubscribe();
  }

  loadLogs(): void {
    this.loading = true;
    this.error = null;

    this.subscriptions.add(
      this.auditService.getLogs(this.filters, this.page, this.pageSize)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (data: AuditLogPage) => {
            this.logs = data.content ?? [];
            this.totalElements = data.totalElements ?? 0;
            this.totalPages = data.totalPages ?? 0;
            this.page = data.number ?? this.page;
            this.loading = false;
          },
          error: (err) => {
            this.error = 'Erreur lors du chargement des logs d\'audit';
            this.loading = false;
            console.error(err);
          }
        })
    );
  }

  applyFilters(): void {
    this.page = 0;
    this.loadLogs();
  }

  resetFilters(): void {
    this.filters = {
      dateDebut: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0],
      dateFin: new Date().toISOString().split('T')[0],
      module: '',
      action: '',
      severity: '',
      success: undefined,
      userId: undefined,
      siteId: undefined,
      caisseId: undefined,
      sessionCaisseId: undefined,
      entityType: '',
      entityId: undefined,
      referenceMetier: ''
    };
    this.applyFilters();
  }

  goToPage(targetPage: number): void {
    if (targetPage < 0 || targetPage >= this.totalPages || targetPage === this.page) {
      return;
    }
    this.page = targetPage;
    this.loadLogs();
  }

  exportCsv(): void {
    this.auditService.exportCsv(this.filters)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `audit-logs-${this.filters.dateFin ?? 'export'}.csv`;
          link.click();
        },
        error: (err) => {
          alert('Erreur lors de l\'export');
          console.error(err);
        }
      });
  }

  canExport(): boolean {
    const user = this.authService.getCurrentUser();
    const permissions = user?.permissions ?? [];

    if (permissions.length > 0) {
      return permissions.includes(PermissionCode.AUDIT_LOG_EXPORT);
    }

    const role = user?.role;
    return role === 'ADMIN' || role === 'RCI';
  }

  showExportFallbackNotice(): boolean {
    const user = this.authService.getCurrentUser();
    return !!user && (!user.permissions || user.permissions.length === 0) && this.canExport();
  }

  setSuccessFilter(value: string): void {
    if (value === '') {
      this.filters.success = undefined;
      return;
    }
    this.filters.success = value === 'true';
  }

  getActionColor(action: string): string {
    const colors: { [key: string]: string } = {
      'SESSION_CAISSE_OPENED': 'bg-green-100 text-green-800',
      'OPERATION_CAISSE_CREATED': 'bg-blue-100 text-blue-800',
      'DEPENSE_CAISSE_PAID': 'bg-orange-100 text-orange-800',
      'REFUS_TRANSITION': 'bg-amber-100 text-amber-800',
      'DATA_EXPORT': 'bg-cyan-100 text-cyan-800',
      'ACCESS_DENIED': 'bg-red-100 text-red-800'
    };
    return colors[action] || 'bg-gray-100 text-gray-800';
  }

  getSeveriteColor(severite: string): string {
    const colors: { [key: string]: string } = {
      'INFO': 'text-gray-700',
      'WARNING': 'text-amber-700 font-semibold',
      'CRITICAL': 'text-red-700 font-bold'
    };
    return colors[severite] || 'text-gray-600';
  }

  private applyQueryFilters(): void {
    const params = this.route.snapshot.queryParamMap;

    this.filters = {
      ...this.filters,
      dateDebut: params.get('dateDebut') || this.filters.dateDebut,
      dateFin: params.get('dateFin') || this.filters.dateFin,
      module: params.get('module') || this.filters.module,
      action: params.get('action') || this.filters.action,
      severity: params.get('severity') || this.filters.severity,
      referenceMetier: params.get('referenceMetier') || this.filters.referenceMetier,
      siteId: params.get('siteId') ? Number(params.get('siteId')) : this.filters.siteId,
      caisseId: params.get('caisseId') ? Number(params.get('caisseId')) : this.filters.caisseId,
      sessionCaisseId: params.get('sessionCaisseId') ? Number(params.get('sessionCaisseId')) : this.filters.sessionCaisseId,
      userId: params.get('userId') ? Number(params.get('userId')) : this.filters.userId,
      entityType: params.get('entityType') || this.filters.entityType,
      entityId: params.get('entityId') ? Number(params.get('entityId')) : this.filters.entityId,
      success: params.get('success') === null ? this.filters.success : params.get('success') === 'true'
    };
  }
}
