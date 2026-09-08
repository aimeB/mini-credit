import { Component, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from './shared/components/navbar/navbar.component';


@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, CommonModule],
  templateUrl: './app.component.html'
})
export class AppComponent {
  title = 'mini-credit-front';
  public router = inject(Router);

  isDemoBelfius(): boolean {
    return this.router.url.startsWith('/demo/belfius-history');
  }
}