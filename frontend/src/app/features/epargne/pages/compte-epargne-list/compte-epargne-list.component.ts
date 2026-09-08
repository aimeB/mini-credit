import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { CompteEpargneService } from '../../services/compte-epargne.service';
import { CompteEpargneResponse } from '../../models/compte-epargne-response';

@Component({
  selector: 'app-compte-epargne-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './compte-epargne-list.component.html'
})
export class CompteEpargneListComponent implements OnChanges {
  @Input() membreId!: number;
  @Output() compteSelected = new EventEmitter<CompteEpargneResponse>();

  private compteEpargneService = inject(CompteEpargneService);

  comptes: CompteEpargneResponse[] = [];
  loading = false;
  errorMessage = '';
  selectedCompteId: number | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['membreId'] && this.membreId) {
      this.loadComptes();
    }
  }

  loadComptes(): void {
    this.loading = true;
    this.errorMessage = '';

    this.compteEpargneService.getByMembre(this.membreId).subscribe({
      next: (data) => {
        this.comptes = data;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.comptes = [];
        this.errorMessage =
          err?.error?.message || 'Erreur lors du chargement des comptes épargne';
      }
    });
  }

  selectCompte(compte: CompteEpargneResponse): void {
    this.selectedCompteId = compte.id;
    this.compteSelected.emit(compte);
  }
}