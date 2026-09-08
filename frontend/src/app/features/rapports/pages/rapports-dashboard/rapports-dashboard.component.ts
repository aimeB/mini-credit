import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { RapportService } from '../../services/rapport.service';
import { KPIDashboard, RapportRisque } from '../../models/rapport.model';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-rapports-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, WorkflowGuidanceBannerComponent],
  templateUrl: './rapports-dashboard.component.html',
  styleUrls: ['./rapports-dashboard.component.css']
})
export class RapportsDashboardComponent implements OnInit, OnDestroy {
  kpi: KPIDashboard | null = null;
  risques: RapportRisque[] = [];
  
  loading = false;
  error: string | null = null;

  dateDebut = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0];
  dateFin = new Date().toISOString().split('T')[0];

  private readonly revenusReportRoles = ['ADMIN', 'GERANT_GENERAL', 'COO', 'RCI', 'CHEF_BUREAU', 'CONTROLEUR'];

  private destroy$ = new Subject<void>();
  private subscriptions = new Subscription();

  constructor(
    private rapportService: RapportService,
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
      metadata: { dashboardType: 'RAPPORTS', expectedRole: role }
    });
  }

  ngOnInit(): void {
    this.loadDashboard();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.subscriptions.unsubscribe();
  }

  private resolveRoleStatus(role: string): string {
    if (role === 'CHEF_BUREAU') return 'CHEF_BUREAU';
    if (role === 'ADMIN') return 'GERANT_GENERAL';
    return role || 'TRANSVERSE';
  }

  canAccessRevenusReport(): boolean {
    const role = (this.authService.getCurrentUser()?.role || '').replace(/^ROLE_/, '').toUpperCase();
    return this.revenusReportRoles.includes(role);
  }

  loadDashboard(): void {
    this.loading = true;
    this.error = null;

    this.subscriptions.add(
      this.rapportService.getKPIDashboard()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (data) => {
            this.kpi = data;
            this.loading = false;
          },
          error: (err) => {
            this.error = 'Erreur lors du chargement du tableau de bord';
            this.loading = false;
            console.error(err);
          }
        })
    );

    this.subscriptions.add(
      this.rapportService.getRapportRisque()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (data) => {
            this.risques = Array.isArray(data) ? data : [data];
          },
          error: (err) => {
            console.error('Erreur lors du chargement des risques', err);
          }
        })
    );
  }

  getNiveauRisqueColor(niveau: string): string {
    const colors: { [key: string]: string } = {
      'FAIBLE': 'bg-green-100 text-green-800',
      'MOYEN': 'bg-yellow-100 text-yellow-800',
      'ELEVE': 'bg-orange-100 text-orange-800',
      'CRITIQUE': 'bg-red-100 text-red-800'
    };
    return colors[niveau] || 'bg-gray-100 text-gray-800';
  }

  exportPDF(type: string): void {
    this.rapportService.exportRapportPDF(type, this.dateDebut, this.dateFin)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `rapport-${type}-${this.dateFin}.pdf`;
          link.click();
        },
        error: (err) => {
          alert('Erreur lors de l\'export PDF');
          console.error(err);
        }
      });
  }

  exportExcel(type: string): void {
    this.rapportService.exportRapportExcel(type, this.dateDebut, this.dateFin)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `rapport-${type}-${this.dateFin}.xlsx`;
          link.click();
        },
        error: (err) => {
          alert('Erreur lors de l\'export Excel');
          console.error(err);
        }
      });
  }
}
