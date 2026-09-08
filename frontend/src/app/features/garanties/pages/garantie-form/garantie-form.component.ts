import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Subject, Subscription } from 'rxjs';

@Component({
  selector: 'app-garantie-form',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './garantie-form.component.html',
  styleUrls: ['./garantie-form.component.css']
})
export class GarantieFormComponent implements OnInit, OnDestroy {
  loading = false;

  private destroy$ = new Subject<void>();
  private subscriptions = new Subscription();

  constructor(public router: Router) {}

  ngOnInit(): void {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.subscriptions.unsubscribe();
  }
}
