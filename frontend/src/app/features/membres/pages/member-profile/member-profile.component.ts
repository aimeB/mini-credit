import { Component, OnInit, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { finalize, filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MembreService } from '../../services/membre.service';
import { CreditService } from '../../../credit/services/credit.service';
import { CompteEpargneService } from '../../../epargne/services/compte-epargne.service';
import { GarantieService } from '../../../garanties/services/garantie.service';
import { AuthService } from '../../../../core/services/auth.service';
import { UsernameService } from '../../../../core/services/username.service';
import { PasswordChangeService } from '../../../../core/services/password-change.service';

import { MembreResponse } from '../../models/membre-response';

@Component({
  selector: 'app-member-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './member-profile.component.html',
  styleUrls: []
})
export class MemberProfileComponent implements OnInit {
  
  private membreService = inject(MembreService);
  private creditService = inject(CreditService);
  private compteEpargneService = inject(CompteEpargneService);
  private garantieService = inject(GarantieService);
  private authService = inject(AuthService);
  private usernameService = inject(UsernameService);
  private passwordChangeService = inject(PasswordChangeService);
  private destroyRef = inject(DestroyRef);
  private router = inject(Router);
  private activatedRoute = inject(ActivatedRoute);

  // Data
  currentMember: MembreResponse | null = null;
  currentUsername = '';
  currentUserEmail = '';
  credits: any[] = [];
  comptes: any[] = [];
  garanties: any[] = [];

  // Loading states
  loading = false;
  loadingCredits = false;
  loadingComptes = false;
  loadingGaranties = false;

  // Modal states
  showChangeUsername = false;
  showChangePassword = false;
  passwordMode: 'change' | 'reset' = 'change'; // change = avec ancien, reset = sans ancien

  // Form data
  newUsername = '';
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';

  // Change states
  changingUsername = false;
  changingPassword = false;

  // Messages
  error = '';
  errorCredits = '';
  errorComptes = '';
  errorGaranties = '';
  usernameChangeError = '';
  usernameChangeSuccess = '';
  passwordChangeError = '';
  passwordChangeSuccess = '';

  ngOnInit(): void {
    // Charger les données au démarrage
    this.loadCurrentUser();
    this.loadMemberData();

    // Recharger les données chaque fois qu'on navigue vers cette route (même en réutilisant le composant)
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        filter((event) => !!event.url && event.url.includes('/member-profile')),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        // Donner un peu de temps pour que le composant soit bien initialisé
        setTimeout(() => {
          this.loadCurrentUser();
          this.loadMemberData();
        }, 100);
      });
  }

  private loadCurrentUser(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.currentUsername = user.username;
      this.currentUserEmail = user.email || 'N/A';
    }
  }

  loadMemberData(): void {
    this.loading = true;
    this.error = '';

    this.membreService.getCurrentMember().pipe(
      finalize(() => {
        this.loading = false;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (data) => {
        this.currentMember = data;
        if (data.id) {
          this.loadCredits(data.id);
          this.loadComptes(data.id);
          this.loadGaranties(data.id);
        }
      },
      error: () => {
        this.error = 'Erreur lors du chargement du profil';
      }
    });
  }

  loadCredits(membreId: number): void {
    this.loadingCredits = true;
    this.errorCredits = '';

    this.creditService.getByMembre(membreId).pipe(
      finalize(() => {
        this.loadingCredits = false;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (data) => {
        this.credits = data || [];
      },
      error: () => {
        this.errorCredits = 'Erreur lors du chargement des crédits';
      }
    });
  }

  loadComptes(membreId: number): void {
    this.loadingComptes = true;
    this.errorComptes = '';

    this.compteEpargneService.getByMembre(membreId).pipe(
      finalize(() => {
        this.loadingComptes = false;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (data) => {
        this.comptes = data || [];
      },
      error: () => {
        this.errorComptes = 'Erreur lors du chargement des comptes épargne';
      }
    });
  }

  loadGaranties(membreId: number): void {
    this.loadingGaranties = true;
    this.errorGaranties = '';

    this.garantieService.getByMembreId(membreId).pipe(
      finalize(() => {
        this.loadingGaranties = false;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (data) => {
        this.garanties = data || [];
      },
      error: () => {
        this.errorGaranties = 'Erreur lors du chargement des garanties';
      }
    });
  }

  getStatutBadgeColor(statut?: string): string {
    switch (statut) {
      case 'ACTIF':
        return 'bg-emerald-100 text-emerald-800';
      case 'SUSPENDU':
        return 'bg-amber-100 text-amber-800';
      case 'CLOTURE':
        return 'bg-gray-100 text-gray-800';
      case 'BLOQUE':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-blue-100 text-blue-800';
    }
  }

  getCreditStatutBadgeColor(statut?: string): string {
    switch (statut) {
      case 'APPROUVE':
        return 'bg-emerald-100 text-emerald-800';
      case 'DECAISSE':
        return 'bg-blue-100 text-blue-800';
      case 'EN_COURS':
        return 'bg-blue-100 text-blue-800';
      case 'REMBOURSE':
        return 'bg-gray-100 text-gray-800';
      case 'EN_RETARD':
        return 'bg-red-100 text-red-800';
      case 'ANNULE':
        return 'bg-gray-100 text-gray-800';
      default:
        return 'bg-slate-100 text-slate-800';
    }
  }

  getEpargneStatutBadgeColor(statut?: string): string {
    switch (statut) {
      case 'ACTIF':
        return 'bg-emerald-100 text-emerald-800';
      case 'BLOQUE':
        return 'bg-amber-100 text-amber-800';
      case 'INACTIF':
        return 'bg-gray-100 text-gray-800';
      case 'FERME':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-blue-100 text-blue-800';
    }
  }

  getGarantieStatutBadgeColor(statut?: string): string {
    switch (statut) {
      case 'ACTIF':
        return 'bg-emerald-100 text-emerald-800';
      case 'EN_ATTENTE':
        return 'bg-amber-100 text-amber-800';
      case 'LIBEREE':
        return 'bg-gray-100 text-gray-800';
      case 'SAISIE':
        return 'bg-red-100 text-red-800';
      case 'REALISEE':
        return 'bg-orange-100 text-orange-800';
      default:
        return 'bg-blue-100 text-blue-800';
    }
  }

  formatCurrency(amount?: number): string {
    if (!amount) return '0,00 FC';
    return new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount) + ' FC';
  }

  // ===== Modal Methods =====

  toggleChangeUsername(): void {
    this.showChangeUsername = !this.showChangeUsername;
    this.newUsername = '';
    this.usernameChangeError = '';
    this.usernameChangeSuccess = '';
  }

  toggleChangePassword(): void {
    this.showChangePassword = !this.showChangePassword;
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.passwordChangeError = '';
    this.passwordChangeSuccess = '';
  }

  submitChangeUsername(): void {
    if (!this.newUsername.trim()) {
      this.usernameChangeError = 'Veuillez entrer un nouveau username';
      return;
    }

    if (this.newUsername.trim() === this.currentUsername) {
      this.usernameChangeError = 'Le nouveau username doit être différent de l\'actuel';
      return;
    }

    this.changingUsername = true;
    this.usernameChangeError = '';
    this.usernameChangeSuccess = '';

    const userId = this.authService.getCurrentUser()?.id;
    if (!userId) {
      this.usernameChangeError = 'Erreur: Utilisateur non trouvé';
      this.changingUsername = false;
      return;
    }

    this.usernameService.changeUsername(userId, { newUsername: this.newUsername })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.changingUsername = false;
          this.usernameChangeSuccess = `✅ Username changé avec succès! Votre nouveau username est "${response.username}"`;
          this.currentUsername = response.username;

          // Fermer le modal après succès
          setTimeout(() => {
            this.toggleChangeUsername();
          }, 2000);
        },
        error: (err) => {
          this.changingUsername = false;
          this.usernameChangeError = err?.error?.message || 'Erreur lors du changement de username';
        }
      });
  }

  submitChangePassword(): void {
    // Mode "Changer" - validation
    if (this.passwordMode === 'change') {
      if (!this.currentPassword.trim()) {
        this.passwordChangeError = 'Veuillez entrer votre mot de passe actuel';
        return;
      }
    }

    // Validations communes
    if (!this.newPassword.trim()) {
      this.passwordChangeError = 'Veuillez entrer un nouveau mot de passe';
      return;
    }

    if (this.newPassword.length < 8) {
      this.passwordChangeError = 'Le nouveau mot de passe doit avoir au moins 8 caractères';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.passwordChangeError = 'Les mots de passe ne correspondent pas';
      return;
    }

    if (this.passwordMode === 'change' && this.newPassword === this.currentPassword) {
      this.passwordChangeError = 'Le nouveau mot de passe doit être différent de l\'actuel';
      return;
    }

    this.changingPassword = true;
    this.passwordChangeError = '';
    this.passwordChangeSuccess = '';

    const userId = this.authService.getCurrentUser()?.id;
    if (!userId) {
      this.passwordChangeError = 'Erreur: Utilisateur non trouvé';
      this.changingPassword = false;
      return;
    }

    if (this.passwordMode === 'change') {
      // Mode "Changer" - avec ancien mot de passe
      this.passwordChangeService.changePassword(userId, {
        currentPassword: this.currentPassword,
        newPassword: this.newPassword,
        confirmPassword: this.confirmPassword
      })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (response) => {
            this.changingPassword = false;
            this.passwordChangeSuccess = `✅ Mot de passe changé avec succès!`;
            
            // Clear form
            this.currentPassword = '';
            this.newPassword = '';
            this.confirmPassword = '';

            // Fermer le modal après succès
            setTimeout(() => {
              this.toggleChangePassword();
            }, 2000);
          },
          error: (err) => {
            this.changingPassword = false;
            this.passwordChangeError = err?.error?.message || 'Erreur lors du changement de mot de passe';
          }
        });
    } else {
      // Mode "Réinitialiser" - sans ancien mot de passe
      this.passwordChangeService.resetPasswordSelf(userId, {
        newPassword: this.newPassword,
        confirmPassword: this.confirmPassword
      })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (response) => {
            this.changingPassword = false;
            this.passwordChangeSuccess = `✅ ${response.message}`;
            
            // Clear form
            this.currentPassword = '';
            this.newPassword = '';
            this.confirmPassword = '';

            // Fermer le modal après succès
            setTimeout(() => {
              this.toggleChangePassword();
            }, 2000);
          },
          error: (err) => {
            this.changingPassword = false;
            this.passwordChangeError = err?.error?.message || 'Erreur lors de la réinitialisation du mot de passe';
          }
        });
    }
  }
}
