import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CollecteTerrainService } from '../../services/collecte-terrain.service';
import { CollecteTerrainResponse } from '../../models/collecte-terrain.model';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

@Component({
  selector: 'app-collectes-a-controler',
  standalone: true,
  imports: [CommonModule, WorkflowGuidanceBannerComponent],
  templateUrl: './collectes-a-controler.component.html',
  styleUrl: './collectes-a-controler.component.css'
})
export class CollectesAControlerComponent implements OnInit {
  private collecteService = inject(CollecteTerrainService);

  loading = false;
  collectes: CollecteTerrainResponse[] = [];
  lastGenerationSummary = '';
  successMessage = '';
  errorMessage = '';
  validatingCollecteId?: number;
  globalGuidance: WorkflowGuidance = {
    title: 'Contrôle des collectes billetteries',
    message: 'Cette vue regroupe les collectes dont le billetage est confirmé et qui attendent le contrôle final. Le Contrôleur doit vérifier les écarts, les montants et la cohérence des opérations avant validation ou rejet.',
    currentStep: 'Contrôle final',
    nextStep: 'Validation ou rejet',
    expectedRole: 'CONTROLEUR',
    expectedAction: 'Contrôler la collecte soumise',
    severity: 'info',
    canCurrentUserAct: true
  };

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.collecteService.list({ statut: 'SOUMISE', page: 0, size: 50 }).subscribe({
      next: (result) => {
        this.collectes = result.content || [];
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur de chargement des collectes à contrôler', error);
        this.errorMessage = this.extractErrorMessage(error, 'Impossible de charger les collectes à contrôler.');
        this.loading = false;
      }
    });
  }

  valider(c: CollecteTerrainResponse): void {
    this.errorMessage = '';
    this.successMessage = '';
    if (!this.canControlerCollecte(c)) {
      this.errorMessage = 'Validation impossible: billetage non confirmé.';
      return;
    }
    this.validatingCollecteId = c.id;
    this.collecteService.valider(c.id, { decision: 'VALIDEE' }).subscribe({
      next: (response) => {
        this.lastGenerationSummary = response?.generationSummary || '';
        this.successMessage = 'Collecte validée avec succès.';
        this.errorMessage = '';
        this.validatingCollecteId = undefined;
        this.load();
      },
      error: (error) => {
        console.error('Erreur de validation de la collecte', { collecteId: c.id, error });
        this.errorMessage = this.extractErrorMessage(error, 'La validation contrôleur a échoué.');
        this.successMessage = '';
        this.validatingCollecteId = undefined;
      }
    });
  }

  rejeter(c: CollecteTerrainResponse): void {
    this.errorMessage = '';
    this.successMessage = '';
    if (!this.canControlerCollecte(c)) {
      this.errorMessage = 'Rejet impossible: billetage non confirmé.';
      return;
    }
    const motif = prompt('Motif de rejet:');
    if (!motif || !motif.trim()) {
      return;
    }
    this.collecteService.rejeter(c.id, { decision: 'REJETEE', motifRejet: motif.trim() }).subscribe({
      next: () => this.load(),
      error: (error) => {
        console.error('Erreur de rejet de la collecte', { collecteId: c.id, error });
        this.errorMessage = this.extractErrorMessage(error, 'Le rejet de la collecte a échoué.');
      }
    });
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    return (error as any)?.error?.message || (error as any)?.message || fallback;
  }

  canControlerCollecte(collecte: CollecteTerrainResponse): boolean {
    return collecte.statut === 'SOUMISE' && collecte.billetageConfirme === true;
  }
}
