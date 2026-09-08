import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

import { WorkflowGuidance } from '../../models/workflow-guidance.model';

@Component({
  selector: 'app-workflow-guidance-banner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section *ngIf="guidance" class="rounded-xl border p-4 mb-4" [ngClass]="containerClass">
      <div class="flex items-start justify-between gap-4 flex-wrap">
        <div>
          <h3 class="text-base font-bold">{{ guidance.title }}</h3>
          <p class="mt-1 text-sm">{{ guidance.message }}</p>
        </div>
        <span class="rounded-full px-3 py-1 text-xs font-semibold" [ngClass]="badgeClass">
          {{ severityLabel }}
        </span>
      </div>

      <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-4 mt-4 text-sm">
        <div>
          <p class="font-semibold">État actuel</p>
          <p>{{ guidance.currentStep }}</p>
        </div>
        <div *ngIf="guidance.nextStep">
          <p class="font-semibold">Étape suivante</p>
          <p>{{ guidance.nextStep }}</p>
        </div>
        <div *ngIf="guidance.expectedRole">
          <p class="font-semibold">Rôle attendu</p>
          <p>{{ guidance.expectedRole }}</p>
        </div>
        <div *ngIf="guidance.expectedAction">
          <p class="font-semibold">Action attendue</p>
          <p>{{ guidance.expectedAction }}</p>
        </div>
      </div>

      <div *ngIf="!guidance.canCurrentUserAct && guidance.blockedReason" class="mt-4 rounded-lg border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-800">
        <p class="font-semibold">Vous ne pouvez pas agir à cette étape</p>
        <p>{{ guidance.blockedReason }}</p>
      </div>

      <div *ngIf="guidance.canCurrentUserAct" class="mt-4 rounded-lg border border-emerald-300 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
        <p class="font-semibold">Vous pouvez agir</p>
        <p>{{ guidance.expectedAction || 'Vous êtes le rôle attendu pour cette étape.' }}</p>
      </div>

      <div *ngIf="guidance.successMessage" class="mt-4 rounded-lg border border-emerald-300 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
        {{ guidance.successMessage }}
      </div>
    </section>
  `
})
export class WorkflowGuidanceBannerComponent {
  @Input() guidance?: WorkflowGuidance | null;

  get containerClass(): string {
    switch (this.guidance?.severity) {
      case 'success':
        return 'border-emerald-200 bg-emerald-50 text-emerald-900';
      case 'warning':
        return 'border-amber-200 bg-amber-50 text-amber-900';
      case 'danger':
        return 'border-red-200 bg-red-50 text-red-900';
      default:
        return 'border-blue-200 bg-blue-50 text-blue-900';
    }
  }

  get badgeClass(): string {
    switch (this.guidance?.severity) {
      case 'success':
        return 'bg-emerald-100 text-emerald-800';
      case 'warning':
        return 'bg-amber-100 text-amber-800';
      case 'danger':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-blue-100 text-blue-800';
    }
  }

  get severityLabel(): string {
    switch (this.guidance?.severity) {
      case 'success':
        return 'Validation';
      case 'warning':
        return 'Contrôle requis';
      case 'danger':
        return 'Blocage';
      default:
        return 'Information';
    }
  }
}