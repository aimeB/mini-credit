import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AgenceService } from '../../../employes/services/agence.service';
import { SiteService } from '../../../membres/services/site.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

@Component({
  selector: 'app-organisation-home',
  standalone: true,
  imports: [CommonModule, RouterLink, WorkflowGuidanceBannerComponent],
  template: `
    <div class="mc-page-wide">

        <!-- En-tête -->
        <header class="mc-page-hero flex flex-wrap items-center justify-between gap-4">
          <div>
            <div class="flex items-center gap-3 mb-1">
              <a routerLink="/dashboard" class="text-sm text-white/75 hover:text-white">Dashboard</a>
              <span class="text-white/40">/</span>
              <span class="text-sm font-medium text-white">Organisation</span>
            </div>
            <h1 class="mc-page-title">Organisation</h1>
            <p class="mc-page-subtitle">Structure des agences et sites opérationnels</p>
          </div>
          <a routerLink="/dashboard"
             class="mc-btn bg-white/15 text-white ring-1 ring-white/30 hover:bg-white/25">
            Dashboard
          </a>
        </header>

        <div class="mb-6">
          <app-workflow-guidance-banner [guidance]="adminReferenceGuidance"></app-workflow-guidance-banner>
        </div>

        <!-- Compteurs rapides -->
        <div class="mc-kpi-grid">
          <div class="mc-kpi-card">
            <div>
              <p class="mc-kpi-value">{{ nbAgences }}</p>
              <p class="mc-kpi-label">Agence{{ nbAgences !== 1 ? 's' : '' }}</p>
            </div>
          </div>
          <div class="mc-kpi-card mc-kpi-card-success">
            <div>
              <p class="mc-kpi-value">{{ nbSites }}</p>
              <p class="mc-kpi-label">Site{{ nbSites !== 1 ? 's' : '' }}</p>
            </div>
          </div>
        </div>

        <!-- Cartes Agences / Sites -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">

          <!-- Carte Agences -->
          <div class="mc-panel">
            <div class="p-6">
              <div class="flex items-center gap-3 mb-4">
                <h2 class="text-xl font-bold text-gray-900">Agences</h2>
              </div>
              <p class="text-gray-500 text-sm mb-6">
                Localisations administratives : ville, commune, quartier.
                Chaque site est rattaché à une agence.
              </p>
              <div class="flex flex-col gap-2">
                <a routerLink="/agences"
                   class="mc-btn justify-between bg-blue-50 text-blue-700 hover:bg-blue-100">
                  <span>Voir la liste des agences</span>
                  <svg class="w-4 h-4 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                  </svg>
                </a>
                <a routerLink="/agences/nouveau"
                   class="mc-btn justify-between bg-white text-blue-700 ring-1 ring-blue-200 hover:bg-blue-50">
                  <span>Créer une agence</span>
                  <svg class="w-4 h-4 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                  </svg>
                </a>
              </div>
            </div>
          </div>

          <!-- Carte Sites -->
          <div class="mc-panel">
            <div class="p-6">
              <div class="flex items-center gap-3 mb-4">
                <h2 class="text-xl font-bold text-gray-900">Sites</h2>
              </div>
              <p class="text-gray-500 text-sm mb-6">
                Zones opérationnelles terrain. Chaque site appartient à une agence
                et accueille les membres et agents terrain.
              </p>
              <div class="flex flex-col gap-2">
                <a routerLink="/sites"
                   class="mc-btn justify-between bg-emerald-50 text-emerald-700 hover:bg-emerald-100">
                  <span>Voir la liste des sites</span>
                  <svg class="w-4 h-4 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                  </svg>
                </a>
                <a routerLink="/sites/nouveau"
                   class="mc-btn justify-between bg-white text-emerald-700 ring-1 ring-emerald-200 hover:bg-emerald-50">
                  <span>Créer un site</span>
                  <svg class="w-4 h-4 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                  </svg>
                </a>
              </div>
            </div>
          </div>
        </div>

        <!-- Rappel du parcours de démarrage -->
        <div class="mc-state mc-state-warning">
          <div class="flex items-start gap-3 mb-4">
            <div>
              <h3 class="font-bold text-amber-900">Parcours de démarrage recommandé</h3>
              <p class="text-amber-700 text-sm">Pour une nouvelle installation, suivez ces étapes dans l'ordre.</p>
            </div>
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-5 gap-3">
            <a routerLink="/agences/nouveau"
               class="mc-btn flex-col bg-white text-center text-amber-800 ring-1 ring-amber-200 hover:bg-amber-50">
              <div class="font-bold text-sm">1</div>
              <p class="font-semibold text-gray-900 text-xs">Agence</p>
            </a>
            <a routerLink="/sites/nouveau"
               class="mc-btn flex-col bg-white text-center text-blue-800 ring-1 ring-blue-200 hover:bg-blue-50">
              <div class="font-bold text-sm">2</div>
              <p class="font-semibold text-gray-900 text-xs">Site</p>
            </a>
            <a routerLink="/employes/nouveau"
               class="mc-btn flex-col bg-white text-center text-fuchsia-800 ring-1 ring-fuchsia-200 hover:bg-fuchsia-50">
              <div class="font-bold text-sm">3</div>
              <p class="font-semibold text-gray-900 text-xs">Employé</p>
            </a>
            <a routerLink="/utilisateurs/nouveau"
               class="mc-btn flex-col bg-white text-center text-emerald-800 ring-1 ring-emerald-200 hover:bg-emerald-50">
              <div class="font-bold text-sm">4</div>
              <p class="font-semibold text-gray-900 text-xs">Utilisateur</p>
            </a>
            <a routerLink="/admin/agents/nouveau"
               class="mc-btn flex-col bg-white text-center text-orange-800 ring-1 ring-orange-200 hover:bg-orange-50">
              <div class="font-bold text-sm">5</div>
              <p class="font-semibold text-gray-900 text-xs">Agent Terrain</p>
            </a>
          </div>
        </div>
    </div>
  `
})
export class OrganisationHomeComponent implements OnInit {
  private agenceService = inject(AgenceService);
  private siteService = inject(SiteService);

  nbAgences = 0;
  nbSites = 0;

  readonly adminReferenceGuidance: WorkflowGuidance = {
    title: 'Referentiel administratif',
    message: 'Cette page contient des donnees de base utilisees par les workflows 3N. Toute modification doit etre faite avec prudence car elle peut influencer les membres, les operations, les rapports, le controle interne ou l audit.',
    currentStep: 'Donnee referentielle consultable ou modifiable',
    nextStep: 'Utilisation dans les workflows metier',
    expectedRole: 'Role autorise selon les droits existants',
    expectedAction: 'Consulter ou mettre a jour la donnee selon les droits existants',
    severity: 'warning',
    canCurrentUserAct: true
  };

  ngOnInit(): void {
    this.agenceService.getAll().subscribe({
      next: (a) => { this.nbAgences = a.length; }
    });
    this.siteService.getAll().subscribe({
      next: (s) => { this.nbSites = s.length; }
    });
  }
}
