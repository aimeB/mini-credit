import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterModule } from '@angular/router';

import { CreditEnCoursResponse } from '../../models/credit-en-cours-response';
import { CreditService } from '../../services/credit.service';

@Component({
  selector: 'app-credit-en-cours',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './credit-en-cours.component.html'
})
export class CreditEnCoursComponent implements OnInit {
  private readonly creditService = inject(CreditService);

  credits: CreditEnCoursResponse[] = [];
  loading = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadCredits();
  }

  loadCredits(): void {
    this.loading = true;
    this.errorMessage = '';

    this.creditService.getCreditsEnCours().subscribe({
      next: (credits) => {
        this.credits = credits ?? [];
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Impossible de charger les crédits en cours.';
        this.loading = false;
      }
    });
  }

  trackByCreditId(_: number, credit: CreditEnCoursResponse): number {
    return credit.id;
  }

  formatRoleValue(value: string | null | undefined): string {
    return value && value.trim() ? value : '-';
  }

  formatDuree(credit: CreditEnCoursResponse): string {
    if (!credit.dureeValeur && !credit.dureeUnite) {
      return '-';
    }

    return `${credit.dureeValeur ?? ''} ${credit.dureeUnite ?? ''}`.trim();
  }

  getStatutBadgeClasses(statut: string): string {
    const classes: Record<string, string> = {
      APPROUVE: 'bg-blue-100 text-blue-800',
      DECAISSE: 'bg-green-100 text-green-800',
      EN_COURS: 'bg-yellow-100 text-yellow-800',
      EN_RETARD: 'bg-orange-100 text-orange-800',
      CONTENTIEUX: 'bg-red-100 text-red-800'
    };

    return classes[statut] || 'bg-gray-100 text-gray-800';
  }
}
