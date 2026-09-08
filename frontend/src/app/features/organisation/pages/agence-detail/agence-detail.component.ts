import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AgenceService } from '../../../employes/services/agence.service';
import { EmployeService } from '../../../employes/services/employe.service';
import { AgenceResponse } from '../../models/agence-response';
import { SiteResponse } from '../../../membres/models/site-response';
import { EmployeResponse } from '../../../employes/models/employe-response';
import { FONCTION_LABELS, FonctionEmploye } from '../../../employes/models/fonction-employe';
import { AgentTerrainResponse } from '../../../../admin/agent-terrain/models/agent-terrain.model';

@Component({
  selector: 'app-agence-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 py-8 px-4 sm:px-6 lg:px-8">
      <div class="max-w-4xl mx-auto">

        <!-- Navigation fil d'Ariane -->
        <div class="flex items-center gap-2 text-sm text-gray-500 mb-6">
          <a routerLink="/dashboard" class="hover:text-gray-700">Dashboard</a>
          <span>/</span>
          <a routerLink="/organisation" class="hover:text-gray-700">Organisation</a>
          <span>/</span>
          <a routerLink="/agences" class="hover:text-gray-700">Agences</a>
          <span>/</span>
          <span class="text-gray-800 font-medium">{{ agence?.nomAgence || 'Détail' }}</span>
        </div>

        <!-- Boutons d'action -->
        <div class="flex flex-wrap items-center justify-between gap-3 mb-6">
          <h1 class="text-2xl font-bold text-gray-900">Détail Agence</h1>
          <div class="flex flex-wrap gap-2">
            <a routerLink="/agences"
               class="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 font-medium text-sm">
              ← Liste agences
            </a>
            @if (agence) {
              <a [routerLink]="['/agences', agence.id, 'modifier']"
                 class="px-4 py-2 bg-amber-500 text-white rounded-lg hover:bg-amber-600 font-medium text-sm">
                Modifier
              </a>
              <a [routerLink]="['/sites/nouveau']" [queryParams]="{agenceId: agence.id}"
                 class="px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 font-medium text-sm">
                + Créer un site
              </a>
              @if (agence.actif) {
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
        } @else if (agence) {

          <!-- Informations agence -->
          <div class="bg-white rounded-xl shadow-md p-6 mb-6">
            <div class="flex items-center gap-3 mb-4">
              <span class="font-mono bg-slate-100 px-3 py-1.5 rounded text-sm font-bold">{{ agence.codeAgence }}</span>
              @if (agence.actif) {
                <span class="px-2 py-1 bg-green-100 text-green-800 text-xs font-semibold rounded-full">Actif</span>
              } @else {
                <span class="px-2 py-1 bg-red-100 text-red-800 text-xs font-semibold rounded-full">Inactif</span>
              }
            </div>
            <h2 class="text-2xl font-bold text-gray-900 mb-5">{{ agence.nomAgence }}</h2>
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-4 text-sm">
              <div>
                <span class="text-gray-500 text-xs uppercase tracking-wide">Ville</span>
                <p class="font-medium text-gray-900 mt-1">{{ agence.ville || '—' }}</p>
              </div>
              <div>
                <span class="text-gray-500 text-xs uppercase tracking-wide">Commune</span>
                <p class="font-medium text-gray-900 mt-1">{{ agence.commune || '—' }}</p>
              </div>
              <div>
                <span class="text-gray-500 text-xs uppercase tracking-wide">Quartier</span>
                <p class="font-medium text-gray-900 mt-1">{{ agence.quartier || '—' }}</p>
              </div>
              <div>
                <span class="text-gray-500 text-xs uppercase tracking-wide">Téléphone</span>
                <p class="font-medium text-gray-900 mt-1">{{ agence.telephone || '—' }}</p>
              </div>
              @if (agence.adresse) {
                <div class="col-span-2">
                  <span class="text-gray-500 text-xs uppercase tracking-wide">Adresse</span>
                  <p class="font-medium text-gray-900 mt-1">{{ agence.adresse }}</p>
                </div>
              }
              @if (agence.reference) {
                <div class="col-span-2">
                  <span class="text-gray-500 text-xs uppercase tracking-wide">Référence</span>
                  <p class="font-medium text-gray-900 mt-1">{{ agence.reference }}</p>
                </div>
              }
            </div>
          </div>

          <!-- Section : Sites rattachés à cette agence -->
          <div class="bg-white rounded-xl shadow-md overflow-hidden mb-6">
            <div class="flex items-center justify-between px-6 py-4 border-b border-gray-100">
              <div class="flex items-center gap-3">
                <span class="text-xl">📍</span>
                <h3 class="text-lg font-bold text-gray-900">
                  Sites rattachés
                  @if (!loadingSites) {
                    <span class="ml-2 px-2 py-0.5 bg-emerald-100 text-emerald-700 text-xs rounded-full font-medium">{{ sites.length }}</span>
                  }
                </h3>
              </div>
              <a [routerLink]="['/sites/nouveau']"
                 class="px-3 py-1.5 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 font-medium text-xs">
                + Nouveau site
              </a>
            </div>

            @if (loadingSites) {
              <div class="text-center py-8 text-gray-400 text-sm">Chargement des sites...</div>
            } @else if (sites.length === 0) {
              <div class="text-center py-10 px-6">
                <div class="text-4xl mb-3">📍</div>
                <p class="text-gray-500 text-sm">Aucun site rattaché à cette agence.</p>
                <a [routerLink]="['/sites/nouveau']"
                   class="mt-4 inline-flex px-4 py-2 bg-emerald-600 text-white rounded-lg text-sm font-medium hover:bg-emerald-700">
                  Créer le premier site
                </a>
              </div>
            } @else {
              <div class="overflow-x-auto">
                <table class="w-full text-sm">
                  <thead class="bg-slate-50 text-gray-500 text-xs uppercase tracking-wide">
                    <tr>
                      <th class="px-5 py-3 text-left font-semibold">Code</th>
                      <th class="px-5 py-3 text-left font-semibold">Nom Site</th>
                      <th class="px-5 py-3 text-left font-semibold">Zone</th>
                      <th class="px-5 py-3 text-left font-semibold">Statut</th>
                      <th class="px-5 py-3 text-left font-semibold">Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (site of sites; track site.id) {
                      <tr class="border-t border-gray-100 hover:bg-gray-50 transition-colors">
                        <td class="px-5 py-3">
                          <span class="font-mono bg-slate-100 px-2 py-1 rounded text-xs font-bold">{{ site.codeSite }}</span>
                        </td>
                        <td class="px-5 py-3 font-semibold text-gray-900">{{ site.nomSite }}</td>
                        <td class="px-5 py-3 text-gray-600 text-xs max-w-xs truncate">{{ site.zone || '—' }}</td>
                        <td class="px-5 py-3">
                          @if (site.actif) {
                            <span class="px-2 py-1 bg-green-100 text-green-800 text-xs font-semibold rounded-full">Actif</span>
                          } @else {
                            <span class="px-2 py-1 bg-red-100 text-red-800 text-xs font-semibold rounded-full">Inactif</span>
                          }
                        </td>
                        <td class="px-5 py-3">
                          <a [routerLink]="['/sites', site.id]"
                             class="px-2 py-1 text-xs bg-blue-50 text-blue-700 rounded hover:bg-blue-100 font-medium">
                            Voir Site →
                          </a>
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          </div>

          <!-- ===== Compteurs résumé personnel ===== -->
          @if (!loadingPersonnel) {
            <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 mb-6">
              <div class="bg-white rounded-xl shadow-sm p-4 text-center">
                <div class="text-2xl font-bold text-gray-900">{{ personnel.length }}</div>
                <div class="text-xs text-gray-500 mt-1 uppercase tracking-wide">Employés actifs</div>
              </div>
              <div class="bg-white rounded-xl shadow-sm p-4 text-center">
                <div class="text-2xl font-bold text-indigo-700">{{ chefsBureau.length }}</div>
                <div class="text-xs text-gray-500 mt-1 uppercase tracking-wide">Chef Bureau</div>
              </div>
              <div class="bg-white rounded-xl shadow-sm p-4 text-center">
                <div class="text-2xl font-bold text-violet-700">{{ gestionnaires.length }}</div>
                <div class="text-xs text-gray-500 mt-1 uppercase tracking-wide">Gestionnaires</div>
              </div>
              <div class="bg-white rounded-xl shadow-sm p-4 text-center">
                <div class="text-2xl font-bold text-orange-700">{{ agentsTerrain.length }}</div>
                <div class="text-xs text-gray-500 mt-1 uppercase tracking-wide">Agents Terrain</div>
              </div>
              <div class="bg-white rounded-xl shadow-sm p-4 text-center">
                <div class="text-2xl font-bold text-teal-700">{{ controleurs.length }}</div>
                <div class="text-xs text-gray-500 mt-1 uppercase tracking-wide">Contrôleurs</div>
              </div>
              <div class="bg-white rounded-xl shadow-sm p-4 text-center">
                <div class="text-2xl font-bold text-amber-700">{{ caissiers.length }}</div>
                <div class="text-xs text-gray-500 mt-1 uppercase tracking-wide">Caissiers</div>
              </div>
            </div>
          }

          <!-- ===== Section Personnel ===== -->
          <div class="bg-white rounded-xl shadow-md overflow-hidden">
            <div class="flex items-center justify-between px-6 py-4 border-b border-gray-100">
              <div class="flex items-center gap-3">
                <span class="text-xl">👥</span>
                <h3 class="text-lg font-bold text-gray-900">
                  Personnel affecté à cette agence
                  @if (!loadingPersonnel) {
                    <span class="ml-2 px-2 py-0.5 bg-blue-100 text-blue-700 text-xs rounded-full font-medium">{{ personnel.length }}</span>
                  }
                </h3>
              </div>
              <div class="flex gap-2">
                <button (click)="ouvrirModalAffectation()"
                        class="px-3 py-1.5 bg-violet-600 text-white rounded-lg hover:bg-violet-700 font-medium text-xs">
                  + Affecter un employé existant
                </button>
                <a [routerLink]="['/employes/nouveau']" [queryParams]="{agenceId: agence!.id}"
                   class="px-3 py-1.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium text-xs">
                  + Nouvel employé
                </a>
              </div>
            </div>

            @if (loadingPersonnel) {
              <div class="text-center py-8 text-gray-400 text-sm">Chargement du personnel...</div>
            } @else if (personnel.length === 0) {
              <div class="text-center py-10 px-6">
                <div class="text-4xl mb-3">👤</div>
                <p class="text-gray-500 text-sm font-medium">Aucun employé actif rattaché à cette agence.</p>
                <a [routerLink]="['/employes/nouveau']" [queryParams]="{agenceId: agence!.id}"
                   class="mt-4 inline-flex px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700">
                  Créer le premier employé
                </a>
              </div>
            } @else {

              <!-- Groupe : Chef de Bureau -->
              @if (chefsBureau.length > 0) {
                <div class="border-t border-gray-100">
                  <div class="px-6 py-3 bg-indigo-50 flex items-center gap-2">
                    <span class="text-sm font-bold text-indigo-800 uppercase tracking-wide">🏢 Chef de Bureau</span>
                    <span class="ml-auto px-2 py-0.5 bg-indigo-100 text-indigo-700 text-xs rounded-full">{{ chefsBureau.length }}</span>
                  </div>
                  @for (emp of chefsBureau; track emp.id) {
                    <div class="px-6 py-4 hover:bg-gray-50 transition-colors border-t border-gray-50">
                      <div class="flex flex-col sm:flex-row sm:items-center gap-3">
                        <div class="flex-1 min-w-0">
                          <div class="flex flex-wrap items-center gap-2">
                            <span class="font-mono bg-slate-100 px-2 py-0.5 rounded text-xs font-bold text-slate-700">{{ emp.matricule }}</span>
                            <span class="font-semibold text-gray-900 text-sm">{{ emp.nomComplet }}</span>
                            @if (emp.utilisateurId) {
                              <span class="px-1.5 py-0.5 bg-green-100 text-green-700 text-xs rounded font-medium">✓ Compte</span>
                            } @else {
                              <span class="px-1.5 py-0.5 bg-gray-100 text-gray-500 text-xs rounded">Sans compte</span>
                            }
                          </div>
                          <div class="flex flex-wrap gap-3 mt-1 text-xs text-gray-500">
                            @if (emp.telephone) { <span>📞 {{ emp.telephone }}</span> }
                            @if (emp.nomSite) { <span>📍 {{ emp.nomSite }}</span> }
                          </div>
                        </div>
                        <a [routerLink]="['/employes', emp.id]" class="shrink-0 px-2 py-1 text-xs bg-blue-50 text-blue-700 rounded hover:bg-blue-100 font-medium">Voir →</a>
                      </div>
                    </div>
                  }
                </div>
              }

              <!-- Groupe : Gestionnaires -->
              @if (gestionnaires.length > 0) {
                <div class="border-t border-gray-100">
                  <div class="px-6 py-3 bg-violet-50 flex items-center gap-2">
                    <span class="text-sm font-bold text-violet-800 uppercase tracking-wide">🧑‍💼 Gestionnaires</span>
                    <span class="ml-auto px-2 py-0.5 bg-violet-100 text-violet-700 text-xs rounded-full">{{ gestionnaires.length }}</span>
                  </div>
                  @for (emp of gestionnaires; track emp.id) {
                    <div class="px-6 py-4 hover:bg-gray-50 transition-colors border-t border-gray-50">
                      <div class="flex flex-col sm:flex-row sm:items-center gap-3">
                        <div class="flex-1 min-w-0">
                          <div class="flex flex-wrap items-center gap-2">
                            <span class="font-mono bg-slate-100 px-2 py-0.5 rounded text-xs font-bold text-slate-700">{{ emp.matricule }}</span>
                            <span class="font-semibold text-gray-900 text-sm">{{ emp.nomComplet }}</span>
                            @if (emp.utilisateurId) {
                              <span class="px-1.5 py-0.5 bg-green-100 text-green-700 text-xs rounded font-medium">✓ Compte</span>
                            } @else {
                              <span class="px-1.5 py-0.5 bg-gray-100 text-gray-500 text-xs rounded">Sans compte</span>
                            }
                          </div>
                          <div class="flex flex-wrap gap-3 mt-1 text-xs text-gray-500">
                            @if (emp.telephone) { <span>📞 {{ emp.telephone }}</span> }
                            @if (emp.nomSite) { <span>📍 {{ emp.nomSite }}</span> }
                          </div>
                        </div>
                        <a [routerLink]="['/employes', emp.id]" class="shrink-0 px-2 py-1 text-xs bg-blue-50 text-blue-700 rounded hover:bg-blue-100 font-medium">Voir →</a>
                      </div>
                    </div>
                  }
                </div>
              }

              <!-- Groupe : Agents Terrain -->
              @if (agentsTerrain.length > 0) {
                <div class="border-t border-gray-100">
                  <div class="px-6 py-3 bg-orange-50 flex items-center gap-2">
                    <span class="text-sm font-bold text-orange-800 uppercase tracking-wide">👷 Agents Terrain</span>
                    <span class="ml-auto px-2 py-0.5 bg-orange-100 text-orange-700 text-xs rounded-full">{{ agentsTerrain.length }}</span>
                  </div>
                  @for (emp of agentsTerrain; track emp.id) {
                    <div class="px-6 py-4 hover:bg-gray-50 transition-colors border-t border-gray-50">
                      <div class="flex flex-col sm:flex-row sm:items-center gap-3">
                        <div class="flex-1 min-w-0">
                          <div class="flex flex-wrap items-center gap-2">
                            <span class="font-mono bg-slate-100 px-2 py-0.5 rounded text-xs font-bold text-slate-700">{{ emp.matricule }}</span>
                            <span class="font-semibold text-gray-900 text-sm">{{ emp.nomComplet }}</span>
                            @if (emp.utilisateurId) {
                              <span class="px-1.5 py-0.5 bg-green-100 text-green-700 text-xs rounded font-medium">✓ Compte</span>
                            } @else {
                              <span class="px-1.5 py-0.5 bg-gray-100 text-gray-500 text-xs rounded">Sans compte</span>
                            }
                          </div>
                          <div class="flex flex-wrap gap-3 mt-1 text-xs text-gray-500">
                            @if (emp.telephone) { <span>📞 {{ emp.telephone }}</span> }
                            @if (emp.nomSite) { <span>📍 {{ emp.nomSite }}</span> }
                          </div>
                        </div>
                        <a [routerLink]="['/employes', emp.id]" class="shrink-0 px-2 py-1 text-xs bg-blue-50 text-blue-700 rounded hover:bg-blue-100 font-medium">Voir →</a>
                      </div>
                    </div>
                  }
                </div>
              }

              <!-- Groupe : Contrôleurs -->
              @if (controleurs.length > 0) {
                <div class="border-t border-gray-100">
                  <div class="px-6 py-3 bg-teal-50 flex items-center gap-2">
                    <span class="text-sm font-bold text-teal-800 uppercase tracking-wide">🔍 Contrôleurs</span>
                    <span class="ml-auto px-2 py-0.5 bg-teal-100 text-teal-700 text-xs rounded-full">{{ controleurs.length }}</span>
                  </div>
                  @for (emp of controleurs; track emp.id) {
                    <div class="px-6 py-4 hover:bg-gray-50 transition-colors border-t border-gray-50">
                      <div class="flex flex-col sm:flex-row sm:items-center gap-3">
                        <div class="flex-1 min-w-0">
                          <div class="flex flex-wrap items-center gap-2">
                            <span class="font-mono bg-slate-100 px-2 py-0.5 rounded text-xs font-bold text-slate-700">{{ emp.matricule }}</span>
                            <span class="font-semibold text-gray-900 text-sm">{{ emp.nomComplet }}</span>
                            @if (emp.utilisateurId) {
                              <span class="px-1.5 py-0.5 bg-green-100 text-green-700 text-xs rounded font-medium">✓ Compte</span>
                            } @else {
                              <span class="px-1.5 py-0.5 bg-gray-100 text-gray-500 text-xs rounded">Sans compte</span>
                            }
                          </div>
                          <div class="flex flex-wrap gap-3 mt-1 text-xs text-gray-500">
                            @if (emp.telephone) { <span>📞 {{ emp.telephone }}</span> }
                            @if (emp.nomSite) { <span>📍 {{ emp.nomSite }}</span> }
                          </div>
                        </div>
                        <a [routerLink]="['/employes', emp.id]" class="shrink-0 px-2 py-1 text-xs bg-blue-50 text-blue-700 rounded hover:bg-blue-100 font-medium">Voir →</a>
                      </div>
                    </div>
                  }
                </div>
              }

              <!-- Groupe : Caissiers -->
              @if (caissiers.length > 0) {
                <div class="border-t border-gray-100">
                  <div class="px-6 py-3 bg-amber-50 flex items-center gap-2">
                    <span class="text-sm font-bold text-amber-800 uppercase tracking-wide">💰 Caissiers</span>
                    <span class="ml-auto px-2 py-0.5 bg-amber-100 text-amber-700 text-xs rounded-full">{{ caissiers.length }}</span>
                  </div>
                  @for (emp of caissiers; track emp.id) {
                    <div class="px-6 py-4 hover:bg-gray-50 transition-colors border-t border-gray-50">
                      <div class="flex flex-col sm:flex-row sm:items-center gap-3">
                        <div class="flex-1 min-w-0">
                          <div class="flex flex-wrap items-center gap-2">
                            <span class="font-mono bg-slate-100 px-2 py-0.5 rounded text-xs font-bold text-slate-700">{{ emp.matricule }}</span>
                            <span class="font-semibold text-gray-900 text-sm">{{ emp.nomComplet }}</span>
                            @if (emp.utilisateurId) {
                              <span class="px-1.5 py-0.5 bg-green-100 text-green-700 text-xs rounded font-medium">✓ Compte</span>
                            } @else {
                              <span class="px-1.5 py-0.5 bg-gray-100 text-gray-500 text-xs rounded">Sans compte</span>
                            }
                          </div>
                          <div class="flex flex-wrap gap-3 mt-1 text-xs text-gray-500">
                            @if (emp.telephone) { <span>📞 {{ emp.telephone }}</span> }
                            @if (emp.nomSite) { <span>📍 {{ emp.nomSite }}</span> }
                          </div>
                        </div>
                        <a [routerLink]="['/employes', emp.id]" class="shrink-0 px-2 py-1 text-xs bg-blue-50 text-blue-700 rounded hover:bg-blue-100 font-medium">Voir →</a>
                      </div>
                    </div>
                  }
                </div>
              }

              <!-- Groupe : Autres -->
              @if (autresPersonnel.length > 0) {
                <div class="border-t border-gray-100">
                  <div class="px-6 py-3 bg-gray-50 flex items-center gap-2">
                    <span class="text-sm font-bold text-gray-600 uppercase tracking-wide">👤 Autres</span>
                    <span class="ml-auto px-2 py-0.5 bg-gray-200 text-gray-600 text-xs rounded-full">{{ autresPersonnel.length }}</span>
                  </div>
                  @for (emp of autresPersonnel; track emp.id) {
                    <div class="px-6 py-4 hover:bg-gray-50 transition-colors border-t border-gray-50">
                      <div class="flex flex-col sm:flex-row sm:items-center gap-3">
                        <div class="flex-1 min-w-0">
                          <div class="flex flex-wrap items-center gap-2">
                            <span class="font-mono bg-slate-100 px-2 py-0.5 rounded text-xs font-bold text-slate-700">{{ emp.matricule }}</span>
                            <span class="font-semibold text-gray-900 text-sm">{{ emp.nomComplet }}</span>
                            @if (emp.fonction) {
                              <span class="px-1.5 py-0.5 bg-slate-100 text-slate-600 text-xs rounded">{{ FONCTION_LABELS[emp.fonction] }}</span>
                            }
                            @if (emp.utilisateurId) {
                              <span class="px-1.5 py-0.5 bg-green-100 text-green-700 text-xs rounded font-medium">✓ Compte</span>
                            }
                          </div>
                          <div class="flex flex-wrap gap-3 mt-1 text-xs text-gray-500">
                            @if (emp.telephone) { <span>📞 {{ emp.telephone }}</span> }
                            @if (emp.nomSite) { <span>📍 {{ emp.nomSite }}</span> }
                          </div>
                        </div>
                        <a [routerLink]="['/employes', emp.id]" class="shrink-0 px-2 py-1 text-xs bg-blue-50 text-blue-700 rounded hover:bg-blue-100 font-medium">Voir →</a>
                      </div>
                    </div>
                  }
                </div>
              }
            }
          </div>

          <!-- ===== Section : Anomalies d'affectation ===== -->
          @if (loadingAnomalies) {
            <div class="mt-6 text-center py-6 text-gray-400 text-sm">Vérification des anomalies...</div>
          } @else if (anomalies.length > 0) {
            <div class="bg-white rounded-xl shadow-md overflow-hidden mt-6 border-l-4 border-orange-400">
              <div class="flex items-center justify-between px-6 py-4 border-b border-orange-100 bg-orange-50">
                <div class="flex items-center gap-3">
                  <span class="text-xl">⚠️</span>
                  <div>
                    <h3 class="text-lg font-bold text-orange-800">Anomalies d'affectation</h3>
                    <p class="text-xs text-orange-600 mt-0.5">Agents terrain dont l'employé ou le gestionnaire n'appartient pas à cette agence</p>
                  </div>
                  <span class="ml-2 px-2 py-0.5 bg-orange-200 text-orange-800 text-xs rounded-full font-bold">{{ anomalies.length }}</span>
                </div>
              </div>
              @for (agent of anomalies; track agent.id) {
                <div class="px-6 py-5 border-t border-orange-50 hover:bg-orange-50 transition-colors">
                  <div class="flex flex-col sm:flex-row sm:items-start gap-4">
                    <div class="flex-1 min-w-0">
                      <!-- En-tête agent -->
                      <div class="flex flex-wrap items-center gap-2 mb-2">
                        <span class="font-mono bg-orange-100 px-2 py-0.5 rounded text-xs font-bold text-orange-800">{{ agent.matricule }}</span>
                        <span class="font-semibold text-gray-900">{{ agent.employeNomComplet || agent.nomCompletUtilisateur }}</span>
                        <span class="px-2 py-0.5 bg-orange-100 text-orange-700 text-xs rounded">Agent Terrain</span>
                        <span class="px-2 py-0.5 bg-slate-100 text-slate-600 text-xs rounded">Site : {{ agent.sitePrincipalNom }}</span>
                      </div>

                      <!-- Anomalie 1 : employé dans mauvaise agence -->
                      @if (agent.agentAgenceId && agent.agentAgenceId !== agence!.id) {
                        <div class="flex items-start gap-2 mb-1">
                          <span class="text-orange-500 mt-0.5 shrink-0">▸</span>
                          <p class="text-sm text-orange-700">
                            <strong>Employé rattaché à</strong> « {{ agent.agentAgenceNom }} »
                            au lieu de « {{ agence!.nomAgence }} ».
                          </p>
                        </div>
                      }

                      <!-- Anomalie 2 : gestionnaire dans mauvaise agence -->
                      @if (agent.gestionnaireId && agent.gestionnaireAgenceId && agent.gestionnaireAgenceId !== agence!.id) {
                        <div class="flex items-start gap-2">
                          <span class="text-orange-500 mt-0.5 shrink-0">▸</span>
                          <p class="text-sm text-orange-700">
                            <strong>Gestionnaire {{ agent.gestionnaireNomComplet }}</strong> rattaché à
                            « {{ agent.gestionnaireAgenceNom }} » au lieu de « {{ agence!.nomAgence }} ».
                          </p>
                        </div>
                      }
                    </div>

                    <!-- Bouton correction (uniquement si l'employé agent est dans la mauvaise agence) -->
                    @if (agent.agentAgenceId && agent.agentAgenceId !== agence!.id && agent.employeId) {
                      <button (click)="corrigerAnomalieAgent(agent)"
                              [disabled]="corrigerEnCours[agent.id]">
                        @if (corrigerEnCours[agent.id]) {
                          <span class="px-3 py-2 bg-gray-200 text-gray-500 rounded-lg text-xs font-medium cursor-not-allowed">En cours...</span>
                        } @else {
                          <span class="px-3 py-2 bg-orange-600 text-white rounded-lg text-xs font-medium hover:bg-orange-700 cursor-pointer">Corriger l'agence →</span>
                        }
                      </button>
                    }
                  </div>
                </div>
              }
            </div>
          }

        }

      </div>
    </div>

    <!-- ===== Modal : Affecter un employé existant ===== -->
    @if (showModalAffectation) {
      <div class="fixed inset-0 z-50 flex items-start justify-center pt-8 px-4"
           style="background:rgba(0,0,0,0.5)">
        <div class="bg-white rounded-2xl shadow-2xl w-full max-w-4xl max-h-[85vh] flex flex-col">

          <!-- En-tête modal -->
          <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 shrink-0">
            <div>
              <h2 class="text-lg font-bold text-gray-900">Affecter un employé existant</h2>
              <p class="text-sm text-gray-500 mt-0.5">
                Agence cible : <strong>{{ agence!.nomAgence }}</strong>
              </p>
            </div>
            <button (click)="fermerModalAffectation()"
                    class="text-gray-400 hover:text-gray-700 text-2xl leading-none font-bold">✕</button>
          </div>

          <!-- Corps modal (scrollable) -->
          <div class="overflow-y-auto flex-1 px-6 py-4">

            @if (loadingModalData) {
              <div class="text-center py-12 text-gray-400">Chargement des employés disponibles...</div>
            } @else if (employesAAfecter.length === 0) {
              <div class="text-center py-12">
                <div class="text-4xl mb-3">✅</div>
                <p class="text-gray-600 font-medium">Tous les employés actifs sont déjà rattachés à cette agence.</p>
              </div>
            } @else {
              <p class="text-sm text-gray-500 mb-4">
                {{ employesAAfecter.length }} employé(s) actif(s) dans d'autres agences.
                Sélectionnez un site dans cette agence puis cliquez "Affecter".
              </p>

              <div class="divide-y divide-gray-100">
                @for (emp of employesAAfecter; track emp.id) {
                  <div class="py-4">
                    <div class="flex flex-col sm:flex-row sm:items-center gap-3">

                      <!-- Infos employé -->
                      <div class="flex-1 min-w-0">
                        <div class="flex flex-wrap items-center gap-2 mb-1">
                          <span class="font-mono bg-slate-100 px-2 py-0.5 rounded text-xs font-bold">{{ emp.matricule }}</span>
                          <span class="font-semibold text-gray-900">{{ emp.nomComplet }}</span>
                          @if (emp.fonction) {
                            <span class="px-1.5 py-0.5 bg-blue-50 text-blue-700 text-xs rounded">{{ FONCTION_LABELS[emp.fonction] }}</span>
                          }
                        </div>
                        <div class="flex flex-wrap gap-3 text-xs text-gray-500">
                          @if (emp.telephone) { <span>📞 {{ emp.telephone }}</span> }
                          <span class="text-orange-600">🏢 {{ emp.nomAgence || 'Agence inconnue' }}</span>
                          @if (emp.nomSite) { <span>📍 {{ emp.nomSite }}</span> }
                        </div>
                        <!-- Avertissements -->
                        @if (emp.fonction === 'AGENT_TERRAIN') {
                          <p class="mt-1 text-xs text-amber-600 bg-amber-50 px-2 py-1 rounded">
                            ⚠️ Agent Terrain : vérifiez ses sites affectés dans l'ancienne agence après le transfert.
                          </p>
                        }
                        @if (emp.fonction === 'GESTIONNAIRE') {
                          <p class="mt-1 text-xs text-amber-600 bg-amber-50 px-2 py-1 rounded">
                            ⚠️ Gestionnaire : ses agents terrain supervisés restent dans leur agence actuelle.
                          </p>
                        }
                      </div>

                      <!-- Sélection site + bouton Affecter -->
                      <div class="flex items-center gap-2 shrink-0">
                        <select [(ngModel)]="siteSelectionMap[emp.id]"
                                class="border border-gray-300 rounded-lg px-2 py-1.5 text-sm focus:ring-2 focus:ring-violet-400 focus:border-violet-400 min-w-36">
                          <option [value]="undefined" disabled selected>— Choisir site —</option>
                          @for (site of sitesDeLAgence; track site.id) {
                            <option [value]="site.id">{{ site.nomSite }}</option>
                          }
                        </select>
                        <button (click)="affecterEmploye(emp)"
                                [disabled]="!siteSelectionMap[emp.id] || affectEnCours[emp.id]"
                                class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
                                [class.bg-violet-600]="siteSelectionMap[emp.id] && !affectEnCours[emp.id]"
                                [class.text-white]="siteSelectionMap[emp.id] && !affectEnCours[emp.id]"
                                [class.hover:bg-violet-700]="siteSelectionMap[emp.id] && !affectEnCours[emp.id]"
                                [class.bg-gray-200]="!siteSelectionMap[emp.id] || affectEnCours[emp.id]"
                                [class.text-gray-400]="!siteSelectionMap[emp.id] || affectEnCours[emp.id]"
                                [class.cursor-not-allowed]="!siteSelectionMap[emp.id] || affectEnCours[emp.id]">
                          {{ affectEnCours[emp.id] ? 'En cours...' : 'Affecter' }}
                        </button>
                      </div>
                    </div>
                  </div>
                }
              </div>
            }
          </div>

          <!-- Pied modal -->
          <div class="px-6 py-3 border-t border-gray-100 shrink-0 flex justify-end">
            <button (click)="fermerModalAffectation()"
                    class="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 text-sm font-medium">
              Fermer
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class AgenceDetailComponent implements OnInit {
  private agenceService = inject(AgenceService);
  private employeService = inject(EmployeService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  readonly FONCTION_LABELS = FONCTION_LABELS;

  agence: AgenceResponse | null = null;
  sites: SiteResponse[] = [];
  personnel: EmployeResponse[] = [];
  anomalies: AgentTerrainResponse[] = [];
  loading = false;
  loadingSites = false;
  loadingPersonnel = false;
  loadingAnomalies = false;
  /** Map agentId → true pendant la correction en cours */
  corrigerEnCours: Record<number, boolean> = {};
  error = '';
  successMessage = '';

  // ── Modal "Affecter un employé existant" ─────────────────
  showModalAffectation = false;
  loadingModalData = false;
  employesAAfecter: EmployeResponse[] = [];
  /** siteId sélectionné par employé : Map<employeId, siteId> */
  siteSelectionMap: Record<number, number | undefined> = {};
  /** employeId → true quand l'affectation est en cours */
  affectEnCours: Record<number, boolean> = {};
  /** Sites actifs de l'agence courante (alias pour la modal) */
  get sitesDeLAgence(): SiteResponse[] { return this.sites.filter(s => s.actif); }

  // ── Groupes par fonction ──────────────────────────────────
  get chefsBureau(): EmployeResponse[] {
    return this.personnel.filter(e => e.fonction === 'CHEF_BUREAU');
  }
  get gestionnaires(): EmployeResponse[] {
    return this.personnel.filter(e => e.fonction === 'GESTIONNAIRE');
  }
  get agentsTerrain(): EmployeResponse[] {
    return this.personnel.filter(e => e.fonction === 'AGENT_TERRAIN');
  }
  get controleurs(): EmployeResponse[] {
    return this.personnel.filter(e => e.fonction === 'CONTROLEUR');
  }
  get caissiers(): EmployeResponse[] {
    return this.personnel.filter(e => e.fonction === 'CAISSIER');
  }
  get autresPersonnel(): EmployeResponse[] {
    const known: FonctionEmploye[] = ['CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN', 'CONTROLEUR', 'CAISSIER'];
    return this.personnel.filter(e => !e.fonction || !known.includes(e.fonction));
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { this.error = 'ID manquant'; return; }
    this.loading = true;
    this.agenceService.getById(+id).subscribe({
      next: (a) => {
        this.agence = a;
        this.loading = false;
        this.chargerSites(+id);
        this.chargerPersonnel(+id);
        this.chargerAnomalies(+id);
      },
      error: () => { this.error = 'Agence introuvable'; this.loading = false; }
    });
  }

  chargerSites(agenceId: number): void {
    this.loadingSites = true;
    this.agenceService.getSitesByAgence(agenceId).subscribe({
      next: (s) => { this.sites = s; this.loadingSites = false; },
      error: () => { this.loadingSites = false; }
    });
  }

  chargerPersonnel(agenceId: number): void {
    this.loadingPersonnel = true;
    this.employeService.getByAgence(agenceId).subscribe({
      next: (p) => { this.personnel = p; this.loadingPersonnel = false; },
      error: () => { this.loadingPersonnel = false; }
    });
  }

  chargerAnomalies(agenceId: number): void {
    this.loadingAnomalies = true;
    this.agenceService.getAnomaliesPersonnel(agenceId).subscribe({
      next: (a) => { this.anomalies = a; this.loadingAnomalies = false; },
      error: () => { this.loadingAnomalies = false; }
    });
  }

  /**
   * Corrige l'agence de l'employé agent terrain en la remettant à l'agence
   * du site principal de l'agent. Réservé ADMIN.
   */
  corrigerAnomalieAgent(agent: AgentTerrainResponse): void {
    if (!this.agence || !agent.employeId) return;
    const confirm = window.confirm(
      `Rattacher l'employé "${agent.employeNomComplet || agent.nomCompletUtilisateur}" à l'agence "${this.agence.nomAgence}" ?\n` +
      `Son site de rattachement sera : ${agent.sitePrincipalNom}.`
    );
    if (!confirm) return;
    this.corrigerEnCours[agent.id] = true;
    this.employeService.changerAgence(agent.employeId, this.agence.id, agent.siteId).subscribe({
      next: () => {
        this.corrigerEnCours[agent.id] = false;
        this.successMessage = `Employé ${agent.employeNomComplet || agent.matricule} rattaché à ${this.agence!.nomAgence}.`;
        setTimeout(() => { this.successMessage = ''; }, 4000);
        // Rafraîchir personnel + anomalies
        this.chargerPersonnel(this.agence!.id);
        this.chargerAnomalies(this.agence!.id);
      },
      error: (err) => {
        this.corrigerEnCours[agent.id] = false;
        this.error = err?.error?.message || 'Erreur lors de la correction.';
        setTimeout(() => { this.error = ''; }, 5000);
      }
    });
  }

  // ── Modal "Affecter un employé existant" ─────────────────

  ouvrirModalAffectation(): void {
    if (!this.agence) return;
    this.showModalAffectation = true;
    this.loadingModalData = true;
    this.siteSelectionMap = {};
    this.employesAAfecter = [];
    this.employeService.getEmployesAAffecterAgence(this.agence.id).subscribe({
      next: (liste) => { this.employesAAfecter = liste; this.loadingModalData = false; },
      error: () => { this.loadingModalData = false; }
    });
  }

  fermerModalAffectation(): void {
    this.showModalAffectation = false;
    this.employesAAfecter = [];
    this.siteSelectionMap = {};
  }

  affecterEmploye(emp: EmployeResponse): void {
    const siteId = this.siteSelectionMap[emp.id];
    if (!this.agence || !siteId) return;
    this.affectEnCours[emp.id] = true;
    this.employeService.changerAgence(emp.id, this.agence.id, siteId).subscribe({
      next: () => {
        this.affectEnCours[emp.id] = false;
        // Retirer de la liste modale
        this.employesAAfecter = this.employesAAfecter.filter(e => e.id !== emp.id);
        this.successMessage = `${emp.nomComplet} affecté(e) à ${this.agence!.nomAgence}.`;
        setTimeout(() => { this.successMessage = ''; }, 4000);
        // Rafraîchir personnel + anomalies
        this.chargerPersonnel(this.agence!.id);
        this.chargerAnomalies(this.agence!.id);
      },
      error: (err) => {
        this.affectEnCours[emp.id] = false;
        this.error = err?.error?.message || `Erreur lors de l'affectation de ${emp.nomComplet}.`;
        setTimeout(() => { this.error = ''; }, 5000);
      }
    });
  }

  desactiver(): void {
    if (!this.agence) return;
    if (!confirm(`Désactiver l'agence "${this.agence.nomAgence}" ?`)) return;
    this.agenceService.deactivate(this.agence.id).subscribe({
      next: () => {
        this.successMessage = `Agence désactivée.`;
        if (this.agence) { this.agence = { ...this.agence, actif: false }; }
        setTimeout(() => { this.successMessage = ''; }, 3000);
      },
      error: () => { this.error = 'Erreur lors de la désactivation'; }
    });
  }
}

