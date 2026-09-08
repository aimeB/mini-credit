import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';

@Component({
  selector: 'app-credit-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './credit-dashboard.component.html'
})
export class CreditDashboardComponent {
  constructor(
    private authService: AuthService,
    private workflowMessageService: WorkflowMessageService
  ) {}

  get globalGuidance(): WorkflowGuidance {
    const role = this.currentRole.toUpperCase();
    const status = this.resolveRoleStatus(role);
    return this.workflowMessageService.getGuidance({
      module: 'DASHBOARD_ROLE',
      status,
      currentRole: role,
      expectedRole: role,
      metadata: { dashboardType: 'CREDIT', expectedRole: role }
    });
  }

  get isAgentTerrain(): boolean {
    return this.authService.hasRole('AGENT_TERRAIN');
  }

  get currentRole(): string {
    return this.authService.getCurrentUser()?.role || '';
  }

  private resolveRoleStatus(role: string): string {
    if (role === 'CHEF_BUREAU') return 'CHEF_BUREAU';
    if (role === 'ADMIN') return 'GERANT_GENERAL';
    return role || 'TRANSVERSE';
  }

  get roleTasks(): string[] {
    const role = this.currentRole.toUpperCase();
    const tasksByRole: Record<string, string[]> = {
      GESTIONNAIRE: [
        'Demandes à pré-analyser',
        'Dossiers en remboursement à suivre'
      ],
      CONTROLEUR: [
        'Recettes à contrôler',
        'Retraits à valider',
        'Garanties à vérifier',
        'Sessions caisse à contrôler'
      ],
      CHEF_BUREAU: [
        'Crédits à approuver'
      ],
      CAISSIER: [
        'Décaissements à payer',
        'Retraits approuvés à payer',
        'Billetage à confirmer'
      ],
      AGENT_TERRAIN: [
        'Recettes à compléter',
        'Demandes collectées à soumettre'
      ],
      RCI: [
        'Anomalies, écarts et audit à traiter'
      ],
      COO: [
        'Supervision opérationnelle inter-modules'
      ],
      ADMIN: [
        'Suivi des arbitrages exceptionnels'
      ]
    };

    return tasksByRole[role] || ['Aucune tâche métier assignée pour ce rôle dans ce module.'];
  }
}