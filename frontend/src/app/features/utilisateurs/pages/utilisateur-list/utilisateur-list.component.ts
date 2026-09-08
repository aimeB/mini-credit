import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { UtilisateurService } from '../../services/utilisateur.service';
import { UtilisateurResponse } from '../../models/utilisateur-response';
import { ROLE_LABELS } from '../../models/role-enum';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-utilisateur-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './utilisateur-list.component.html'
})
export class UtilisateurListComponent implements OnInit {
  private utilisateurService = inject(UtilisateurService);
  private authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  utilisateurs: UtilisateurResponse[] = [];
  loading = false;
  error = '';
  success = '';
  temporaryPasswordVisibleForUserId: number | null = null;
  temporaryPasswordValue = '';
  roleLabels = ROLE_LABELS;

  readonly protectedRolesForChefBureau = ['ADMIN', 'RCI', 'COO', 'GERANT_GENERAL'];

  ngOnInit(): void {
    this.chargerUtilisateurs();
  }

  chargerUtilisateurs(): void {
    this.loading = true;
    this.error = '';
    this.success = '';

    this.utilisateurService.getAll()
      .pipe(
        finalize(() => {
          this.loading = false;
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (utilisateurs) => {
          this.utilisateurs = utilisateurs;
        },
        error: () => {
          this.error = 'Erreur lors du chargement des utilisateurs';
        }
      });
  }

  supprimer(id: number, nom: string): void {
    if (!confirm(`Êtes-vous sûr de vouloir supprimer ${nom} ?`)) {
      return;
    }

    this.utilisateurService.delete(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.success = 'Utilisateur supprimé avec succès !';
          this.chargerUtilisateurs();
        },
        error: () => {
          this.error = 'Erreur lors de la suppression de l\'utilisateur';
        }
      });
  }

  getRoleLabel(role: string): string {
    return this.roleLabels[role] || role;
  }

  getRoleColor(role: string): string {
    const colors: Record<string, string> = {
      'ADMIN': 'bg-red-100 text-red-800',
      'CREDIT_MANAGER': 'bg-blue-100 text-blue-800',
      'CAISSIER': 'bg-green-100 text-green-800',
      'AGENT_TERRAIN': 'bg-amber-100 text-amber-800',
      'SUPERVISEUR': 'bg-purple-100 text-purple-800'
    };
    return colors[role] || 'bg-gray-100 text-gray-800';
  }

  canResetPassword(target: UtilisateurResponse): boolean {
    const current = this.authService.getCurrentUser();
    if (!current) {
      return false;
    }

    if (current.role === 'ADMIN') {
      return true;
    }

    if (current.role !== 'CHEF_BUREAU' && current.role !== 'CHEF_BUREAU') {
      return false;
    }

    const targetRoles = target.roles || [];
    if (targetRoles.some(r => this.protectedRolesForChefBureau.includes(r))) {
      return false;
    }

    return current.siteId != null && target.employeSiteNom != null && !!target.employeSiteNom;
  }

  resetPassword(target: UtilisateurResponse): void {
    this.error = '';
    this.success = '';
    this.temporaryPasswordVisibleForUserId = null;
    this.temporaryPasswordValue = '';

    const motif = window.prompt('Motif de réinitialisation (obligatoire) :');
    if (!motif || motif.trim().length < 5) {
      this.error = 'Motif obligatoire (min 5 caractères).';
      return;
    }

    this.utilisateurService.resetPassword(target.id, motif.trim())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.success = response.advisoryMessage
            ? `${response.advisoryMessage} (Compte: ${response.username})`
            : `Mot de passe temporaire généré pour ${response.username}. Affiché une seule fois ci-dessous.`;
          this.temporaryPasswordVisibleForUserId = target.id;
          this.temporaryPasswordValue = response.temporaryPassword;
        },
        error: (err) => {
          this.error = err?.error?.message || 'Erreur lors de la réinitialisation du mot de passe.';
        }
      });
  }

  masquerMotDePasseTemporaire(): void {
    this.temporaryPasswordVisibleForUserId = null;
    this.temporaryPasswordValue = '';
  }
}
