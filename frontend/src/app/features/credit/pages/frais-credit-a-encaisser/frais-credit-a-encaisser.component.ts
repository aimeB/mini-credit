import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterModule } from '@angular/router';

import { DemandeCreditService } from '../../services/demande-credit.service';
import { FraisCreditAEncaisserResponse } from '../../models/frais-credit-a-encaisser-response';

@Component({
  selector: 'app-frais-credit-a-encaisser',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './frais-credit-a-encaisser.component.html'
})
export class FraisCreditAEncaisserComponent implements OnInit {
  private readonly demandeCreditService = inject(DemandeCreditService);

  demandes: FraisCreditAEncaisserResponse[] = [];
  loading = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadDemandes();
  }

  loadDemandes(): void {
    this.loading = true;
    this.errorMessage = '';

    this.demandeCreditService.getFraisCreditAEncaisser().subscribe({
      next: (data) => {
        this.demandes = data ?? [];
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur chargement frais crédit à encaisser :', error);
        this.errorMessage = error?.error?.message || 'Impossible de charger les frais crédit à encaisser.';
        this.loading = false;
      }
    });
  }

  trackByDemandeId(_: number, demande: FraisCreditAEncaisserResponse): number {
    return demande.demandeCreditId;
  }
}