import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { RecetteTerrainService } from '../../services/recette-terrain.service';
import { RecetteTerrainResponse, OperationGenerationStatus } from '../../models';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';

@Component({
  selector: 'app-recette-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './recette-detail.component.html',
  styleUrls: ['./recette-detail.component.css']
})
export class RecetteDetailComponent implements OnInit {
  private recetteService = inject(RecetteTerrainService);
  private authService = inject(AuthService);
  private workflowMessageService = inject(WorkflowMessageService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  recette?: RecetteTerrainResponse;
  isLoading = false;
  isGeneratingOperations = false;
  generationErrorMessage: string | null = null;
  currentUserRole: string = '';
  guidance: WorkflowGuidance | null = null;

  // PHASE 6B.2: Statuts de génération
  readonly OperationGenerationStatus = OperationGenerationStatus;

  ngOnInit(): void {
    const id = this.route.snapshot.params['id'];
    const currentUser = this.authService.getCurrentUser();
    this.currentUserRole = currentUser?.role || '';
    this.loadRecette(id);
  }

  private loadRecette(id: number): void {
    this.isLoading = true;
    this.recetteService.getById(id).subscribe({
      next: (recette) => {
        this.recette = recette;
        this.guidance = this.workflowMessageService.getGuidance({
          module: 'RECETTE_JOURNALIERE',
          status: recette.statut,
          currentRole: this.currentUserRole,
          permissions: this.authService.getCurrentUser()?.permissions,
          metadata: {
            operationsGenerated: this.isRecetteAlreadyGenerated(),
            manquant: recette.manquant,
            ecart: (recette.manquant || 0) + (recette.excedent || 0)
          }
        });
        this.isLoading = false;
      },
      error: (err) => {
        alert('Erreur: ' + err.message);
        this.isLoading = false;
      }
    });
  }

  onEdit(): void {
    if (this.recette?.statut !== 'BROUILLON') {
      alert('Impossible de modifier une recette non-brouillon');
      return;
    }
    this.router.navigate(['/recettes', this.recette?.id, 'edit']);
  }

  onDelete(): void {
    if (!confirm('Êtes-vous sûr?')) return;

    if (this.recette?.id) {
      this.recetteService.delete(this.recette.id).subscribe({
        next: () => {
          alert('Recette supprimée');
          this.router.navigate(['/recettes']);
        },
        error: (err) => alert('Erreur: ' + err.message)
      });
    }
  }

  onSoumettre(): void {
    if (!this.recette || this.recette.statut !== 'BROUILLON') {
      alert('Recette non-brouillon');
      return;
    }

    if (!confirm('Soumettre cette recette pour validation?')) return;

    this.recetteService.soumettre(this.recette.id).subscribe({
      next: () => {
        alert('Recette soumise');
        this.loadRecette(this.recette!.id);
      },
      error: (err) => alert('Erreur: ' + err.message)
    });
  }

  onValidate(): void {
    if (this.recette?.id) {
      this.router.navigate(['/recettes', this.recette.id, 'valider']);
    }
  }

  onBack(): void {
    this.router.navigate(['/recettes']);
  }

  getStatusColor(statut: string): string {
    const colors: Record<string, string> = {
      'BROUILLON': 'warning',
      'SOUMISE': 'info',
      'VALIDEE': 'success',
      'REJETEE': 'danger'
    };
    return colors[statut] || 'secondary';
  }

  getStatusLabel(statut: string): string {
    const labels: Record<string, string> = {
      'BROUILLON': 'Brouillon',
      'SOUMISE': 'Soumise',
      'VALIDEE': 'Validée',
      'REJETEE': 'Rejetée'
    };
    return labels[statut] || statut;
  }

  canEdit(): boolean {
    return this.recette?.statut === 'BROUILLON' && this.currentUserRole === 'ADMIN';
  }

  canValidate(): boolean {
    return this.recette?.statut === 'SOUMISE' && 
           ['ADMIN', 'CHEF_BUREAU', 'CONTROLEUR'].includes(this.currentUserRole);
  }

  /**
   * PHASE 6B.2: Vérifie si le bouton "Générer opérations" doit être visible
   */
  canGenerateOperations(): boolean {
    return this.recette?.statut === 'VALIDEE' &&
           !this.isRecetteAlreadyGenerated() &&
           ['ADMIN', 'CONTROLEUR'].includes(this.currentUserRole);
  }

  /**
   * PHASE 6B.2: Vérifie si les opérations ont déjà été générées
   */
  isRecetteAlreadyGenerated(): boolean {
    return this.recette?.operationGenerationStatus === OperationGenerationStatus.GENEREE ||
           this.recette?.operationGenerationStatus === OperationGenerationStatus.PARTIELLE;
  }

  /**
   * PHASE 6B.2: Déclenche la génération manuelle des opérations
   */
  onGenerateOperations(): void {
    if (!this.recette?.id) {
      alert('Recette non valide');
      return;
    }

    if (!confirm('Générer les opérations financières pour cette recette?')) {
      return;
    }

    this.isGeneratingOperations = true;
    this.generationErrorMessage = null;

    this.recetteService.generateOperations(this.recette.id).subscribe({
      next: (updatedRecette) => {
        this.recette = updatedRecette;
        this.isGeneratingOperations = false;
        
        // Message de succès basé sur le statut
        if (this.recette.operationGenerationStatus === OperationGenerationStatus.GENEREE) {
          alert(`✓ Opérations générées avec succès!\n\nÉpargne: ${this.recette.operationEpargneCount || 0}\nCaisse: ${this.recette.operationCaisseCount || 0}`);
        } else if (this.recette.operationGenerationStatus === OperationGenerationStatus.PARTIELLE) {
          alert(`⚠ Génération partielle\n\nÉpargne: ${this.recette.operationEpargneCount || 0}\nCaisse: ${this.recette.operationCaisseCount || 0}\n\nVérifiez les détails.`);
        }
      },
      error: (err) => {
        this.isGeneratingOperations = false;
        this.generationErrorMessage = err.error?.message || err.message || 'Erreur lors de la génération';
        alert('Erreur: ' + this.generationErrorMessage);
      }
    });
  }

  /**
   * PHASE 6B.2: Obtient le label du statut de génération
   */
  getGenerationStatusLabel(status?: string): string {
    const labels: Record<string, string> = {
      [OperationGenerationStatus.NON_GENEREE]: 'Non générées',
      [OperationGenerationStatus.GENEREE]: 'Générées',
      [OperationGenerationStatus.PARTIELLE]: 'Génération partielle',
      [OperationGenerationStatus.ERREUR]: 'Erreur de génération',
      [OperationGenerationStatus.EN_COURS]: 'En cours de génération'
    };
    return labels[status || ''] || 'Inconnu';
  }

  /**
   * PHASE 6B.2: Obtient la classe CSS pour le badge du statut
   */
  getGenerationStatusColor(status?: string): string {
    const colors: Record<string, string> = {
      [OperationGenerationStatus.NON_GENEREE]: 'secondary',
      [OperationGenerationStatus.GENEREE]: 'success',
      [OperationGenerationStatus.PARTIELLE]: 'warning',
      [OperationGenerationStatus.ERREUR]: 'danger',
      [OperationGenerationStatus.EN_COURS]: 'info'
    };
    return colors[status || ''] || 'secondary';
  }
}
