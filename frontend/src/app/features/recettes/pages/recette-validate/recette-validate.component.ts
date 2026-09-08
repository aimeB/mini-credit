import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { RecetteTerrainService } from '../../services/recette-terrain.service';
import { RecetteTerrainResponse, ValidateRecetteTerrainRequest } from '../../models';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

@Component({
  selector: 'app-recette-validate',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './recette-validate.component.html',
  styleUrls: ['./recette-validate.component.css']
})
export class RecetteValidateComponent implements OnInit {
  private recetteService = inject(RecetteTerrainService);
  private authService = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  recette?: RecetteTerrainResponse;
  isLoading = false;
  currentUserId: number = 0;
  globalGuidance: WorkflowGuidance = {
    title: 'Validation de la recette journalière',
    message: 'Cette page sert au contrôle de la recette déjà soumise. Le Contrôleur doit comparer les montants attendus, les espèces remises, le billetage confirmé et les écarts éventuels avant de valider ou rejeter. Aucune validation ne doit être faite tant que le billetage n’est pas cohérent.',
    currentStep: 'Contrôle Contrôleur',
    nextStep: 'Validation ou rejet',
    expectedRole: 'CONTROLEUR',
    expectedAction: 'Décider sur la recette soumise',
    severity: 'info',
    canCurrentUserAct: true
  };

  decision: 'VALIDEE' | 'REJETEE' = 'VALIDEE';
  motifRejet = '';

  ngOnInit(): void {
    const id = this.route.snapshot.params['id'];
    const currentUser = this.authService.getCurrentUser();
    this.currentUserId = currentUser?.id || 0;
    this.loadRecette(id);
  }

  private loadRecette(id: number): void {
    this.isLoading = true;
    this.recetteService.getById(id).subscribe({
      next: (recette) => {
        this.recette = recette;
        this.isLoading = false;
      },
      error: (err) => {
        alert('Erreur: ' + err.message);
        this.isLoading = false;
      }
    });
  }

  onValidate(): void {
    if (!this.recette || this.recette.statut !== 'SOUMISE') {
      alert('Recette non soumise');
      return;
    }

    if (this.decision === 'REJETEE' && !this.motifRejet.trim()) {
      alert('Motif de rejet requis');
      return;
    }

    const request: ValidateRecetteTerrainRequest = {
      decision: this.decision,
      motifRejet: this.decision === 'REJETEE' ? this.motifRejet : undefined,
      validePar: this.currentUserId
    };

    this.isLoading = true;
    this.recetteService.valider(this.recette.id, request).subscribe({
      next: () => {
        alert(`Recette ${this.decision === 'VALIDEE' ? 'validée' : 'rejetée'}`);
        this.router.navigate(['/recettes', this.recette!.id]);
      },
      error: (err) => {
        alert('Erreur: ' + err.error?.message || err.message);
        this.isLoading = false;
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/recettes', this.recette?.id]);
  }
}
