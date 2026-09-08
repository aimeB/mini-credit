import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowGuidanceBannerComponent } from '../../../../shared/components/workflow-guidance-banner/workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../../../shared/models/workflow-guidance.model';
import { WorkflowMessageService } from '../../../../shared/services/workflow-message.service';
import { SessionCaisseAnomalieResponse } from '../../models/session-caisse-anomalie-response';
import { SessionCaisseService } from '../../services/session-caisse.service';

@Component({
  selector: 'app-session-caisse-anomalies',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, WorkflowGuidanceBannerComponent],
  templateUrl: './session-caisse-anomalies.component.html',
  styleUrl: './session-caisse-anomalies.component.css'
})
export class SessionCaisseAnomaliesComponent implements OnInit {
  anomalies: SessionCaisseAnomalieResponse[] = [];
  loading = false;
  error = '';

  filtreDate = '';
  filtreSession = '';
  filtreStatut = '';
  filtreType = '';
  anomaliesGuidance: WorkflowGuidance | null = null;

  constructor(
    private sessionCaisseService: SessionCaisseService,
    private authService: AuthService,
    private workflowMessageService: WorkflowMessageService
  ) {}

  ngOnInit(): void {
    this.anomaliesGuidance = this.workflowMessageService.getGuidance({
      module: 'CAISSE',
      status: 'PRE_CLOTUREE',
      currentRole: this.authService.getCurrentUser()?.role,
      expectedRole: 'CONTROLEUR',
      nextStep: 'Traitement des anomalies de session'
    });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';

    this.sessionCaisseService.getAnomaliesGlobales().subscribe({
      next: (data) => {
        this.anomalies = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Impossible de charger les anomalies de session.';
        this.loading = false;
      }
    });
  }

  get anomaliesFiltrees(): SessionCaisseAnomalieResponse[] {
    return this.anomalies.filter((a) => {
      const matchesDate = !this.filtreDate || (a.dateDemande || '').slice(0, 10) === this.filtreDate;
      const matchesSession = !this.filtreSession || String(a.sessionId) === this.filtreSession;
      const matchesStatut = !this.filtreStatut || a.statutDossier === this.filtreStatut;
      const matchesType = !this.filtreType || a.typeAnomalie === this.filtreType;
      return matchesDate && matchesSession && matchesStatut && matchesType;
    });
  }
}
