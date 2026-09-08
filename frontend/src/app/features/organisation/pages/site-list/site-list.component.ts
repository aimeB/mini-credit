import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { SiteService } from '../../../membres/services/site.service';
import { SiteResponse } from '../../../membres/models/site-response';
import { AgenceService } from '../../../employes/services/agence.service';
import { AgenceResponse } from '../../models/agence-response';
import { AdminAgentTerrainService } from '../../../../admin/agent-terrain/services/admin-agent-terrain.service';
import { AgentTerrainResponse } from '../../../../admin/agent-terrain/models/agent-terrain.model';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

@Component({
  selector: 'app-site-list',
  standalone: true,
  imports: [CommonModule, RouterLink, WorkflowGuidanceBannerComponent],
  templateUrl: './site-list.component.html'
})
export class SiteListComponent implements OnInit {
  private siteService = inject(SiteService);
  private agenceService = inject(AgenceService);
  private agentService = inject(AdminAgentTerrainService);

  sites: SiteResponse[] = [];
  agences: AgenceResponse[] = [];
  agents: AgentTerrainResponse[] = [];
  loading = false;
  error = '';
  successMessage = '';

  readonly globalGuidance: WorkflowGuidance = {
    title: 'Gestion des sites',
    message: 'Cette page permet de gerer les sites suivis par les agents terrain. Les sites servent a rattacher les membres, organiser les collectes, suivre l activite terrain et produire les rapports. Un membre doit etre rattache a un site.',
    currentStep: 'Site consultable ou modifiable',
    nextStep: 'Affectation membres / agents / suivi terrain',
    expectedRole: 'Gestionnaire / Chef de Bureau selon les droits existants',
    expectedAction: 'Creer, verifier ou mettre a jour les sites selon les droits existants',
    severity: 'info',
    canCurrentUserAct: true,
    blockedReason: 'Un membre ne doit pas etre cree sans site de rattachement.'
  };

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.error = '';
    forkJoin({
      sites: this.siteService.getAll(),
      agences: this.agenceService.getAll(),
      agents: this.agentService.getAll()
    }).pipe(finalize(() => { this.loading = false; }))
      .subscribe({
        next: ({ sites, agences, agents }) => {
          this.sites = sites;
          this.agences = agences;
          this.agents = agents;
        },
        error: () => { this.error = 'Erreur lors du chargement des données'; }
      });
  }

  getNomAgence(agenceId: number): string {
    return this.agences.find(a => a.id === agenceId)?.nomAgence ?? `Agence #${agenceId}`;
  }

  /** Retourne les agents actifs affectés à ce site (via site principal ou sites additionnels) */
  getAgentsDuSite(siteId: number): AgentTerrainResponse[] {
    return this.agents.filter(a =>
      a.actif && (a.siteId === siteId || (a.siteIds ?? []).includes(siteId))
    );
  }

  getAgentResume(siteId: number): string {
    const agentsDuSite = this.getAgentsDuSite(siteId);
    if (agentsDuSite.length === 0) return '';
    if (agentsDuSite.length === 1) return agentsDuSite[0].nomCompletUtilisateur;
    return `${agentsDuSite.length} agents`;
  }

  getGestionnaireResume(siteId: number): string {
    const agentsDuSite = this.getAgentsDuSite(siteId);
    const gestionnaires = [...new Set(
      agentsDuSite
        .filter(a => a.gestionnaireNomComplet)
        .map(a => a.gestionnaireNomComplet!)
    )];
    if (gestionnaires.length === 0) return '';
    if (gestionnaires.length === 1) return gestionnaires[0];
    return `${gestionnaires.length} gestionnaires`;
  }

  desactiver(site: SiteResponse): void {
    if (!confirm(`Désactiver le site "${site.nomSite}" ?`)) return;
    this.siteService.deactivate(site.id).subscribe({
      next: () => {
        this.successMessage = `Site "${site.nomSite}" désactivé.`;
        this.charger();
        setTimeout(() => { this.successMessage = ''; }, 3000);
      },
      error: () => { this.error = 'Erreur lors de la désactivation'; }
    });
  }
}

