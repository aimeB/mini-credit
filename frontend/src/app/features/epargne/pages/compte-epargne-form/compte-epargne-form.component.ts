import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CompteEpargneService } from '../../services/compte-epargne.service';
import { TypeCompteEpargne } from '../../models/type-compte-epargne';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-compte-epargne-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './compte-epargne-form.component.html'
})
export class CompteEpargneFormComponent {
  @Input() membreId!: number;
  @Output() saved = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private compteService = inject(CompteEpargneService);
  private authService = inject(AuthService);

  typesCompte: TypeCompteEpargne[] = ['COTISATION', 'EPARGNE_VOLONTAIRE', 'MIXTE'];
  loading = false;
  errorMessage = '';

  form = this.fb.group({
    typeCompte: this.fb.control<TypeCompteEpargne | null>(null, Validators.required),
    dateOuverture: ['', Validators.required]
  });

  submit(): void {
    if (!this.canCreateMissingAccount) {
      this.errorMessage = 'Action reservee a l\'administrateur.';
      return;
    }

    if (this.form.invalid || !this.membreId) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.compteService.create({
      membreId: this.membreId,
      typeCompte: this.form.value.typeCompte!,
      dateOuverture: this.form.value.dateOuverture!
    }).subscribe({
      next: () => {
        this.loading = false;
        this.form.reset();
        this.saved.emit();
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Erreur lors de la création du compte';
      }
    });
  }

  get canCreateMissingAccount(): boolean {
    return this.authService.hasRole('ADMIN');
  }
}