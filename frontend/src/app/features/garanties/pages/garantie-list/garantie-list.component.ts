import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { GarantieService } from '../../services/garantie.service';
import { GarantieResponse } from '../../models/garantie-response';

@Component({
  selector: 'app-garantie-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './garantie-list.component.html',
  styleUrls: ['./garantie-list.component.css']
})
export class GarantieListComponent implements OnInit, OnDestroy {
  garanties: GarantieResponse[] = [];
  loading: boolean = false;
  error: string | null = null;

  private destroy$ = new Subject<void>();
  private subscriptions = new Subscription();

  constructor(private garantieService: GarantieService) {}

  ngOnInit(): void {
    this.loadGaranties();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.subscriptions.unsubscribe();
  }

  loadGaranties(): void {
    this.loading = true;
    this.error = null;

    this.subscriptions.add(
      this.garantieService.getAll()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (data) => {
            this.garanties = data;
            this.loading = false;
          },
          error: (err) => {
            this.error = 'Erreur lors du chargement des garanties';
            console.error(err);
            this.loading = false;
          }
        })
    );
  }

  getTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'BIEN_IMMOBILIER': 'Bien Immobilier',
      'TERRAIN': 'Terrain',
      'MAISON': 'Maison',
      'COMMERCE': 'Commerce',
      'BIEN_MOBILIER': 'Bien Mobilier',
      'VEHICULE': 'Véhicule',
      'EQUIPEMENT': 'Équipement',
      'MATERIEL': 'Matériel',
      'GARANTIE_PERSONNEL': 'Garantie Personnelle',
      'CAUTION_SOLIDAIRE': 'Caution Solidaire',
      'HYPOTHEQUE': 'Hypothèque',
      'GAGE': 'Gage',
      'TITRE_FINANCIER': 'Titre Financier',
      'DEPOT_ESPECES': 'Dépôt Espèces',
      'DEPOT_LIVRET': 'Dépôt Livret',
      'AUTRE': 'Autre'
    };
    return labels[type] || type;
  }

  getStatutColor(statut: string): string {
    const colors: { [key: string]: string } = {
      'ACTIF': 'text-green-600',
      'EN_ATTENTE': 'text-yellow-600',
      'REALISEE': 'text-orange-600',
      'LIBEREE': 'text-blue-600',
      'SAISIE': 'text-red-600'
    };
    return colors[statut] || 'text-gray-600';
  }

  getStatutBadgeColor(statut: string): string {
    const colors: { [key: string]: string } = {
      'ACTIF': 'bg-green-100 text-green-800',
      'EN_ATTENTE': 'bg-yellow-100 text-yellow-800',
      'REALISEE': 'bg-orange-100 text-orange-800',
      'LIBEREE': 'bg-blue-100 text-blue-800',
      'SAISIE': 'bg-red-100 text-red-800'
    };
    return colors[statut] || 'bg-gray-100 text-gray-800';
  }
}
