import { Component, HostListener, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EmployeService } from '../../services/employe.service';
import { EmployeResponse } from '../../models/employe-response';
import { FONCTION_LABELS } from '../../models/fonction-employe';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-employe-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 py-8 px-4 sm:px-6 lg:px-8">
      <div class="max-w-3xl mx-auto">

        <!-- Fil d'Ariane -->
        <div class="flex items-center gap-2 text-sm text-gray-500 mb-6">
          <a routerLink="/dashboard" class="hover:text-gray-700">Dashboard</a>
          <span>/</span>
          <a routerLink="/employes" class="hover:text-gray-700">Employés</a>
          <span>/</span>
          <span class="text-gray-800 font-medium">{{ employe?.nomComplet || 'Détail' }}</span>
        </div>

        <!-- Actions -->
        <div class="flex flex-wrap items-center justify-between gap-3 mb-6">
          <h1 class="text-2xl font-bold text-gray-900">Fiche Employé</h1>
          <div class="flex flex-wrap gap-2">
            <a routerLink="/employes"
               class="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 font-medium text-sm">
              ← Liste employés
            </a>
            @if (employe?.agenceId) {
              <a [routerLink]="['/agences', employe!.agenceId]"
                 class="px-4 py-2 bg-sky-100 text-sky-700 rounded-lg hover:bg-sky-200 font-medium text-sm">
                ↑ Agence
              </a>
            }
            @if (employe && canEdit) {
              <a [routerLink]="['/employes', employe.id, 'edit']"
                 class="px-4 py-2 bg-amber-500 text-white rounded-lg hover:bg-amber-600 font-medium text-sm">
                Modifier
              </a>
            }
          </div>
        </div>

        @if (loading) {
          <div class="text-center py-12 text-gray-500">Chargement...</div>
        } @else if (error) {
          <div class="p-4 bg-red-50 border-l-4 border-red-500 rounded-lg text-red-700">{{ error }}</div>
        } @else if (employe) {

          <!-- Carte identité -->
          <div class="bg-white rounded-xl shadow-md p-6 mb-6">
            <div class="flex flex-col sm:flex-row sm:items-center gap-5 mb-5">
              @if (employe.photoUrl) {
                <button
                  type="button"
                  (click)="openPhotoLightbox()"
                  class="group relative flex-shrink-0 rounded-full cursor-zoom-in focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
                  aria-label="Cliquer pour agrandir la photo de l'employé"
                  title="Cliquer pour agrandir"
                  data-testid="employe-photo-button"
                >
                  <img
                    [src]="employe.photoUrl"
                    [alt]="'Photo de ' + employe.nomComplet"
                    (error)="onPhotoError()"
                    class="w-24 h-24 rounded-full object-cover border-2 border-slate-200 transition-transform group-hover:scale-105"
                    data-testid="employe-photo"
                  />
                  <span class="absolute inset-0 flex items-center justify-center rounded-full bg-black/0 text-white opacity-0 transition-opacity group-hover:bg-black/35 group-hover:opacity-100" aria-hidden="true">⌕</span>
                </button>
              } @else {
                <div
                  class="flex-shrink-0 w-24 h-24 rounded-full bg-gradient-to-br from-slate-500 to-slate-700 text-white flex items-center justify-center text-2xl font-bold uppercase"
                  role="img"
                  aria-label="Avatar de l'employé"
                  data-testid="employe-avatar"
                >
                  {{ getEmployeInitials(employe) }}
                </div>
              }

              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-2 mb-3">
                  <span class="font-mono bg-slate-100 px-3 py-1.5 rounded text-sm font-bold">{{ employe.matricule }}</span>
                  @if (employe.actif) {
                    <span class="px-2 py-1 bg-green-100 text-green-800 text-xs font-semibold rounded-full">Actif</span>
                  } @else {
                    <span class="px-2 py-1 bg-red-100 text-red-800 text-xs font-semibold rounded-full">Inactif</span>
                  }
                  @if (employe.fonction) {
                    <span class="px-2 py-1 bg-blue-100 text-blue-800 text-xs font-semibold rounded-full">{{ fonctionLabel }}</span>
                  }
                </div>
                <h2 class="text-2xl font-bold text-gray-900">{{ employe.nomComplet }}</h2>
              </div>
            </div>
            @if (photoError) {
              <p class="mt-4 text-sm text-amber-700" role="alert">{{ photoError }}</p>
            }

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-5 text-sm">
              <div>
                <span class="text-gray-500 text-xs uppercase tracking-wide">Téléphone</span>
                <p class="font-medium text-gray-900 mt-1">{{ employe.telephone || '—' }}</p>
              </div>
              <div>
                <span class="text-gray-500 text-xs uppercase tracking-wide">Adresse</span>
                <p class="font-medium text-gray-900 mt-1">{{ employe.adresse || '—' }}</p>
              </div>
              <div>
                <span class="text-gray-500 text-xs uppercase tracking-wide">Fonction</span>
                <p class="font-medium text-gray-900 mt-1">{{ fonctionLabel || '—' }}</p>
              </div>
              <div>
                <span class="text-gray-500 text-xs uppercase tracking-wide">Agence</span>
                <p class="font-medium text-gray-900 mt-1">{{ employe.nomAgence || '—' }}</p>
              </div>
              <div>
                <span class="text-gray-500 text-xs uppercase tracking-wide">Site</span>
                <p class="font-medium text-gray-900 mt-1">{{ employe.nomSite || '—' }}</p>
              </div>
              <div>
                <span class="text-gray-500 text-xs uppercase tracking-wide">Date d'embauche</span>
                <p class="font-medium text-gray-900 mt-1">{{ employe.dateEmbauche | date:'dd/MM/yyyy' }}</p>
              </div>
              @if (employe.commune) {
                <div>
                  <span class="text-gray-500 text-xs uppercase tracking-wide">Commune</span>
                  <p class="font-medium text-gray-900 mt-1">{{ employe.commune }}</p>
                </div>
              }
            </div>
          </div>

          <!-- Rémunération -->
          <div class="bg-white rounded-xl shadow-md p-6 mb-6">
            <h3 class="text-base font-bold text-gray-800 mb-4">Rémunération</h3>
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-4 text-sm">
              <div>
                <span class="text-gray-500 text-xs uppercase tracking-wide">Salaire de base</span>
                <p class="font-semibold text-gray-900 mt-1">{{ employe.salaireBase | number:'1.0-0' }} FC</p>
              </div>
              @if (employe.primeFixe) {
                <div>
                  <span class="text-gray-500 text-xs uppercase tracking-wide">Prime fixe</span>
                  <p class="font-semibold text-gray-900 mt-1">{{ employe.primeFixe | number:'1.0-0' }} FC</p>
                </div>
              }
              @if (employe.bonusVariable) {
                <div>
                  <span class="text-gray-500 text-xs uppercase tracking-wide">Bonus variable</span>
                  <p class="font-semibold text-gray-900 mt-1">{{ employe.bonusVariable | number:'1.0-0' }} FC</p>
                </div>
              }
              <div class="col-span-2 sm:col-span-3 border-t border-gray-100 pt-3">
                <span class="text-gray-500 text-xs uppercase tracking-wide">Total rémunération</span>
                <p class="text-lg font-bold text-emerald-700 mt-1">{{ employe.totalRemuneration | number:'1.0-0' }} FC</p>
              </div>
            </div>
          </div>

          <!-- Compte utilisateur lié -->
          <div class="bg-white rounded-xl shadow-md p-6">
            <h3 class="text-base font-bold text-gray-800 mb-4">Compte utilisateur</h3>
            @if (employe.utilisateurId) {
              <div class="flex items-center gap-3">
                <span class="text-2xl">✅</span>
                <div>
                  <p class="font-semibold text-gray-900">Compte lié</p>
                  @if (employe.roleUtilisateur) {
                    <p class="text-xs text-gray-500 mt-0.5">Rôle : <span class="font-mono bg-blue-50 px-1.5 py-0.5 rounded text-blue-700">{{ employe.roleUtilisateur }}</span></p>
                  }
                </div>
              </div>
            } @else {
              <div class="flex items-center gap-3">
                <span class="text-2xl">⚠️</span>
                <div>
                  <p class="font-semibold text-gray-600">Pas de compte utilisateur lié</p>
                  <p class="text-xs text-gray-400 mt-0.5">Un compte utilisateur peut être associé séparément selon les règles d'administration.</p>
                </div>
              </div>
            }
          </div>

        }
      </div>
    </div>

    @if (lightboxOpen && employe?.photoUrl) {
      <div
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 p-4"
        role="dialog"
        aria-modal="true"
        aria-label="Photo agrandie de l'employé"
        (click)="closePhotoLightbox()"
        data-testid="employe-photo-lightbox"
      >
        <div class="relative flex max-h-full max-w-full items-center justify-center" (click)="$event.stopPropagation()">
          <img
            [src]="employe!.photoUrl"
            [alt]="'Photo de ' + employe!.nomComplet"
            (error)="onLightboxPhotoError()"
            class="max-h-[85vh] max-w-[90vw] rounded-lg object-contain shadow-2xl"
            data-testid="employe-photo-large"
          />
          <button
            type="button"
            (click)="closePhotoLightbox()"
            class="absolute right-2 top-2 inline-flex h-10 w-10 items-center justify-center rounded-full bg-black/65 text-2xl text-white hover:bg-black/85 focus:outline-none focus:ring-2 focus:ring-white"
            aria-label="Fermer la photo agrandie"
            data-testid="close-photo-lightbox"
          >
            ×
          </button>
        </div>
      </div>
    }
  `
})
export class EmployeDetailComponent implements OnInit {
  private employeService = inject(EmployeService);
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);

  readonly FONCTION_LABELS = FONCTION_LABELS;

  employe: EmployeResponse | null = null;
  loading = false;
  error = '';
  photoError = '';
  lightboxOpen = false;

  get canEdit(): boolean {
    return this.authService.hasAnyRole(['ADMIN']);
  }

  get fonctionLabel(): string {
    if (!this.employe?.fonction) return '';
    return this.FONCTION_LABELS[this.employe.fonction] ?? this.employe.fonction;
  }

  getEmployeInitials(employe: EmployeResponse): string {
    const prenomInitial = employe.prenom?.trim().charAt(0) ?? '';
    const nomInitial = employe.nom?.trim().charAt(0) ?? '';
    return `${prenomInitial}${nomInitial}`.toUpperCase() || 'E';
  }

  onPhotoError(): void {
    this.closePhotoLightbox();
    if (this.employe) {
      this.employe = { ...this.employe, photoUrl: undefined };
      this.photoError = 'Photo importée mais impossible à afficher. Vérifiez l’accès au fichier.';
    }
  }

  openPhotoLightbox(): void {
    if (this.employe?.photoUrl) {
      this.lightboxOpen = true;
    }
  }

  closePhotoLightbox(): void {
    this.lightboxOpen = false;
  }

  onLightboxPhotoError(): void {
    this.closePhotoLightbox();
    this.onPhotoError();
  }

  @HostListener('document:keydown.escape')
  closePhotoLightboxOnEscape(): void {
    this.closePhotoLightbox();
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { this.error = 'ID manquant'; return; }
    this.loading = true;
    this.employeService.getById(+id).subscribe({
      next: (e) => { this.employe = e; this.loading = false; },
      error: () => { this.error = 'Employé introuvable'; this.loading = false; }
    });
  }
}
