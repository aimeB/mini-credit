import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (loading) {
      <div class="flex items-center justify-center py-8">
        <div class="loading-spinner"></div>
        @if (message) {
          <p class="ml-4 text-gray-600">{{ message }}</p>
        }
      </div>
    }
  `,
  styles: []
})
export class LoadingSpinnerComponent {
  @Input() loading = false;
  @Input() message = 'Chargement...';
}
