import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card" [class]="className">
      <div class="card-header" *ngIf="title">
        <h3>{{ title }}</h3>
      </div>
      <ng-content></ng-content>
    </div>
  `,
  styles: []
})
export class CardComponent {
  @Input() title: string | null = null;
  @Input() className = '';
}
