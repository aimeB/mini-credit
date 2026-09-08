import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { CreditRembourseResponse } from '../../models/credit-rembourse-response';
import { CreditService } from '../../services/credit.service';

type StatutFilter = 'TOUS' | 'REMBOURSE' | 'CLOTURE';

@Component({
  selector: 'app-credit-rembourses',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './credit-rembourses.component.html'
})
export class CreditRemboursesComponent implements OnInit {
  private readonly creditService = inject(CreditService);

  credits: CreditRembourseResponse[] = [];
  loading = false;
  errorMessage = '';

  statutFilter: StatutFilter = 'TOUS';
  antenneFilter = '';
  searchTerm = '';
  dateDebut = '';
  dateFin = '';

  ngOnInit(): void {
    this.loadCredits();
  }

  loadCredits(): void {
    this.loading = true;
    this.errorMessage = '';

    this.creditService.getCreditsRembourses().subscribe({
      next: (credits) => {
        this.credits = credits ?? [];
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Impossible de charger les crédits remboursés.';
        this.loading = false;
      }
    });
  }

  get creditsFiltres(): CreditRembourseResponse[] {
    const search = this.searchTerm.trim().toLowerCase();

    return this.credits.filter((credit) => {
      const statutOk = this.statutFilter === 'TOUS'
        || (this.statutFilter === 'REMBOURSE' && credit.statut === 'REMBOURSE' && !credit.dateCloture)
        || (this.statutFilter === 'CLOTURE' && !!credit.dateCloture);
      const antenneOk = !this.antenneFilter || credit.antenneNom === this.antenneFilter;
      const searchOk = !search
        || credit.numeroCredit.toLowerCase().includes(search)
        || (credit.membreNomComplet ?? '').toLowerCase().includes(search);
      const dateReference = this.getDateReference(credit);
      const dateDebutOk = !this.dateDebut || (!!dateReference && dateReference >= this.dateDebut);
      const dateFinOk = !this.dateFin || (!!dateReference && dateReference <= this.dateFin);

      return statutOk && antenneOk && searchOk && dateDebutOk && dateFinOk;
    });
  }

  get antennes(): string[] {
    return Array.from(new Set(
      this.credits
        .map(credit => credit.antenneNom)
        .filter((antenne): antenne is string => !!antenne)
    )).sort();
  }

  resetFilters(): void {
    this.statutFilter = 'TOUS';
    this.antenneFilter = '';
    this.searchTerm = '';
    this.dateDebut = '';
    this.dateFin = '';
  }

  trackByCreditId(_: number, credit: CreditRembourseResponse): number {
    return credit.id;
  }

  getStatutBadgeClasses(credit: CreditRembourseResponse): string {
    if (credit.dateCloture) {
      return 'bg-slate-200 text-slate-800';
    }

    return credit.statut === 'REMBOURSE'
      ? 'bg-emerald-100 text-emerald-800'
      : 'bg-blue-100 text-blue-800';
  }

  getStatutLabel(credit: CreditRembourseResponse): string {
    return credit.dateCloture ? 'Clôturé' : credit.statut;
  }

  private getDateReference(credit: CreditRembourseResponse): string | null {
    const dateValue = credit.dateCloture || credit.dateDernierPaiement;
    return dateValue ? dateValue.substring(0, 10) : null;
  }
}