import { CommonModule } from '@angular/common';
import { Component, OnInit, DestroyRef, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MembreService } from '../../services/membre.service';
import { MembreResponse } from '../../models/membre-response';
import { CompteEpargneService } from '../../../epargne/services/compte-epargne.service';
import { CompteEpargneResponse } from '../../../epargne/models/compte-epargne-response';
import { CreditService } from '../../../credit/services/credit.service';
import { CreditResponse } from '../../../credit/models/credit-response';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

@Component({
  selector: 'app-membre-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, WorkflowGuidanceBannerComponent],
  templateUrl: './membre-detail.component.html'
})
export class MembreDetailComponent implements OnInit {

  private membreService = inject(MembreService);
  private compteEpargneService = inject(CompteEpargneService);
  private creditService = inject(CreditService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);

  membre: MembreResponse | null = null;
  loading = false;
  loadingCompte = false;
  error = '';
  compteError = '';
  compteEpargne: CompteEpargneResponse | null = null;
  credits: CreditResponse[] = [];
  loadingCredits = false;
  creditError = '';

  readonly detailGuidance: WorkflowGuidance = {
    title: 'Dossier membre',
    message: 'Le dossier membre regroupe les informations utilisees par les operations epargne, les credits, les garanties, les retraits, les recettes journalieres et les rapports. Les informations doivent rester coherentes car elles alimentent plusieurs workflows.',
    currentStep: 'Dossier membre consultable',
    nextStep: 'Suivi epargne / credit / recettes selon le besoin',
    expectedRole: 'Role autorise selon les droits existants',
    expectedAction: 'Consulter ou mettre a jour les informations selon les droits existants',
    severity: 'info',
    canCurrentUserAct: true
  };

  ngOnInit(): void {
    this.loadMember();
  }

  loadMember(): void {
    this.loading = true;
    this.error = '';

    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error = 'ID du membre non fourni';
      this.loading = false;
      return;
    }

    this.membreService.getById(Number(id)).pipe(
      finalize(() => {
        this.loading = false;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (data) => {
        this.membre = data;
        this.loadCompteEpargne(data.id);
        this.loadCredits(data.id);
      },
      error: () => {
        this.error = 'Erreur lors du chargement des détails du membre';
      }
    });
  }

  loadCredits(membreId: number): void {
    this.loadingCredits = true;
    this.creditError = '';
    this.credits = [];

    this.creditService.getByMembre(membreId).pipe(
      finalize(() => {
        this.loadingCredits = false;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (credits) => {
        this.credits = credits ?? [];
      },
      error: (err) => {
        console.error('Erreur chargement crédits membre', err);
        this.creditError = 'Impossible de charger les crédits du membre.';
      }
    });
  }

  deleteMember(): void {
    if (!this.membre) return;

    const confirmed = confirm(
      'Êtes-vous sûr de vouloir supprimer ce membre ? Cette action est définitive.'
    );

    if (!confirmed) return;

    this.loading = true;
    this.membreService.delete(this.membre.id).pipe(
      finalize(() => {
        this.loading = false;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => {
        this.router.navigate(['/membres']);
      },
      error: () => {
        this.error = 'Erreur lors de la suppression du membre';
      }
    });
  }

  getStatutBadgeColor(statut?: string): string {
    switch (statut) {
      case 'ACTIF':
        return 'bg-emerald-100 text-emerald-800';
      case 'SUSPENDU':
        return 'bg-amber-100 text-amber-800';
      case 'CLOTURE':
        return 'bg-gray-100 text-gray-800';
      case 'BLOQUE':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-blue-100 text-blue-800';
    }
  }

  getStatutLabel(statut?: string): string {
    switch (statut) {
      case 'EN_ATTENTE':
        return 'En Attente';
      case 'ACTIF':
        return 'Actif';
      case 'SUSPENDU':
        return 'Suspendu';
      case 'BLOQUE':
        return 'Bloqué';
      case 'CLOTURE':
        return 'Clôturé';
      default:
        return statut || 'Non défini';
    }
  }

  loadCompteEpargne(membreId: number): void {
    this.loadingCompte = true;
    this.compteError = '';
    this.compteEpargne = null;

    this.compteEpargneService.getActiveByMembre(membreId).pipe(
      finalize(() => {
        this.loadingCompte = false;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (compte) => {
        this.compteEpargne = compte;
      },
      error: () => {
        this.compteEpargne = null;
      }
    });
  }

  repairMissingAccount(): void {
    if (!this.membre || !this.canRepairAccount) {
      return;
    }

    this.loadingCompte = true;
    this.compteError = '';

    this.compteEpargneService.repairMissingForMember(this.membre.id).pipe(
      finalize(() => {
        this.loadingCompte = false;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (compte) => {
        this.compteEpargne = compte;
      },
      error: (err) => {
        this.compteError = err?.error?.message || 'Echec de reparation du compte epargne manquant';
      }
    });
  }

  get canRepairAccount(): boolean {
    return this.authService.hasRole('ADMIN');
  }

  get canEditMember(): boolean {
    return !this.authService.hasRole('AGENT_TERRAIN');
  }

  onPhotoError(): void {
    if (this.membre) {
      this.membre = { ...this.membre, photoUrl: undefined };
    }
  }

  formatCdf(value: number | null | undefined): string {
    return `${new Intl.NumberFormat('fr-CD').format(Number(value || 0))} CDF`;
  }
}
