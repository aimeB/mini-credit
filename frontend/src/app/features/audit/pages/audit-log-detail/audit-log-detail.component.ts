import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuditService } from '../../services/audit.service';
import { AuditLog } from '../../models/audit-log.model';

@Component({
  selector: 'app-audit-log-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './audit-log-detail.component.html',
  styleUrls: ['./audit-log-detail.component.css']
})
export class AuditLogDetailComponent implements OnInit, OnDestroy {
  loading = false;
  error: string | null = null;
  log: AuditLog | null = null;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly auditService: AuditService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id || Number.isNaN(id)) {
      this.error = 'Identifiant de log invalide';
      return;
    }

    this.loading = true;
    this.auditService.getById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (log) => {
          this.log = log;
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Erreur lors du chargement du détail audit';
          this.loading = false;
          console.error(err);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
