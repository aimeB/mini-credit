import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { distinctUntilChanged, finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';

import { MembreService } from '../../services/membre.service';
import { MembreCreateRequest } from '../../models/membre-create-request';
import { MembreUpdateRequest } from '../../models/membre-update-request';
import { MembreResponse } from '../../models/membre-response';
import { MemberCredentialsModalComponent } from '../../components/member-credentials-modal/member-credentials-modal.component';

import { SiteService } from '../../services/site.service';
import { AgentTerrainService } from '../../services/agent-terrain.service';
import { AuthService, User } from '../../../../core/services/auth.service';
import { Sexe } from '../../../../shared/enums/sexe.enum';
import { StatutMembre } from '../../enum/statut-membre';
import { SiteResponse } from '../../models/site-response';
import { AgentTerrainResponse } from '../../models/agent-terrain-response';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

@Component({
  selector: 'app-membre-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, WorkflowGuidanceBannerComponent],
  templateUrl: './membre-form.component.html'
})
export class MembreFormComponent implements OnInit {

  private fb = inject(FormBuilder);
  private membreService = inject(MembreService);
  private siteService = inject(SiteService);
  private agentTerrainService = inject(AgentTerrainService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  loading = false;
  loadingData = false;
  error = '';
  success = '';
  successMessage = '';
  loadingAgents = false;

  readonly creationGuidance: WorkflowGuidance = {
    title: 'Creation d un membre',
    message: 'La creation d un membre alimente les workflows epargne, credit, recettes journalieres et rapports. Le membre doit etre rattache a un site et a un agent responsable. Apres creation, le systeme doit disposer d un compte epargne unique pour ce membre selon le processus existant.',
    currentStep: 'Creation membre',
    nextStep: 'Creation / activation du compte epargne',
    expectedRole: 'Agent Terrain / Gestionnaire / role autorise selon les droits existants',
    expectedAction: 'Saisir les informations du membre, son site et son agent responsable',
    severity: 'info',
    canCurrentUserAct: true,
    blockedReason: 'La creation ne doit pas etre finalisee si les informations obligatoires existantes ne sont pas completes ou si le membre existe deja.'
  };

  readonly statutGuidance: WorkflowGuidance = {
    title: 'Statut du membre',
    message: 'Le statut du membre influence l utilisation du dossier dans les workflows. Les statuts prevus sont ACTIF, INACTIF, SUSPENDU et CLOTURE. Toute modification de statut doit rester historisee.',
    currentStep: 'Statut membre',
    nextStep: 'Application du statut aux operations autorisees',
    expectedRole: 'Role autorise selon les droits existants',
    expectedAction: 'Verifier le statut avant toute operation sensible',
    severity: 'warning',
    canCurrentUserAct: true,
    blockedReason: 'Certaines operations peuvent etre bloquees si le membre est suspendu, inactif ou cloture selon les regles existantes.'
  };

  get formGuidance(): WorkflowGuidance {
    return this.isEditMode ? this.statutGuidance : this.creationGuidance;
  }

  isEditMode = false;
  membreId: number | null = null;

  sites: SiteResponse[] = [];
  agents: AgentTerrainResponse[] = [];
  currentUser: User | null = this.authService.getCurrentUser();
  isAgentTerrain = this.currentUser?.role === 'AGENT_TERRAIN';
  selectedSite: SiteResponse | null = null;
  selectedSiteLabel = this.currentUser?.siteNom ?? '';

  form = this.fb.group({
    nom: ['', [Validators.required, Validators.minLength(1)]],
    postnom: [''],
    prenom: ['', [Validators.required, Validators.minLength(1)]],
    sexe: ['', Validators.required],
    dateNaissance: ['', Validators.required],
    telephonePrincipal: ['', [Validators.required, Validators.minLength(6)]],
    telephoneSecondaire: [''],
    adresse: ['', [Validators.required, Validators.minLength(3)]],
    ville: ['Kinshasa', [Validators.required, Validators.minLength(1)]],
    commune: ['', [Validators.required, Validators.minLength(1)]],
    quartier: ['', [Validators.required, Validators.minLength(1)]],
      email: ['', [Validators.email]],
    professionActivite: ['', [Validators.required, Validators.minLength(1)]],
    lieuActivite: ['', [Validators.required, Validators.minLength(1)]],
    sourceInscription: [''],
    siteId: [null as number | null, Validators.required],
    agentId: [null as number | null],
    dateAdhesion: ['', Validators.required],
    statut: [''],
    observation: ['']
  });

  ngOnInit(): void {
    this.loadingData = true;
    this.error = '';

    const idParam = this.route.snapshot.paramMap.get('id');

    if (idParam) {
      this.isEditMode = true;
      this.membreId = Number(idParam);
    }

    if (this.isAgentTerrain) {
      const siteId = this.currentUser?.siteId;
      if (!siteId) {
        this.loadingData = false;
        this.error = "Votre compte n'est rattaché à aucun site";
        return;
      }

      this.selectedSite = {
        id: siteId,
        codeSite: '',
        nomSite: this.currentUser?.siteNom ?? 'Mon site'
      };
      this.sites = [this.selectedSite];
      this.form.patchValue({ siteId });
      this.form.get('siteId')?.disable({ emitEvent: false });

      this.loadingData = false;
      this.loadAgentsBySite(siteId, !this.isEditMode, () => {
        if (this.isEditMode) {
          this.chargerMembre(this.membreId!);
        }
      });

      return;
    }

    this.setupAgentReloadOnSiteChange();

    this.siteService.getAll().pipe(
      finalize(() => {
        this.loadingData = false;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (sites) => {
        this.sites = sites ?? [];

        if (!this.isEditMode) {
          if (this.sites.length === 1) {
            this.form.patchValue({ siteId: this.sites[0].id });
          }
          return;
        }

        this.chargerMembre(this.membreId!);
      },
      error: () => {
        this.error = 'Erreur lors du chargement des sites';
      }
    });
  }

  private setupAgentReloadOnSiteChange(): void {
    this.form.get('siteId')?.valueChanges.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((siteId) => {
      this.form.patchValue({ agentId: null }, { emitEvent: false });
      this.loadAgentsBySite(siteId != null ? Number(siteId) : null);
    });
  }

  private loadAgentsBySite(siteId: number | null, autoSelectSingle = false, afterLoad?: () => void): void {
    if (!siteId) {
      this.agents = [];
      afterLoad?.();
      return;
    }

    this.loadingAgents = true;
    this.agentTerrainService.getBySite(siteId).pipe(
      finalize(() => {
        this.loadingAgents = false;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (agents) => {
        this.agents = (agents ?? []).map((agent: AgentTerrainResponse) => ({
          ...agent,
          nomAffichage: this.buildAgentLabel(agent)
        }));

        if (autoSelectSingle && this.agents.length === 1) {
          this.form.patchValue({ agentId: this.agents[0].id }, { emitEvent: false });
        }

        afterLoad?.();
      },
      error: () => {
        this.agents = [];
        this.error = 'Erreur lors du chargement des agents du site';
        afterLoad?.();
      }
    });
  }

  private buildAgentLabel(agent: AgentTerrainResponse): string {
    return (
      agent.nomCompletUtilisateur ||
      agent.nomComplet ||
      agent.username ||
      `Agent #${agent.id}`
    );
  }

  chargerMembre(id: number): void {
    this.error = '';

    this.membreService.getById(id).pipe(
      finalize(() => {
        this.loadingData = false;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (membre: MembreResponse) => {
        this.form.patchValue({
          nom: membre.nom ?? '',
          postnom: membre.postnom ?? '',
          prenom: membre.prenom ?? '',
          sexe: membre.sexe ?? '',
          dateNaissance: membre.dateNaissance ?? '',
          telephonePrincipal: membre.telephonePrincipal ?? '',
          telephoneSecondaire: membre.telephoneSecondaire ?? '',
          adresse: membre.adresse ?? '',
          ville: membre.ville ?? 'Kinshasa',
          commune: membre.commune ?? '',
          quartier: membre.quartier ?? '',
          professionActivite: membre.professionActivite ?? '',
          lieuActivite: membre.lieuActivite ?? '',
          sourceInscription: membre.sourceInscription ?? '',
          siteId: membre.siteId ?? null,
          agentId: membre.agentId ?? null,
          dateAdhesion: membre.dateAdhesion ?? '',
          statut: membre.statut ?? '',
          observation: membre.observation ?? ''
        });
      },
      error: () => {
        this.error = 'Erreur lors du chargement du membre';
      }
    });
  }

  enregistrer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = '';
    this.success = '';

    const raw = this.form.getRawValue();
    const resolvedSiteId = this.isAgentTerrain && this.currentUser?.siteId
      ? this.currentUser.siteId
      : (raw.siteId ? Number(raw.siteId) : 0);

    if (this.isEditMode && this.membreId) {
      const request: MembreUpdateRequest = {
        nom: raw.nom || undefined,
        postnom: raw.postnom || undefined,
        prenom: raw.prenom || undefined,
        sexe: (raw.sexe || undefined) as Sexe | undefined,
        dateNaissance: raw.dateNaissance || undefined,
        telephonePrincipal: raw.telephonePrincipal || undefined,
        telephoneSecondaire: raw.telephoneSecondaire || undefined,
        adresse: raw.adresse || undefined,
        ville: raw.ville || undefined,
        commune: raw.commune || undefined,
        quartier: raw.quartier || undefined,
        professionActivite: raw.professionActivite || undefined,
        lieuActivite: raw.lieuActivite || undefined,
        sourceInscription: raw.sourceInscription || undefined,
        dateAdhesion: raw.dateAdhesion || undefined,
        statut: (raw.statut || undefined) as StatutMembre | undefined,
        observation: raw.observation || undefined,
        agentId: raw.agentId != null ? Number(raw.agentId) : undefined,
        siteId: resolvedSiteId || undefined
      };

      this.membreService.update(this.membreId, request).pipe(
        finalize(() => {
          this.loading = false;
        }),
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: () => {
          this.success = 'Membre mis à jour avec succès !';
          setTimeout(() => {
            this.router.navigate(['/membres']);
          }, 1500);
        },
        error: (err) => {
          this.error = err?.error?.message || 'Erreur lors de la modification du membre';
        }
      });

      return;
    }

    const createRequest: MembreCreateRequest = {
      nom: raw.nom?.trim() ?? '',
      postnom: raw.postnom?.trim() || undefined,
      prenom: raw.prenom?.trim() ?? '',
      sexe: (raw.sexe || undefined) as Sexe | undefined,
      dateNaissance: raw.dateNaissance ?? undefined,
      telephonePrincipal: raw.telephonePrincipal?.trim() ?? '',
      telephoneSecondaire: raw.telephoneSecondaire?.trim() || undefined,
      adresse: raw.adresse?.trim() ?? '',
      ville: raw.ville?.trim() ?? 'Kinshasa',
      commune: raw.commune?.trim() ?? '',
      quartier: raw.quartier?.trim() ?? '',
      email: raw.email?.trim() ?? '',
      professionActivite: raw.professionActivite?.trim() ?? '',
      lieuActivite: raw.lieuActivite?.trim() ?? '',
      sourceInscription: raw.sourceInscription?.trim() || undefined,
      siteId: resolvedSiteId,
      agentId: raw.agentId ? Number(raw.agentId) : undefined,
      dateAdhesion: raw.dateAdhesion ?? new Date().toISOString().split('T')[0],
      observation: raw.observation?.trim() || undefined
    };

    this.membreService.create(createRequest).pipe(
      finalize(() => {
        this.loading = false;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        this.success = 'Membre créé avec succès !';
        
        // Afficher le modal avec les credentials
        if (response.activationCode) {
          const dialogRef = this.dialog.open(MemberCredentialsModalComponent, {
            width: '600px',
            data: response,
            disableClose: false
          });

          dialogRef.afterClosed().subscribe(() => {
            // Rediriger vers la liste avec indicateur de rafraichissement explicite
            this.router.navigate(['/membres'], { queryParams: { refresh: Date.now() } });
          });
        } else {
          // Fallback si pas de credentials
          setTimeout(() => {
            this.router.navigate(['/membres'], { queryParams: { refresh: Date.now() } });
          }, 1500);
        }
      },
      error: (err) => {
        this.error = err?.error?.message || 'Erreur lors de la création du membre';
      }
    });
  }

  getFieldError(fieldName: string): string {
    const control = this.form.get(fieldName);
    if (!control || !control.errors || !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return 'Ce champ est obligatoire';
    }

    return '';
  }

  get siteFieldDisabled(): boolean {
    return this.isAgentTerrain && !!this.currentUser?.siteId;
  }
}