import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CollecteTerrainService } from '../../services/collecte-terrain.service';
import { CollecteTerrainResponse } from '../../models/collecte-terrain.model';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

@Component({
  selector: 'app-collectes-soumises-billetage',
  standalone: true,
  imports: [CommonModule, WorkflowGuidanceBannerComponent],
  templateUrl: './collectes-soumises-billetage.component.html',
  styleUrl: './collectes-soumises-billetage.component.css'
})
export class CollectesSoumisesBilletageComponent implements OnInit {
  private collecteService = inject(CollecteTerrainService);

  loading = false;
  collectes: CollecteTerrainResponse[] = [];
  lastMessage = '';
  globalGuidance: WorkflowGuidance = {
    title: 'Billetage des collectes soumises',
    message: 'Cette vue liste les collectes en attente de billetage. Le Caissier doit confirmer les espèces déclarées par l’Agent Terrain avant le passage au contrôle. Le billetage ne remplace pas la validation finale.',
    currentStep: 'Billetage',
    nextStep: 'Contrôle Contrôleur',
    expectedRole: 'CAISSIER',
    expectedAction: 'Confirmer le billetage',
    severity: 'warning',
    canCurrentUserAct: true
  };

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.collecteService.list({ statut: 'SOUMISE', page: 0, size: 50 }).subscribe({
      next: (result) => {
        this.collectes = (result.content || []).filter((collecte) => collecte.billetageConfirme !== true);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  confirmerBilletage(c: CollecteTerrainResponse): void {
    const valeurParDefaut = String(c.especesDeclareesAgent ?? c.especesRemises ?? c.totalGeneralCalcule ?? 0);
    const montantRaw = prompt('Montant confirmé au billetage (CDF):', valeurParDefaut);
    if (montantRaw === null) {
      return;
    }

    const montant = Number(montantRaw);
    if (Number.isNaN(montant) || montant < 0) {
      this.lastMessage = 'Montant confirmé invalide.';
      return;
    }

    const observation = prompt('Observation billetage (optionnel):', c.observationBilletage || '') || '';
    this.collecteService.confirmerBilletage(c.id, {
      especesConfirmeesCaissier: montant,
      observationBilletage: observation.trim() || undefined,
    }).subscribe({
      next: () => {
        this.lastMessage = 'Billetage confirmé avec succès.';
        this.load();
      },
      error: () => {
        this.lastMessage = 'Échec de confirmation billetage.';
      }
    });
  }
}
