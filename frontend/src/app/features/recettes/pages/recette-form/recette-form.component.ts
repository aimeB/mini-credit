import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { RecetteTerrainService } from '../../services/recette-terrain.service';
import { AgentTerrainService } from '../../../membres/services/agent-terrain.service';
import { SiteService } from '../../../membres/services/site.service';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import {
  RecetteTerrainResponse,
  CreateRecetteTerrainRequest,
  UpdateRecetteTerrainRequest
} from '../../models';

@Component({
  selector: 'app-recette-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './recette-form.component.html',
  styleUrls: ['./recette-form.component.css']
})
export class RecetteFormComponent implements OnInit {
  private recetteService = inject(RecetteTerrainService);
  private agentService = inject(AgentTerrainService);
  private siteService = inject(SiteService);
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  form!: FormGroup;
  recette?: RecetteTerrainResponse;
  agents: any[] = [];
  sites: any[] = [];
  isLoading = false;
  isEditMode = false;
  recetteId?: number;
  submitMode: 'BROUILLON' | 'SOUMISE' = 'BROUILLON';
  currentUser = this.authService.getCurrentUser();
  isAgentTerrainUser = this.authService.hasRole('AGENT_TERRAIN');
  connectedAgent?: any;
  connectedAgentSites: any[] = [];
  hasNoAssignedSite = false;
  globalGuidance: WorkflowGuidance = {
    title: 'Suivi des recettes journalières terrain',
    message: 'Cette page permet de suivre la recette journalière depuis l’activité terrain jusqu’à la génération des opérations officielles. L’Agent Terrain remet au bureau les fonds collectés, les demandes de crédit et la fiche de recette. Le billetage est réalisé avec le Caissier et sert de preuve opérationnelle. Le Contrôleur vérifie ensuite les montants, les informations, les écarts éventuels et valide la recette. Les opérations officielles ne doivent être générées qu’après validation du Contrôleur, afin d’éviter les doubles encodages et de garantir la traçabilité.',
    currentStep: 'Activité terrain',
    nextStep: 'Retour bureau',
    expectedRole: 'Agent Terrain',
    expectedAction: 'Compléter puis soumettre la recette journalière',
    severity: 'info',
    canCurrentUserAct: true
  };

  ngOnInit(): void {
    this.recetteId = this.route.snapshot.params['id'];
    this.isEditMode = !!this.recetteId;

    this.initForm();
    this.loadData();
  }

  private initForm(): void {
    this.form = this.fb.group({
      agentTerrainId: [{ value: '', disabled: this.isEditMode || this.isAgentTerrainUser }, this.isAgentTerrainUser ? [] : [Validators.required]],
      siteId: [{ value: '', disabled: this.isEditMode }, Validators.required],
      dateRecette: [{ value: '', disabled: this.isEditMode }, Validators.required],
      membresVisites: [0, [Validators.required, Validators.min(0)]],
      nouveauxMembres: [0, [Validators.required, Validators.min(0)]],
      carnetDistribues: [0, [Validators.required, Validators.min(0)]],
      epargneCollectee: [0, [Validators.required, Validators.min(0)]],
      remboursementsCreditCollectes: [0, [Validators.required, Validators.min(0)]],
      creditIdsTraites: [''],
      fraisCollectes: [0, [Validators.required, Validators.min(0)]],
      demandesCreditRecueillies: [0, [Validators.min(0)]],
      demandesCreditIds: [''],
      especesRemises: [0, [Validators.required, Validators.min(0)]],
      especesEmises: [0, [Validators.min(0)]],
      observations: ['', Validators.maxLength(500)]
    });

    this.form.valueChanges.subscribe(() => {
      const obsCtrl = this.form.get('observations');
      if (!obsCtrl) return;
      const validators = this.isObservationRequired
        ? [Validators.required, Validators.maxLength(500)]
        : [Validators.maxLength(500)];
      obsCtrl.setValidators(validators);
      obsCtrl.updateValueAndValidity({ emitEvent: false });
    });
  }

