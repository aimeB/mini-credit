import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { SessionAuditEvent } from '../../models/session-audit.model';

@Component({
  selector: 'app-session-timeline',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './session-timeline.component.html'
})
export class SessionTimelineComponent {
  @Input() events: SessionAuditEvent[] = [];

  formatDate(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date);
  }
}
