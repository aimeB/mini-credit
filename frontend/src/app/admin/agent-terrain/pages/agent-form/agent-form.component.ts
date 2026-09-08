import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AdminAgentTerrainService } from '../../services/admin-agent-terrain.service';
import { CreateAgentTerrainRequest, UpdateAgentTerrainRequest, UtilisateurSimple, SiteSimple } from '../../models/agent-terrain.model';
import { EmployeResponse } from '../../../../features/employes/models/employe-response';
import { UtilisateurResponse } from '../../../../features/utilisateurs/models/utilisateur-response';
import { SiteService } from '../../../../features/membres/services/site.service';
import { SiteResponse } from '../../../../features/membres/models/site-response';
import { firstValueFrom } from 'rxjs';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

@Component({
  selector: 'app-agent-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  template: `
    <div class="mc-page-wide">
      <header class="mc-page-hero flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 class="mc-page-title">{{ isEditMode ? 'Modifier Agent' : 'Nouvel Agent Terrain' }}</h1>
          <p class="mc-page-subtitle">Affectation opérationnelle d'un Agent Terrain à un site et à un Gestionnaire.</p>
        </div>
        <button class="mc-btn bg-white/15 text-white ring-1 ring-white/30 hover:bg-white/25" type="button" (click)="onBack()">Retour</button>
      </header>

      <div class="mb-3">
        <app-workflow-guidance-banner [guidance]="supervisionGuidance"></app-workflow-guidance-banner>
      </div>

      <div *ngIf="isLoading" class="mc-state mc-state-info text-center">
        Chargement...
      </div>

      <div *ngIf="!isLoading" class="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div>
          <form [formGroup]="form" (ngSubmit)="onSubmit()" class="mc-panel space-y-4">

            <!-- Bloc d'aide prérequis (création uniquement) -->
            @if (!isEditMode) {
              <div class="mc-state mc-state-warning text-sm" role="alert">
                <strong>Prérequis avant de créer un Agent Terrain :</strong>
                <ol class="mb-1 mt-1 list-decimal space-y-1 pl-5">
                  <li>L'<strong>employé</strong> doit exister avec la fonction <code>AGENT_TERRAIN</code></li>
                  <li>Son <strong>compte utilisateur</strong> doit exister, lui être lié, avec le rôle <code>AGENT_TERRAIN</code></li>
                </ol>
                <div class="mc-button-row mt-2">
                  <a routerLink="/employes/nouveau" class="mc-btn bg-white text-amber-800 ring-1 ring-amber-200 hover:bg-amber-50">Employé Agent Terrain</a>
                  <a routerLink="/utilisateurs/nouveau" class="mc-btn bg-white text-amber-800 ring-1 ring-amber-200 hover:bg-amber-50">Compte Utilisateur</a>
                </div>
              </div>
            }

            <!-- Bloc site pré-sélectionné (mode contexte depuis SiteDetail) -->
            @if (preselectSiteInfo && !isEditMode) {
              <div class="mc-state mc-state-info" role="alert">
                <div class="mb-1 flex items-center gap-2">
                  <span class="text-xs font-bold uppercase tracking-wide text-blue-700">
                    Site d'affectation
                  </span>
                </div>
                <p class="mb-1 font-bold">{{ preselectSiteInfo.nomSite }}</p>
                @if (preselectSiteInfo.nomAgence) {
                  <p class="mb-0 text-sm text-slate-600">
                    Agence : {{ preselectSiteInfo.nomAgence }}
                  </p>
                }
                @if (preselectSiteInfo.zone) {
                  <p class="mb-0 text-sm text-slate-600">
                    Zone : {{ preselectSiteInfo.zone }}
                  </p>
                }
                <p class="mb-0 mt-2 text-xs text-slate-500">
                  Prérempli depuis la fiche site · Non modifiable
                </p>
              </div>
            }

            <!-- Utilisateur Agent Terrain (dropdown filtré) -->
            <div>
              <label for="utilisateur" class="mc-field-label">Utilisateur Agent Terrain *</label>
              @if (!isEditMode && utilisateurs.length === 0 && !isLoading) {
                <div class="mc-state mc-state-warning mb-2 text-sm">
                  Aucun utilisateur Agent Terrain disponible.
                  Créez d'abord un employé Agent Terrain, puis son compte utilisateur.
                </div>
              }
              <select id="utilisateur" class="mc-select" formControlName="utilisateurId"
                [class.is-invalid]="form.get('utilisateurId')?.invalid && form.get('utilisateurId')?.touched">
                <option value="">-- Sélectionner --</option>
                <option *ngFor="let user of utilisateurs" [value]="user.id">
                  {{ user.username }}
                  <ng-container *ngIf="user.employeNomComplet"> — {{ user.employeNomComplet }}</ng-container>
                </option>
              </select>
              <div class="text-xs text-red-600">Utilisateur requis</div>
            </div>

            <!-- Site Principal — masqué si pré-sélectionné depuis SiteDetail -->
            @if (!preselectSiteInfo || isEditMode) {
              <div>
                <label for="site" class="mc-field-label">Site Principal *</label>
                <select id="site" class="mc-select" formControlName="siteId"
                  [class.is-invalid]="form.get('siteId')?.invalid && form.get('siteId')?.touched">
                  <option value="">-- Selectionner --</option>
                  <option *ngFor="let site of sites" [value]="site.id">
                    {{ site.nomSite }} ({{ site.ville }})
                  </option>
                </select>
                <div class="text-xs text-red-600">Site requis</div>
              </div>
            }

            <!-- Gestionnaire -->
            <div>
              <label for="gestionnaire" class="mc-field-label">Gestionnaire *</label>
              <select id="gestionnaire" class="mc-select" formControlName="gestionnaireId"
                [class.is-invalid]="form.get('gestionnaireId')?.invalid && form.get('gestionnaireId')?.touched">
                <option value="">-- Selectionner un Gestionnaire --</option>
                <option *ngFor="let g of gestionnaires" [value]="g.id">
                  {{ g.nomComplet }} ({{ g.matricule }})
                </option>
              </select>
              <div class="text-xs text-red-600">Gestionnaire requis</div>
              <div *ngIf="gestionnaires.length === 0" class="mt-1 text-xs text-amber-700">
                Aucun Gestionnaire actif disponible.
              </div>
            </div>

            <!-- Date d'affectation -->
            <div>
              <label for="dateAffectation" class="mc-field-label">Date d'affectation</label>
              <input type="date" id="dateAffectation" class="mc-input" formControlName="dateAffectation">
            </div>

            <!-- Boutons -->
            <div class="mc-button-row">
              <button type="submit" class="mc-btn mc-btn-primary" [disabled]="!form.valid || isSaving">
                {{ isSaving ? 'Enregistrement...' : (isEditMode ? 'Modifier' : 'Créer') }}
              </button>
              <button type="button" class="mc-btn bg-gray-200 text-gray-800 hover:bg-gray-300" (click)="onBack()">Annuler</button>
            </div>

            <div *ngIf="successMessage" class="mc-state mc-state-success mt-3">{{ successMessage }}</div>
            <div *ngIf="errorMessage" class="mc-state mc-state-danger mt-3">{{ errorMessage }}</div>
          </form>
        </div>

        <div>
          <div class="mc-panel">
            <h2 class="text-lg font-bold text-gray-900">Instructions</h2>
            <ul class="mt-4 list-disc space-y-2 pl-5 text-sm text-gray-600">
              <li><strong>Utilisateur</strong> : Lien vers un compte utilisateur existant</li>
              @if (!preselectSiteInfo || isEditMode) {
                <li><strong>Site Principal</strong> : Site d'affectation principal</li>
              }
              <li><strong>Gestionnaire</strong> : Employé superviseur (fonction = Gestionnaire)</li>
            </ul>
            <p class="mt-4 text-sm text-gray-500">Le matricule est généré automatiquement.</p>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class AgentFormComponent implements OnInit {
  form!: FormGroup;
  utilisateurs: UtilisateurResponse[] = [];
  sites: SiteSimple[] = [];
  gestionnaires: EmployeResponse[] = [];

  isEditMode = false;
  agentId?: number;
  isLoading = false;
  isSaving = false;
  successMessage = '';
  errorMessage = '';

  /** ID du site passé en query param (depuis la page détail site) */
  siteIdFromParam: number | null = null;
  /** ID du site vers lequel revenir après création */
  returnToSiteId: number | null = null;
  /** Infos complètes du site pré-sélectionné (pour affichage) */
  preselectSiteInfo: SiteResponse | null = null;

  readonly supervisionGuidance: WorkflowGuidance = {
    title: 'Supervision des agents',
    message: 'Le Gestionnaire supervise les agents terrain, suit les sites, participe au developpement des sites et effectue la pre-analyse des credits. Cet ecran doit aider a organiser les affectations terrain et le suivi operationnel sans remplacer les etapes de validation financiere.',
    currentStep: 'Supervision terrain',
    nextStep: 'Suivi des sites / pre-analyse credit / amelioration terrain',
    expectedRole: 'Gestionnaire',
    expectedAction: 'Suivre les agents, les sites et les observations terrain',
    severity: 'info',
    canCurrentUserAct: true,
    blockedReason: 'Le Gestionnaire ne valide pas les operations financieres.'
  };

  constructor(
    private fb: FormBuilder,
    private agentService: AdminAgentTerrainService,
    private siteService: SiteService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    // Lire les query params (snapshot : ils ne changent pas en cours de navigation)
    const qp = this.route.snapshot.queryParamMap;
    const siteIdParam = qp.get('siteId');
    const returnToParam = qp.get('returnTo');
    if (siteIdParam) {
      this.siteIdFromParam = Number(siteIdParam);
      this.returnToSiteId = returnToParam ? Number(returnToParam) : this.siteIdFromParam;
    }

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      this.isEditMode = !!id;
      if (id) {
        this.agentId = Number(id);
        this.loadAgentData();
      } else {
        this.loadLookupData();
      }
    });
  }

  initForm(): void {
    this.form = this.fb.group({
      utilisateurId: ['', Validators.required],
      siteId: ['', Validators.required],
      gestionnaireId: ['', Validators.required],
      dateAffectation: [null]
    });
  }

  private async loadAgentData(): Promise<void> {
    this.isLoading = true;
    try {
      const [agent, sites, gestionnaires] = await Promise.all([
        firstValueFrom(this.agentService.getById(this.agentId!)),
        firstValueFrom(this.agentService.getSites()),
        firstValueFrom(this.agentService.getGestionnaires())
      ]);

      // En mode édition, l'utilisateur est désactivé — la liste n'est pas nécessaire
      this.utilisateurs = [];
      this.sites = sites;
      this.gestionnaires = gestionnaires;

      this.form.patchValue({
        utilisateurId: agent.utilisateurId,
        siteId: agent.siteId,
        gestionnaireId: agent.gestionnaireId ?? '',
        dateAffectation: agent.dateAffectation ?? null
      });

      this.form.get('utilisateurId')?.disable();
    } catch (err) {
      console.error('Erreur chargement agent', err);
      this.errorMessage = "Erreur lors du chargement de l'agent";
    } finally {
      this.isLoading = false;
    }
  }

  private async loadLookupData(): Promise<void> {
    this.isLoading = true;
    try {
      const [utilisateurs, sites, gestionnaires] = await Promise.all([
        firstValueFrom(this.agentService.getUtilisateursDisponibles()),
        firstValueFrom(this.agentService.getSites()),
        firstValueFrom(this.agentService.getGestionnaires())
      ]);
      this.utilisateurs = utilisateurs;
      this.sites = sites;
      this.gestionnaires = gestionnaires;

      // Si le site est pré-sélectionné depuis la page de détail site
      if (this.siteIdFromParam) {
        this.form.patchValue({ siteId: this.siteIdFromParam });
        try {
          this.preselectSiteInfo = await firstValueFrom(this.siteService.getById(this.siteIdFromParam));
        } catch {
          // Fallback : utiliser les infos de la liste simplifiée
          const found = sites.find(s => s.id === this.siteIdFromParam);
          if (found) {
            this.preselectSiteInfo = { id: found.id, nomSite: found.nomSite, codeSite: '' };
          }
        }
      }
    } catch (err) {
      console.error('Erreur chargement donnees', err);
      this.errorMessage = 'Erreur lors du chargement des données';
    } finally {
      this.isLoading = false;
    }
  }

  onSubmit(): void {
    if (!this.form.valid) return;

    this.isSaving = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (this.isEditMode) {
      const request: UpdateAgentTerrainRequest = {
        siteId: Number(this.form.get('siteId')?.value),
        gestionnaireId: Number(this.form.get('gestionnaireId')?.value)
      };
      this.agentService.update(this.agentId!, request).subscribe({
        next: () => {
          this.successMessage = 'Agent modifié avec succès';
          setTimeout(() => this.router.navigate(['/admin/agents']), 1500);
        },
        error: (err) => {
          this.errorMessage = err?.error?.message || 'Erreur lors de la modification';
          this.isSaving = false;
        }
      });
    } else {
      const utilisateurId = Number(this.form.get('utilisateurId')?.value);
      // Le site vient du param (pré-sélection) ou du formulaire (mode normal)
      const siteId = this.siteIdFromParam ?? Number(this.form.get('siteId')?.value);
      // Matricule auto-généré : AT-{utilisateurId} — unique car un seul agent par utilisateur
      const autoMatricule = `AT-${utilisateurId}`;

      const request: CreateAgentTerrainRequest = {
        utilisateurId,
        matricule: autoMatricule,
        siteId,
        gestionnaireId: Number(this.form.get('gestionnaireId')?.value),
        dateAffectation: this.form.get('dateAffectation')?.value ?? undefined
      };
      this.agentService.create(request).subscribe({
        next: () => {
          this.successMessage = 'Agent créé avec succès';
          const destination = this.returnToSiteId
            ? ['/sites', this.returnToSiteId]
            : ['/admin/agents'];
          setTimeout(() => this.router.navigate(destination), 1500);
        },
        error: (err) => {
          this.errorMessage = err?.error?.message || 'Erreur lors de la création';
          this.isSaving = false;
        }
      });
    }
  }

  onBack(): void {
    if (this.returnToSiteId) {
      this.router.navigate(['/sites', this.returnToSiteId]);
    } else {
      this.router.navigate(['/admin/agents']);
    }
  }
}