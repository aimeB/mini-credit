import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CollecteTerrainService } from '../../services/collecte-terrain.service';
import { CollecteTerrainResponse } from '../../models/collecte-terrain.model';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

@Component({
  selector: 'app-suivi-collectes-terrain',
  standalone: true,
  imports: [CommonModule, WorkflowGuidanceBannerComponent],
  templateUrl: './suivi-collectes-terrain.component.html',
  styleUrl: './suivi-collectes-terrain.component.css'
})
export class SuiviCollectesTerrainComponent implements OnInit {
  private collecteService = inject(CollecteTerrainService);

  loading = false;
  collectes: CollecteTerrainResponse[] = [];
  globalGuidance: WorkflowGuidance = {
    title: 'Suivi des collectes terrain',
    message: 'Cette vue permet une consultation de suivi des collectes terrain et de leurs états. Elle sert à vérifier l’avancement, les écarts et le volume des opérations sans modifier le flux métier.',
    currentStep: 'Lecture de suivi',
    nextStep: 'Analyse des écarts',
    expectedRole: 'Gestionnaire / Contrôleur',
    expectedAction: 'Suivre l’avancement terrain',
    severity: 'info',
    canCurrentUserAct: false
  };

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.collecteService.list({ page: 0, size: 100 }).subscribe({
      next: (result) => {
        this.collectes = result.content || [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}
