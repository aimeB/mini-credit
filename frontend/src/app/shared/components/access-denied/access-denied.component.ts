import { CommonModule, Location } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './access-denied.component.html'
})
export class AccessDeniedComponent {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private location = inject(Location);

  title = "Vous n'êtes pas autorisé à effectuer cette action.";
  detail = '';
  requestUrl = '';
  supportMessage = "Consultez le référent de l'étape ou l'administrateur si vous pensez qu'il s'agit d'une erreur de paramétrage.";

  constructor() {
    const params = this.route.snapshot.queryParamMap;
    this.title = params.get('title') || this.title;
    this.detail = params.get('detail') || '';
    this.requestUrl = params.get('requestUrl') || '';
  }

  goHome(): void {
    this.router.navigate(['/dashboard']);
  }

  goPrevious(): void {
    this.location.back();
  }
}
