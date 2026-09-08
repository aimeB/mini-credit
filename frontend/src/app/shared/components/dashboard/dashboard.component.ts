import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService, User } from '../../../core/services/auth.service';
import { WorkflowGuidanceBannerComponent } from '../workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../models/workflow-guidance.model';
import { WorkflowMessageService } from '../../services/workflow-message.service';
import { WorkflowTaskDashboard } from '../../models/workflow-task.model';
import { WorkflowTaskService } from '../../services/workflow-task.service';
import { DemandeCreditService } from '../../../features/credit/services/demande-credit.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  currentUser: User | null = null;
  roleGuidance: WorkflowGuidance | null = null;
  taskDashboard: WorkflowTaskDashboard | null = null;
  loadingTaskDashboard = false;
  fraisCreditAEncaisserCount = 0;

  dashboardItems = [
    {
      title: 'Gestion des Membres',
      description: 'Ajouter, modifier et consulter les membres',
      icon: '👥',
      route: '/membres',
      roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN']
    },
    {
      title: 'Gestion des Crédits',
      description: 'Demandes, analyses et contrats de crédit',
      icon: '💰',
      route: '/credits',
      roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN']
    },
    {
      title: 'Gestion de la Caisse',
      description: 'Sessions, opérations et journal de caisse',
      icon: '🏦',
      route: '/caisses',
      roles: ['ADMIN', 'CAISSIER']
    },
    {
      title: 'Nouveau retrait épargne',
      description: 'Encoder une demande de retrait pour un membre au guichet.',
      icon: '🧾',
      route: '/epargne/demandes-retrait/nouveau',
      actionLabel: 'Créer une demande de retrait',
      roles: ['CAISSIER']
    },
    {
      title: 'Retraits épargne à payer',
      description: 'Consultez les retraits validés par le contrôleur et effectuez le paiement.',
      icon: '💵',
      route: '/epargne/demandes-retrait',
      queryParams: { statut: 'VALIDEE' },
      actionLabel: 'Voir les retraits à payer',
      roles: ['CAISSIER']
    },
    {
      title: 'Frais crédit à encaisser',
      description: 'Encaisser les frais de demande/analyse des dossiers crédit.',
      icon: '💳',
      route: '/credits/frais-a-encaisser',
      actionLabel: 'Voir les frais à encaisser',
      badgeKey: 'fraisCreditAEncaisserCount',
      roles: ['CAISSIER']
    },
    {
      title: 'Épargne',
      description: 'Comptes d\'épargne et opérations',
      icon: '💸',
      route: '/epargne',
      roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN']
    },
    {
      title: 'Gestion des Utilisateurs',
      description: 'Créer et gérer les utilisateurs et leurs rôles',
      icon: '🔐',
      route: '/utilisateurs',
      roles: ['ADMIN']
    },
    {
      title: 'Gestion du Personnel',
      description: 'Employés, salaires et données de paie',
      icon: '👔',
      route: '/employes',
      roles: ['ADMIN']
    },
    {
      title: 'Garanties crédit',
      description: 'Consultation des garanties et accès au workflow 3N',
      icon: '🔒',
      route: '/garanties',
      roles: ['ADMIN', 'CHEF_BUREAU', 'CONTROLEUR', 'RCI']
    },
    {
      title: 'Rapports & Analytics',
      description: 'Tableaux de bord, KPIs et analyses',
      icon: '📊',
      route: '/rapports',
      roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE']
    },
    {
      title: 'Reporting terrain & collectes',
      description: 'Suivi des collectes terrain, remises caisse, écarts et historique legacy',
      icon: '📝',
      route: '/recettes',
      roles: ['ADMIN', 'GESTIONNAIRE', 'CHEF_BUREAU', 'CONTROLEUR', 'COO', 'RCI', 'GERANT_GENERAL']
    },
    {
      title: 'Audit & Logs',
      description: 'Traçabilité des opérations',
      icon: '🔐',
      route: '/audit/logs',
      roles: ['ADMIN', 'RCI', 'CHEF_BUREAU', 'CONTROLEUR']
    },
    {
      title: 'Organisation',
      description: 'Agences, sites et structure opérationnelle. Consultez et gérez la hiérarchie Agence → Site → Agent.',
      icon: '🏢',
      route: '/organisation',
      roles: ['ADMIN']
    }
  ];

  constructor(
    private authService: AuthService,
    private workflowMessageService: WorkflowMessageService,
    private workflowTaskService: WorkflowTaskService,
    private demandeCreditService: DemandeCreditService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      this.refreshGuidance();
      this.loadTaskDashboard();
      this.loadFraisCreditAEncaisserCount();
    });
  }

  private loadFraisCreditAEncaisserCount(): void {
    if (this.normalizeRole(this.currentUser?.role) !== 'CAISSIER') {
      this.fraisCreditAEncaisserCount = 0;
      return;
    }

    this.demandeCreditService.getFraisCreditAEncaisser().subscribe({
      next: (demandes) => {
        this.fraisCreditAEncaisserCount = demandes?.length ?? 0;
      },
      error: () => {
        this.fraisCreditAEncaisserCount = 0;
      }
    });
  }

  private loadTaskDashboard(): void {
    if (!this.currentUser || !this.canShowTaskWidget()) {
      this.taskDashboard = null;
      return;
    }

    this.loadingTaskDashboard = true;
    this.workflowTaskService.getDashboard().subscribe({
      next: (dashboard) => {
        this.taskDashboard = dashboard;
        this.loadingTaskDashboard = false;
      },
      error: () => {
        this.taskDashboard = null;
        this.loadingTaskDashboard = false;
      }
    });
  }

  private refreshGuidance(): void {
    const roleStatus = this.resolveDashboardRoleStatus();
    this.roleGuidance = this.workflowMessageService.getGuidance({
      module: 'DASHBOARD_ROLE',
      status: roleStatus,
      currentRole: this.currentUser?.role,
      expectedRole: this.currentUser?.role,
      permissions: this.currentUser?.permissions,
      metadata: { dashboardType: 'PRINCIPAL', expectedRole: this.currentUser?.role }
    });
  }

  private resolveDashboardRoleStatus(): string {
    const role = (this.currentUser?.role || '').toUpperCase();

    if (role === 'CHEF_BUREAU') return 'CHEF_BUREAU';
    if (role === 'ADMIN') return 'GERANT_GENERAL';
    if (role) return role;
    return 'TRANSVERSE';
  }

  logout(): void {
    this.authService.logout();
  }

  hasAccess(item: any): boolean {
    if (!this.currentUser) return false;
    return item.roles.includes(this.normalizeRole(this.currentUser.role));
  }

  private normalizeRole(role: string | null | undefined): string {
    let normalized = (role ?? '').trim().toUpperCase();
    if (normalized.startsWith('ROLE_')) {
      normalized = normalized.slice(5);
    }
    return normalized;
  }

  canShowTaskWidget(): boolean {
    const role = this.normalizeRole(this.currentUser?.role);
    return ['ADMIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU', 'GESTIONNAIRE', 'RCI', 'COO', 'GERANT_GENERAL'].includes(role);
  }

  getActionsRoute(): string {
    return '/mes-actions';
  }

  getTotalAFaire(): number {
    return this.taskDashboard?.totalAFaire ?? this.taskDashboard?.countAFaire ?? 0;
  }

  getTotalEnCours(): number {
    return this.taskDashboard?.totalEnCours ?? 0;
  }

  getUrgentCount(): number {
    return this.taskDashboard?.urgentCount ?? 0;
  }

  getOverdueCount(): number {
    return this.taskDashboard?.overdueCount ?? 0;
  }

  getRoleLabel(role: string): string {
    const normalizedRole = this.normalizeRole(role);
    const roleLabels: { [key: string]: string } = {
      'ADMIN': 'Administrateur',
      'CHEF_BUREAU': 'Chef de Bureau',
      'GESTIONNAIRE': 'Gestionnaire',
      'AGENT_TERRAIN': 'Agent de Terrain',
      'CAISSIER': 'Caissier',
      'CONTROLEUR': 'Contrôleur',
      'RCI': 'Responsable Contrôle Interne',
      'MEMBER': 'Membre'
    };
    return roleLabels[normalizedRole] || normalizedRole;
  }

  getGradient(item: any): string {
    const gradients: { [key: string]: string } = {
      'Gestion des Membres': 'from-blue-500 to-blue-600',
      'Gestion des Crédits': 'from-emerald-500 to-emerald-600',
      'Gestion de la Caisse': 'from-orange-500 to-orange-600',
      'Nouveau retrait épargne': 'from-sky-500 to-cyan-600',
      'Retraits épargne à payer': 'from-amber-500 to-orange-600',
      'Frais crédit à encaisser': 'from-emerald-500 to-teal-600',
      'Épargne': 'from-yellow-500 to-yellow-600',
      'Gestion des Utilisateurs': 'from-red-500 to-red-600',
      'Gestion du Personnel': 'from-fuchsia-500 to-fuchsia-600',
      'Gestion des Garanties': 'from-purple-500 to-purple-600',
      'Rapports & Analytics': 'from-indigo-500 to-indigo-600',
      'Gestion des Recettes': 'from-teal-500 to-teal-600',
      'Audit & Logs': 'from-cyan-500 to-cyan-600',
      'Organisation': 'from-sky-500 to-sky-600'
    };
    return gradients[item.title] || 'from-blue-500 to-blue-600';
  }

  getButtonColor(item: any): string {
    const colors: { [key: string]: string } = {
      'Gestion des Membres': 'bg-blue-600 hover:bg-blue-700',
      'Gestion des Crédits': 'bg-emerald-600 hover:bg-emerald-700',
      'Gestion de la Caisse': 'bg-orange-600 hover:bg-orange-700',
      'Nouveau retrait épargne': 'bg-sky-600 hover:bg-sky-700',
      'Retraits épargne à payer': 'bg-amber-600 hover:bg-amber-700',
      'Frais crédit à encaisser': 'bg-emerald-600 hover:bg-emerald-700',
      'Épargne': 'bg-yellow-600 hover:bg-yellow-700',
      'Gestion des Utilisateurs': 'bg-red-600 hover:bg-red-700',
      'Gestion du Personnel': 'bg-fuchsia-600 hover:bg-fuchsia-700',
      'Gestion des Garanties': 'bg-purple-600 hover:bg-purple-700',
      'Rapports & Analytics': 'bg-indigo-600 hover:bg-indigo-700',
      'Gestion des Recettes': 'bg-teal-600 hover:bg-teal-700',
      'Audit & Logs': 'bg-cyan-600 hover:bg-cyan-700',
      'Organisation': 'bg-sky-600 hover:bg-sky-700'
    };
    return colors[item.title] || 'bg-blue-600 hover:bg-blue-700';
  }

  getBadgeValue(item: any): number | null {
    if (item?.badgeKey === 'fraisCreditAEncaisserCount') {
      return this.fraisCreditAEncaisserCount;
    }
    return null;
  }
}
