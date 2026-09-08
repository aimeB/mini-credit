import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AdminAgentTerrainService } from '../../services/admin-agent-terrain.service';
import { AgentTerrainResponse } from '../../models/agent-terrain.model';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

@Component({
  selector: 'app-agent-list',
  standalone: true,
  imports: [CommonModule, RouterModule, WorkflowGuidanceBannerComponent],
  template: `
    <div class="mc-page-wide">
      <header class="mc-page-hero flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 class="mc-page-title">Gestion des Agents Terrain</h1>
          <p class="mc-page-subtitle">Affectations terrain, sites principaux et gestionnaires liés.</p>
        </div>
        <button class="mc-btn mc-btn-primary" type="button" (click)="onCreateNew()">Nouvel Agent</button>
      </header>

      <div class="mb-3">
        <app-workflow-guidance-banner [guidance]="globalGuidance"></app-workflow-guidance-banner>
      </div>

      <div *ngIf="isLoading" class="mc-state mc-state-info text-center">
        Chargement des agents...
      </div>

      <div *ngIf="!isLoading && agents.length === 0" class="mc-state mc-state-warning">
        Aucun agent terrain trouve.
        <button type="button" (click)="onCreateNew()" class="mc-btn bg-amber-600 text-white hover:bg-amber-700 ml-2">Creer le premier agent</button>
      </div>

      <div *ngIf="!isLoading && agents.length > 0" class="mc-table-wrap">
        <table class="mc-table mc-table-admin w-full">
          <colgroup>
            <col class="w-[10%]">
            <col class="w-[12%]">
            <col class="w-[18%]">
            <col class="w-[15%]">
            <col class="w-[15%]">
            <col class="w-[10%]">
            <col class="w-[8%]">
            <col class="w-[12%]">
          </colgroup>
          <thead>
            <tr>
              <th>Matricule</th>
              <th>Utilisateur</th>
              <th>Nom Complet</th>
              <th>Site Principal</th>
              <th>Gestionnaire</th>
              <th>Date Affectation</th>
              <th>Statut</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let agent of agents">
              <td class="font-mono text-[11px] font-semibold text-blue-600 break-all">{{ agent.matricule }}</td>
              <td class="break-all"><code class="rounded bg-slate-100 px-1 py-0.5 text-[11px]">{{ agent.username }}</code></td>
              <td class="break-words font-medium text-slate-900" [title]="agent.nomCompletUtilisateur">{{ agent.nomCompletUtilisateur }}</td>
              <td class="mc-cell-muted break-words" [title]="agent.sitePrincipalNom || ''">{{ agent.sitePrincipalNom || '—' }}</td>
              <td class="mc-cell-muted break-words" [title]="agent.gestionnaireNomComplet || ''">{{ agent.gestionnaireNomComplet || '—' }}</td>
              <td>
                <small *ngIf="agent.dateAffectation" class="mc-date-short" [title]="agent.dateAffectation">
                  {{ agent.dateAffectation | date: 'dd/MM/yyyy' }}
                </small>
                <span *ngIf="!agent.dateAffectation">—</span>
              </td>
              <td>
                <span *ngIf="agent.actif" class="mc-badge bg-green-100 text-green-800 px-2 py-0.5 text-[10px]">Actif</span>
                <span *ngIf="!agent.actif" class="mc-badge bg-red-100 text-red-800 px-2 py-0.5 text-[10px]">Inactif</span>
              </td>
              <td>
                <div class="mc-actions-compact">
                <button class="mc-btn bg-blue-50 text-blue-700 hover:bg-blue-100 mc-btn-compact" type="button" (click)="onEdit(agent.id)">
                  Modif.
                </button>
                <button class="mc-btn bg-red-50 text-red-700 hover:bg-red-100 mc-btn-compact" type="button" (click)="onDelete(agent.id)" [disabled]="isDeleting">
                  Suppr.
                </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <small class="mc-cell-muted block p-2">Total: {{ agents.length }} agent(s)</small>
      </div>
    </div>
  `,
  styles: []
})
export class AgentListComponent implements OnInit {
  agents: AgentTerrainResponse[] = [];
  isLoading = false;
  isDeleting = false;

  readonly globalGuidance: WorkflowGuidance = {
    title: 'Gestion des agents terrain',
    message: 'Cette page permet de suivre les agents terrain responsables du recrutement, des collectes, des remboursements collectes, des demandes de credit recueillies et de la remise des fonds au bureau. Les agents terrain sont lies aux sites, aux membres et aux recettes journalieres.',
    currentStep: 'Agent terrain consultable ou modifiable',
    nextStep: 'Affectation terrain / suivi des collectes',
    expectedRole: 'Gestionnaire / Chef de Bureau selon les droits existants',
    expectedAction: 'Affecter, suivre ou mettre a jour les informations de l agent selon les droits existants',
    severity: 'info',
    canCurrentUserAct: true,
    blockedReason: 'L Agent Terrain collecte et transmet, mais ne valide pas les operations financieres.'
  };

  constructor(
    private agentService: AdminAgentTerrainService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadAgents();
  }

  loadAgents(): void {
    this.isLoading = true;
    this.agentService.getAll().subscribe({
      next: (data) => {
        this.agents = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur chargement agents', err);
        this.isLoading = false;
      }
    });
  }

  onCreateNew(): void {
    this.router.navigate(['/admin/agents/nouveau']);
  }

  onEdit(agentId: number): void {
    this.router.navigate([`/admin/agents/${agentId}/edit`]);
  }

  onDelete(agentId: number): void {
    if (!confirm('Etes-vous sur de vouloir supprimer cet agent?')) {
      return;
    }
    this.isDeleting = true;
    this.agentService.delete(agentId).subscribe({
      next: () => {
        this.loadAgents();
        this.isDeleting = false;
      },
      error: (err) => {
        console.error('Erreur suppression', err);
        this.isDeleting = false;
      }
    });
  }
}