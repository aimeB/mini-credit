import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AgenceService } from '../../../employes/services/agence.service';
import { AgenceResponse } from '../../models/agence-response';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';

@Component({
  selector: 'app-agence-list',
  standalone: true,
  imports: [CommonModule, RouterLink, WorkflowGuidanceBannerComponent],
  templateUrl: './agence-list.component.html'
})
export class AgenceListComponent implements OnInit {
  private agenceService = inject(AgenceService);

  agences: AgenceResponse[] = [];
  loading = false;
  error = '';
  successMessage = '';

  readonly globalGuidance: WorkflowGuidance = {
    title: 'Gestion des agences et antennes',
    message: 'Cette page permet de gerer les structures operationnelles utilisees pour organiser les equipes, les operations, la caisse, les rapports et la tracabilite. L antenne est importante pour rattacher les operations a leur contexte operationnel.',
    currentStep: 'Agence ou antenne consultable ou modifiable',
    nextStep: 'Rattachement operationnel / rapports / controle',
    expectedRole: 'Chef de Bureau / COO / Admin selon les droits existants',
    expectedAction: 'Consulter ou mettre a jour les informations selon les droits existants',
    severity: 'info',
    canCurrentUserAct: true,
    blockedReason: 'Toute operation sensible doit rester rattachee a son antenne lorsque le systeme expose cette information.'
  };

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.error = '';
    this.agenceService.getAll()
      .pipe(finalize(() => { this.loading = false; }))
      .subscribe({
        next: (data) => { this.agences = data; },
        error: () => { this.error = 'Erreur lors du chargement des agences'; }
      });
  }

  desactiver(agence: AgenceResponse): void {
    if (!confirm(`Désactiver l'agence "${agence.nomAgence}" ?`)) return;
    this.agenceService.deactivate(agence.id).subscribe({
      next: () => {
        this.successMessage = `Agence "${agence.nomAgence}" désactivée.`;
        this.charger();
        setTimeout(() => { this.successMessage = ''; }, 3000);
      },
      error: () => { this.error = 'Erreur lors de la désactivation'; }
    });
  }
}

