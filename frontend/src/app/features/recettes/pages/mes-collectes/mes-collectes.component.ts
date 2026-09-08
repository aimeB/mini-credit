import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CollecteTerrainService } from '../../services/collecte-terrain.service';
import { CollecteTerrainResponse } from '../../models/collecte-terrain.model';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

@Component({
  selector: 'app-mes-collectes',
  standalone: true,
  imports: [CommonModule, WorkflowGuidanceBannerComponent],
  templateUrl: './mes-collectes.component.html',
  styleUrl: './mes-collectes.component.css'
})
export class MesCollectesComponent implements OnInit {
  private collecteService = inject(CollecteTerrainService);
  private router = inject(Router);

  loading = false;
  errorMessage = '';
  actionMessage = '';
  actionError = '';
  actionInProgressId: number | null = null;
  collectes: CollecteTerrainResponse[] = [];
  expandedCollecteId: number | null = null;
  globalGuidance: WorkflowGuidance = {
    title: 'Mes collectes terrain',
    message: 'Cette vue permet à l’Agent Terrain de reprendre ses brouillons, de soumettre les collectes prêtes et de suivre leur état jusqu’à validation. Une collecte soumise ne doit plus être modifiée sauf retour explicite au brouillon.',
    currentStep: 'Suivi personnel',
    nextStep: 'Reprise ou soumission',
    expectedRole: 'Agent Terrain',
    expectedAction: 'Suivre et faire avancer ses collectes',
    severity: 'info',
    canCurrentUserAct: true
  };

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.actionError = '';
    this.actionMessage = '';

    this.collecteService.list({ page: 0, size: 100 }).subscribe({
      next: (result) => {
        this.collectes = result.content || [];
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Impossible de charger vos collectes.';
      }
    });
  }

  toggleDetail(collecteId: number): void {
    this.expandedCollecteId = this.expandedCollecteId === collecteId ? null : collecteId;
  }

  isExpanded(collecteId: number): boolean {
    return this.expandedCollecteId === collecteId;
  }

  canReprendre(collecte: CollecteTerrainResponse): boolean {
    return collecte.statut === 'BROUILLON';
  }

  canSoumettre(collecte: CollecteTerrainResponse): boolean {
    return collecte.statut === 'BROUILLON';
  }

  reprendre(collecte: CollecteTerrainResponse): void {
    if (!this.canReprendre(collecte)) {
      return;
    }

    this.router.navigate(['/collectes', collecte.id, 'edition']);
  }

  soumettreDepuisListe(collecte: CollecteTerrainResponse): void {
    if (!this.canSoumettre(collecte)) {
      return;
    }

    this.actionInProgressId = collecte.id;
    this.actionError = '';
    this.actionMessage = '';

    this.collecteService.soumettre(collecte.id, {
      especesRemises: Number(collecte.especesRemises || 0),
      observations: collecte.observations || undefined,
    }).subscribe({
      next: () => {
        this.actionInProgressId = null;
        this.actionMessage = 'Collecte soumise au contrôle.';
        this.load();
      },
      error: (err) => {
        this.actionInProgressId = null;
        this.actionError = err?.error?.message || 'Soumission impossible. Vérifiez la complétude de la collecte.';
      }
    });
  }

  statusBadgeLabel(collecte: CollecteTerrainResponse): string {
    if (collecte.statut === 'SOUMISE') {
      return 'En attente de controle';
    }
    if (collecte.statut === 'VALIDEE') {
      return 'Validee';
    }
    if (collecte.statut === 'REJETEE') {
      return 'Rejetee';
    }
    return '';
  }

  formatCdf(value: number | null | undefined): string {
    return `${new Intl.NumberFormat('fr-CD').format(Number(value || 0))} CDF`;
  }
}