  private async loadData(): Promise<void> {
    this.isLoading = true;

    try {
      const agents = await firstValueFrom(this.agentService.getAll());
      const sites = await firstValueFrom(this.siteService.getAll());
      const recette = this.isEditMode && this.recetteId 
        ? await firstValueFrom(this.recetteService.getById(this.recetteId))
        : undefined;

      this.agents = agents || [];
      this.sites = sites || [];
      this.recette = recette;

      if (this.isAgentTerrainUser && this.currentUser) {
        this.connectedAgent = this.agents.find(a => a.utilisateurId === this.currentUser?.id);
        if (this.connectedAgent) {
          this.form.patchValue({ agentTerrainId: this.connectedAgent.id });
          const allowedSiteIds = new Set<number>();
          if (this.connectedAgent.siteId) {
            allowedSiteIds.add(Number(this.connectedAgent.siteId));
          }
          if (Array.isArray(this.connectedAgent.siteIds)) {
            this.connectedAgent.siteIds.forEach((sid: number) => allowedSiteIds.add(Number(sid)));
          }
          this.connectedAgentSites = this.sites.filter(s => allowedSiteIds.has(Number(s.id)));

          if (!this.isEditMode) {
            if (this.connectedAgentSites.length === 0) {
              this.hasNoAssignedSite = true;
              this.form.patchValue({ siteId: '' });
              this.form.get('siteId')?.disable();
            } else if (this.connectedAgentSites.length === 1) {
              this.form.patchValue({ siteId: this.connectedAgentSites[0].id });
              this.form.get('siteId')?.disable();
            } else {
              this.form.get('siteId')?.enable();
            }
          }
        }
      }

      if (this.isEditMode && recette) {
        this.populateForm(recette);
      }

      this.isLoading = false;
    } catch (err) {
      console.error('Erreur chargement', err);
      this.isLoading = false;
    }
  }

  private populateForm(recette: RecetteTerrainResponse): void {
    this.form.patchValue({
      agentTerrainId: recette.agentTerrainId,
      siteId: recette.siteId,
      dateRecette: recette.dateRecette,
      membresVisites: recette.membresVisites,
      nouveauxMembres: recette.nouveauxMembres,
      carnetDistribues: recette.carnetDistribues,
      epargneCollectee: recette.epargneCollectee,
      remboursementsCreditCollectes: recette.remboursementsCreditCollectes,
      creditIdsTraites: recette.creditIdsTraites || '',
      fraisCollectes: recette.fraisCollectes,
      demandesCreditRecueillies: recette.demandesCreditRecueillies,
      demandesCreditIds: recette.demandesCreditIds || '',
      especesRemises: recette.especesRemises,
      especesEmises: recette.especesEmises,
      observations: recette.observations
    });
  }

