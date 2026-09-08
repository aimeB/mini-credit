import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { SiteService } from '../../../membres/services/site.service';
import { SiteResponse } from '../../../membres/models/site-response';
import { AgentTerrainResponse } from '../../../../admin/agent-terrain/models/agent-terrain.model';

@Component({
  selector: 'app-site-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 py-8 px-4 sm:px-6 lg:px-8">
      <div class="max-w-4xl mx-auto">

        <!-- Fil d'Ariane -->
        <div class="flex items-center gap-2 text-sm text-gray-500 mb-6">
          <a routerLink="/dashboard" class="hover:text-gray-700">Dashboard</a>
          <span>/</span>
          <a routerLink="/organisation" class="hover:text-gray-700">Organisation</a>
          <span>/</span>
          <a routerLink="/sites" class="hover:text-gray-700">Sites</a>
          <span>/</span>
          <span class="text-gray-800 font-medium">{{ site?.nomSite || 'Détail' }}</span>
        </div>

        <!-- Actions -->
        <div class="flex flex-wrap items-center justify-between gap-3 mb-6">
          <h1 class="text-2xl font-bold text-gray-900">Détail Site</h1>
          <div class="flex flex-wrap gap-2">
            <a routerLink="/sites"
               class="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 font-medium text-sm">
              ← Liste sites
            </a>
            @if (site?.agenceId) {
              <a [routerLink]="['/agences', site!.agenceId]"
                 class="px-4 py-2 bg-sky-100 text-sky-700 rounded-lg hover:bg-sky-200 font-medium text-sm">
                ↑ Agence
              </a>
            }
            @if (site) {
              <a [routerLink]="['/sites', site.id, 'modifier']"
                 class="px-4 py-2 bg-amber-500 text-white rounded-lg hover:bg-amber-600 font-medium text-sm">
                Modifier
              </a>
              @if (site.actif) {
                <button (click)="desactiver()"
                        class="px-4 py-2 bg-red-100 text-red-700 rounded-lg hover:bg-red-200 font-medium text-sm">
                  Désactiver
                </button>
              }
            }
          </div>
        </div>

        @if (successMessage) {
          <div class="mb-4 p-3 bg-emerald-50 border-l-4 border-emerald-500 rounded-lg text-emerald-700 text-sm">{{ successMessage }}</div>
        }

        @if (loading) {
          <div class="text-center py-12 text-gray-500">Chargement...</div>
        } @else if (error) {
          <div class="p-4 bg-red-50 border-l-4 border-red-500 rounded-lg text-red-700">{{ error }}</div>
        } @else if (site) {

          <!-- Informations site -->
          <div class="bg-white rounded-xl shadow-md p-6 mb-6">
            <div class="flex items-center gap-3 mb-4">
              <span class="font-mono bg-slate-100 px-3 py-1.5 rounded text-sm font-bold">{{ site.codeSite }}</span>
              @if (site.actif) {
                <span class="px-2 py-1 bg-green-100 text-green-800 text-xs font-semibold rounded-full">Actif</span>
              } @else {
                <span class="px-2 py-1 bg-red-100 text-red-800 text-xs font-semibold rounded-full">Inactif</span>
              }
            </div>
            <h2 class="text-2xl font-bold text-gray-900 mb-5">{{ site.nomSite }}</h2>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
              <div>
                <span class="text-gray-500 text-xs uppercase tracking-wide">Agence</span>
                <p class="font-semibold text-gray-900 mt-1">{{ site.nomAgence || 'N/A' }}</p>
                @if (site.communeAgence || site.villeAgence) {
                  <p class="text-xs text-gray-400 mt-0.5">{{ site.communeAgence }}, {{ site.villeAgence }}</p>
                }
              </div>
              <div>
                <span class="text-gray-500 text-xs uppercase tracking-wide">Zone opérationnelle</span>
                <p class="font-medium text-gray-900 mt-1">{{ site.zone || '—' }}</p>
              </div>
            </div>
          </div>

          <!-- Section : Agents Terrain affectés -->
          <div class="bg-white rounded-xl shadow-md overflow-hidden">
            <div class="flex items-center justify-between px-6 py-4 border-b border-gray-100">
              <div class="flex items-center gap-3">
                <span class="text-xl">👷</span>
                <h3 class="text-lg font-bold text-gray-900">
                  Agents Terrain affectés
                  @if (!loadingAgents) {
                    <span class="ml-2 px-2 py-0.5 bg-orange-100 text-orange-700 text-xs rounded-full font-medium">{{ agents.length }}</span>
                  }
                </h3>
              </div>
              <a [routerLink]="['/admin/agents/nouveau']" [queryParams]="{siteId: site!.id, returnTo: site!.id}"
                 class="px-3 py-1.5 bg-orange-500 text-white rounded-lg hover:bg-orange-600 font-medium text-xs">
                + Nouvel agent
              </a>
            </div>

            @if (loadingAgents) {
              <div class="text-center py-8 text-gray-400 text-sm">Chargement des agents...</div>
            } @else if (agents.length === 0) {
              <div class="text-center py-10 px-6">
                <div class="text-4xl mb-3">👷</div>
                <p class="text-gray-500 text-sm font-medium">Aucun agent terrain affecté à ce site.</p>
                <p class="text-gray-400 text-xs mt-1">Créez un agent terrain et affectez-le à ce site.</p>
                <a [routerLink]="['/admin/agents/nouveau']" [queryParams]="{siteId: site!.id, returnTo: site!.id}"
                   class="mt-4 inline-flex px-4 py-2 bg-orange-500 text-white rounded-lg text-sm font-medium hover:bg-orange-600">
                  Créer un agent terrain
                </a>
              </div>
            } @else {
              <div class="divide-y divide-gray-100">
                @for (agent of agents; track agent.id) {
                  <div class="px-6 py-4 hover:bg-gray-50 transition-colors">
                    <div class="flex flex-col sm:flex-row sm:items-center gap-4">

                      <!-- Agent Terrain -->
                      <div class="flex-1">
                        <div class="flex items-center gap-2 mb-1">
                          <span class="text-lg">👤</span>
                          <span class="font-semibold text-gray-900">{{ agent.nomCompletUtilisateur }}</span>
                          @if (agent.actif) {
                            <span class="px-1.5 py-0.5 bg-green-100 text-green-700 text-xs rounded font-medium">Actif</span>
                          } @else {
                            <span class="px-1.5 py-0.5 bg-red-100 text-red-700 text-xs rounded font-medium">Inactif</span>
                          }
                        </div>
                        <div class="flex flex-wrap items-center gap-3 text-sm text-gray-500">
                          <span class="font-mono bg-slate-100 px-2 py-0.5 rounded text-xs">{{ agent.matricule }}</span>
                          @if (agent.dateAffectation) {
                            <span class="text-xs">Depuis : {{ agent.dateAffectation | date:'dd/MM/yyyy' }}</span>
                          }
                        </div>
                      </div>

                      <!-- Gestionnaire -->
                      @if (agent.gestionnaireNomComplet) {
                        <div class="sm:text-right">
                          <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">Gestionnaire</p>
                          <div class="flex items-center gap-2 sm:justify-end">
                            <span class="text-sm">🧑‍💼</span>
                            <span class="font-medium text-gray-800 text-sm">{{ agent.gestionnaireNomComplet }}</span>
                          </div>
                        </div>
                      } @else {
                        <div class="sm:text-right">
                          <span class="text-xs text-gray-400 italic">Pas de gestionnaire</span>
                        </div>
                      }

                    </div>
                  </div>
                }
              </div>
            }
          </div>

        }
      </div>
    </div>
  `
})
export class SiteDetailComponent implements OnInit {
  private siteService = inject(SiteService);
  private route = inject(ActivatedRoute);

  site: SiteResponse | null = null;
  agents: AgentTerrainResponse[] = [];
  loading = false;
  loadingAgents = false;
  error = '';
  successMessage = '';

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { this.error = 'ID manquant'; return; }
    this.loading = true;
    this.siteService.getById(+id).subscribe({
      next: (s) => {
        this.site = s;
        this.loading = false;
        this.chargerAgents(+id);
      },
      error: () => { this.error = 'Site introuvable'; this.loading = false; }
    });
  }

  chargerAgents(siteId: number): void {
    this.loadingAgents = true;
    this.siteService.getAgentsTerrainBySite(siteId).subscribe({
      next: (a) => { this.agents = a; this.loadingAgents = false; },
      error: () => { this.loadingAgents = false; }
    });
  }

  desactiver(): void {
    if (!this.site) return;
    if (!confirm(`Désactiver le site "${this.site.nomSite}" ?`)) return;
    this.siteService.deactivate(this.site.id).subscribe({
      next: () => {
        this.successMessage = `Site désactivé.`;
        if (this.site) { this.site = { ...this.site, actif: false }; }
        setTimeout(() => { this.successMessage = ''; }, 3000);
      },
      error: () => { this.error = 'Erreur lors de la désactivation'; }
    });
  }
}