  onSubmit(mode: 'BROUILLON' | 'SOUMISE' = 'BROUILLON'): void {
    this.submitMode = mode;

    if (this.isAgentTerrainUser && this.hasNoAssignedSite) {
      alert("Aucun site n'est affecté à votre compte. Veuillez contacter le gestionnaire ou l'administrateur.");
      return;
    }

    if (!this.form.valid) {
      alert('Formulaire invalide');
      return;
    }

    if (this.isObservationRequired && !this.form.get('observations')?.value?.trim()) {
      alert('Observation obligatoire si écart trésorerie différent de 0.');
      return;
    }

    this.isLoading = true;
    const formValue = this.form.getRawValue();

    if (this.isEditMode && this.recetteId) {
      const updateData: UpdateRecetteTerrainRequest = {
        membresVisites: formValue.membresVisites,
        nouveauxMembres: formValue.nouveauxMembres,
        carnetDistribues: formValue.carnetDistribues,
        epargneCollectee: formValue.epargneCollectee,
        remboursementsCreditCollectes: formValue.remboursementsCreditCollectes,
        creditIdsTraites: formValue.creditIdsTraites || null,
        fraisCollectes: formValue.fraisCollectes,
        demandesCreditRecueillies: formValue.demandesCreditRecueillies,
        demandesCreditIds: formValue.demandesCreditIds || null,
        especesRemises: formValue.especesRemises,
        especesEmises: formValue.especesEmises,
        observations: formValue.observations
      };

      this.recetteService.update(this.recetteId, updateData).subscribe({
        next: () => {
          if (mode === 'SOUMISE') {
            this.recetteService.soumettre(this.recetteId!).subscribe({
              next: () => {
                alert('Recette soumise au contrôle');
                this.router.navigate(['/recettes', this.recetteId]);
              },
              error: (err) => {
                alert('Erreur soumission: ' + (err.error?.message || err.message));
                this.isLoading = false;
              }
            });
            return;
          }
          alert('Brouillon mis à jour');
          this.router.navigate(['/recettes', this.recetteId]);
        },
        error: (err) => {
          alert('Erreur: ' + (err.error?.message || err.message));
          this.isLoading = false;
        }
      });
    } else {
      const createData: CreateRecetteTerrainRequest = this.isAgentTerrainUser
        ? {
            siteId: formValue.siteId,
            dateRecette: formValue.dateRecette,
            membresVisites: formValue.membresVisites,
            nouveauxMembres: formValue.nouveauxMembres,
            carnetDistribues: formValue.carnetDistribues,
            epargneCollectee: formValue.epargneCollectee,
            remboursementsCreditCollectes: formValue.remboursementsCreditCollectes,
            creditIdsTraites: formValue.creditIdsTraites || null,
            fraisCollectes: formValue.fraisCollectes,
            demandesCreditRecueillies: formValue.demandesCreditRecueillies,
            demandesCreditIds: formValue.demandesCreditIds || null,
            especesRemises: formValue.especesRemises,
            especesEmises: formValue.especesEmises,
            observations: formValue.observations || undefined
          }
        : formValue;

      this.recetteService.create(createData).subscribe({
        next: (recette) => {
          if (mode === 'SOUMISE') {
            this.recetteService.soumettre(recette.id).subscribe({
              next: () => {
                alert('Recette créée et soumise au contrôle');
                this.router.navigate(['/recettes', recette.id]);
              },
              error: (err) => {
                alert('Erreur soumission: ' + (err.error?.message || err.message));
                this.isLoading = false;
              }
            });
            return;
          }
          alert('Brouillon enregistré');
          this.router.navigate(['/recettes', recette.id]);
        },
        error: (err) => {
          alert('Erreur: ' + (err.error?.message || err.message));
          this.isLoading = false;
        }
      });
    }
  }

  onCancel(): void {
    if (this.isEditMode && this.recetteId) {
      this.router.navigate(['/recettes', this.recetteId]);
    } else {
      this.router.navigate(['/recettes']);
    }
  }

  get totalCollecte(): number {
    return (this.form.get('epargneCollectee')?.value || 0) +
           (this.form.get('remboursementsCreditCollectes')?.value || 0) +
           (this.form.get('fraisCollectes')?.value || 0);
  }

  get ecartTresorerie(): number {
    return (this.form.get('especesRemises')?.value || 0) - this.totalCollecte;
  }

  get isObservationRequired(): boolean {
    return this.ecartTresorerie !== 0;
  }

  get statusAffichage(): string {
    return this.recette?.statut || 'BROUILLON';
  }

  get connectedAgentLabel(): string {
    if (this.connectedAgent?.nomCompletUtilisateur) return this.connectedAgent.nomCompletUtilisateur;
    if (this.currentUser?.nomComplet) return this.currentUser.nomComplet;
    return 'Agent connecté';
  }

  get currentSiteLabel(): string {
    const siteId = this.form.get('siteId')?.value;
    const site = this.sites.find(s => s.id === Number(siteId));
    if (site?.nomSite) return site.nomSite;
    if (this.connectedAgentSites.length === 1) return this.connectedAgentSites[0].nomSite;
    if (this.recette?.siteNom) return this.recette.siteNom;
    if (this.hasNoAssignedSite) return 'Aucun site affecté';
    return 'Sélection requise';
  }

  get canChooseSite(): boolean {
    return this.isAgentTerrainUser && this.connectedAgentSites.length > 1 && !this.isEditMode;
  }

  get showSiteSelectForNonAgent(): boolean {
    return !this.isAgentTerrainUser && !this.isEditMode;
  }
}
