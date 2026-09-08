import { Component, HostListener, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { Subject, merge, of } from 'rxjs';
import { catchError, map, takeUntil } from 'rxjs/operators';
import { AuthService, User } from '../../../core/services/auth.service';
import { PermissionCode } from '../../enums/permission-code.enum';
import { WorkflowTaskService } from '../../services/workflow-task.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <nav class="sticky top-0 z-50 bg-gradient-to-r from-slate-900 via-blue-900 to-slate-900 text-white shadow-2xl" (click)="$event.stopPropagation()">
      <!-- Pas de max-w : la navbar occupe toute la largeur de l'écran -->
      <div class="w-full px-3 sm:px-5 lg:px-8">

        <!-- Ligne principale : 3 zones flex -->
        <div class="flex items-center h-14 gap-2">

          <!-- ① LOGO — flex-shrink-0 : jamais écrasé -->
          <div class="flex items-center gap-2 flex-shrink-0">
            <img src="/images/3n-logo.jpg" alt="3N Logo" class="navbar-logo">
            <span class="text-xs font-bold text-white hidden xl:inline whitespace-nowrap">Gestion Financière</span>
          </div>

          <div class="hidden md:block w-px h-5 bg-white/20 flex-shrink-0"></div>

          <!-- ② MENU CENTRAL — les modules prioritaires restent visibles, les autres sont dans Plus -->
          <div class="hidden xl:flex flex-1 min-w-0 items-center gap-px">

            <!-- Tableau de Bord -->
            <a routerLink="/dashboard"
               routerLinkActive="bg-white/20"
               [routerLinkActiveOptions]="{ exact: true }"
               class="flex-shrink-0 px-2 py-1.5 rounded-md text-xs font-medium text-white hover:bg-white/10 transition-colors flex items-center gap-1">
              <svg class="w-3.5 h-3.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path>
              </svg>
              <span class="hidden lg:inline whitespace-nowrap">Accueil</span>
            </a>

            <!-- Modules dynamiques -->
            <ng-container *ngFor="let item of primaryModules">
              <a [routerLink]="getModuleRoute(item)"
                  [queryParams]="getModuleQueryParams(item)"
                 routerLinkActive="bg-white/20"
                 class="flex-shrink-0 px-2 py-1.5 rounded-md text-xs font-medium text-white hover:bg-white/10 transition-colors flex items-center gap-1"
                 [ngClass]="getNavItemClasses(item)">
                <span class="text-sm leading-none">{{ item.icon }}</span>
                <span class="hidden lg:inline whitespace-nowrap">{{ item.navLabel }}</span>
              </a>
            </ng-container>

            <div class="relative flex-shrink-0" *ngIf="overflowModules.length > 0">
              <button
                type="button"
                (click)="toggleMoreMenu()"
                [attr.aria-expanded]="moreMenuOpen"
                aria-haspopup="true"
                class="flex items-center gap-1 rounded-md px-2 py-1.5 text-xs font-medium text-white hover:bg-white/10 transition-colors">
                <span>Plus</span>
                <span aria-hidden="true">⌄</span>
              </button>
              <div *ngIf="moreMenuOpen" class="absolute right-0 top-full z-50 mt-2 max-h-[min(70vh,28rem)] w-56 overflow-y-auto rounded-lg border border-white/10 bg-slate-900 p-1 shadow-xl">
                <a *ngFor="let item of overflowModules"
                   [routerLink]="getModuleRoute(item)"
                   [queryParams]="getModuleQueryParams(item)"
                   (click)="closeMenus()"
                   routerLinkActive="bg-white/20"
                   class="block rounded-md px-3 py-2 text-xs font-medium text-white hover:bg-white/10">
                  <span class="mr-1" aria-hidden="true">{{ item.icon }}</span>{{ item.navLabel }}
                </a>
              </div>
            </div>
          </div>

          <!-- ③ BLOC DROIT — flex-shrink-0 + ml-auto : TOUJOURS visible, ne rétrécit jamais -->
          <div class="flex-shrink-0 flex items-center gap-1.5 ml-auto">

            <a routerLink="/mes-actions"
               routerLinkActive="bg-white/20"
               class="relative flex-shrink-0 px-2.5 py-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-white text-xs font-semibold transition-colors flex items-center gap-1"
               *ngIf="canShowTaskArea()">
              <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V4a2 2 0 10-4 0v1.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"></path>
              </svg>
              <span class="hidden sm:inline whitespace-nowrap">Actions</span>
              <span *ngIf="taskCount > 0"
                class="absolute -top-1 -right-1 min-w-[1.15rem] h-[1.15rem] px-1 rounded-full bg-red-600 text-white text-[10px] leading-[1.15rem] text-center font-bold">
                {{ taskCount > 99 ? '99+' : taskCount }}
              </span>
            </a>

            <!-- Info utilisateur -->
            <div class="hidden sm:flex items-center gap-1.5 px-2 py-1 rounded-lg bg-white/5" *ngIf="currentUser">
              <div class="w-7 h-7 bg-gradient-to-br from-blue-400 to-emerald-400 rounded-full flex-shrink-0 flex items-center justify-center text-xs font-bold">
                {{ (currentUser.username || currentUser.nomComplet || 'A').charAt(0).toUpperCase() }}
              </div>
              <div class="hidden xl:block min-w-0">
                <p class="text-xs font-semibold truncate max-w-[120px]">{{ currentUser.nomComplet }}</p>
                <p class="text-xs text-gray-300 truncate max-w-[120px]">{{ getRoleLabel(currentUser.role, currentUser.posteEmploye) }}</p>
              </div>
            </div>

            <!-- Bouton Déconnexion — toujours visible, ne rétrécit jamais -->
            <button
              (click)="logout()"
              class="flex-shrink-0 px-2.5 py-1.5 rounded-lg bg-red-600 hover:bg-red-700 text-white text-xs font-semibold transition-colors flex items-center gap-1">
              <svg class="w-3.5 h-3.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path>
              </svg>
              <span class="hidden sm:inline whitespace-nowrap">Déconnexion</span>
            </button>

            <!-- Bouton menu mobile -->
            <button
              (click)="toggleMobileMenu()"
              aria-label="Ouvrir le menu de navigation"
              [attr.aria-expanded]="mobileMenuOpen"
              class="xl:hidden inline-flex items-center justify-center p-1.5 rounded-lg hover:bg-white/10 transition-colors flex-shrink-0">
              @if (!mobileMenuOpen) {
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
                </svg>
              } @else {
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
              }
            </button>
          </div>

        </div><!-- fin ligne principale -->

        <!-- Menu mobile déroulant -->
        @if (mobileMenuOpen) {
          <div class="xl:hidden border-t border-white/10 py-3 space-y-1">
            <a routerLink="/dashboard"
               (click)="closeMenus()"
               routerLinkActive="bg-white/20"
               class="block px-3 py-2 rounded-lg text-sm font-medium hover:bg-white/10 transition-colors">
              🏠 Tableau de Bord
            </a>
            <ng-container *ngFor="let item of visibleModules">
              <a [routerLink]="getModuleRoute(item)"
                  [queryParams]="getModuleQueryParams(item)"
                 (click)="closeMenus()"
                 routerLinkActive="bg-white/20"
                 class="block px-3 py-2 rounded-lg text-sm font-medium hover:bg-white/10 transition-colors">
                {{ item.icon }} {{ item.navLabel }}
              </a>
            </ng-container>
            <a routerLink="/mes-actions"
               (click)="closeMenus()"
               routerLinkActive="bg-white/20"
               class="block px-3 py-2 rounded-lg text-sm font-medium hover:bg-white/10 transition-colors"
               *ngIf="canShowTaskArea()">
              🔔 Mes actions
              <span *ngIf="taskCount > 0" class="ml-2 px-1.5 py-0.5 rounded-full bg-red-600 text-white text-xs">{{ taskCount > 99 ? '99+' : taskCount }}</span>
            </a>
            <div class="border-t border-white/10 pt-2 mt-2" *ngIf="currentUser">
              <p class="px-3 text-xs text-gray-400 mb-1">{{ currentUser.nomComplet }} · {{ getRoleLabel(currentUser.role, currentUser.posteEmploye) }}</p>
              <button (click)="logout()"
                class="w-full text-left px-3 py-2 rounded-lg text-sm font-medium text-red-400 hover:bg-white/10 transition-colors flex items-center gap-2">
                🚪 Déconnexion
              </button>
            </div>
          </div>
        }

      </div><!-- fin w-full -->
    </nav>
  `,
  styles: []
})
export class NavbarComponent implements OnInit, OnDestroy {
  private router = inject(Router);
  private authService = inject(AuthService);
  private workflowTaskService = inject(WorkflowTaskService);
  private destroy$ = new Subject<void>();
  
  mobileMenuOpen = false;
  moreMenuOpen = false;
  currentUser: User | null = null;
  visibleModules: any[] = [];
  taskCount = 0;

  get primaryModules(): any[] {
    return this.visibleModules.slice(0, 8);
  }

  get overflowModules(): any[] {
    return this.visibleModules.slice(8);
  }

  private normalizeRole(role: string | null | undefined): string {
    let normalized = (role ?? '').trim().toUpperCase();
    if (normalized.startsWith('ROLE_')) {
      normalized = normalized.slice(5);
    }
    return normalized;
  }

  modules = [
    {
      title: 'Gestion des Membres',
      navLabel: 'Membres',
      icon: '👥',
      route: '/membres',
      roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN'],
      color: 'blue'
    },
    {
      title: 'Gestion des Crédits',
      navLabel: 'Crédits',
      icon: '💰',
      route: '/credits',
      roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN', 'CONTROLEUR'],
      color: 'emerald'
    },
    {
      title: 'Gestion de la Caisse',
      navLabel: 'Caisse',
      icon: '🏦',
      route: '/caisses',
      roles: ['ADMIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU'],
      color: 'orange'
    },
    {
      title: 'Retraits épargne à payer',
      navLabel: 'Retraits épargne',
      icon: '💵',
      route: '/epargne/demandes-retrait',
      queryParams: { statut: 'VALIDEE' },
      roles: ['CAISSIER'],
      color: 'yellow'
    },
    {
      title: 'Frais crédit à encaisser',
      navLabel: 'Frais crédit',
      icon: '💳',
      route: '/credits/frais-a-encaisser',
      roles: ['CAISSIER'],
      color: 'emerald'
    },
    {
      title: 'Nouveau retrait épargne',
      navLabel: 'Nouveau retrait',
      icon: '🧾',
      route: '/epargne/demandes-retrait/nouveau',
      roles: ['CAISSIER'],
      color: 'cyan'
    },
    {
      title: 'Rapports Caisse',
      navLabel: 'Rapports caisse',
      icon: '📈',
      route: '/caisses/rapports/journalier',
      roles: ['ADMIN', 'GERANT_GENERAL', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU', 'RCI', 'CHEF_BUREAU'],
      color: 'indigo'
    },
    {
      title: 'Épargne',
      navLabel: 'Épargne',
      icon: '💸',
      route: '/epargne',
      roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN', 'CONTROLEUR'],
      color: 'yellow'
    },
    {
      title: 'Pré-analyses crédit',
      navLabel: 'Pré-analyses',
      icon: '🧮',
      route: '/credits/pre-analyses',
      roles: ['GESTIONNAIRE'],
      color: 'emerald'
    },
    {
      title: 'Collecte Terrain',
      navLabel: 'Ma collecte',
      icon: '🧾',
      route: '/collectes/ma-collecte',
      roles: ['ADMIN', 'AGENT_TERRAIN', 'CHEF_BUREAU', 'CONTROLEUR', 'CAISSIER', 'GESTIONNAIRE'],
      roleRoutes: {
        AGENT_TERRAIN: '/collectes/ma-collecte',
        CAISSIER: '/collectes/soumises-billetage',
        CONTROLEUR: '/collectes/a-controler',
        CHEF_BUREAU: '/collectes/suivi-terrain',
        ADMIN: '/collectes/a-controler',
        GESTIONNAIRE: '/collectes/suivi-terrain'
      },
      color: 'green'
    },
    {
      title: 'Mes Collectes',
      navLabel: 'Mes collectes',
      icon: '📒',
      route: '/collectes/mes-collectes',
      roles: ['AGENT_TERRAIN'],
      color: 'green'
    },
    {
      title: 'Reporting terrain & collectes',
      navLabel: 'Collectes terrain',
      icon: '🗃️',
      route: '/recettes',
      roles: ['ADMIN', 'GESTIONNAIRE', 'CONTROLEUR', 'CHEF_BUREAU', 'COO', 'RCI', 'GERANT_GENERAL'],
      color: 'cyan'
    },
    {
      title: 'Gestion des Utilisateurs',
      navLabel: 'Utilisateurs',
      icon: '👤',
      route: '/utilisateurs',
      roles: ['ADMIN'],
      color: 'red'
    },
    {
      title: 'Organisation',
      navLabel: 'Organisation',
      icon: '🏢',
      route: '/agences',
      roles: ['ADMIN'],
      color: 'sky'
    },
    {
      title: 'Agents Terrain',
      navLabel: 'Agents',
      icon: '👥',
      route: '/admin/agents',
      roles: ['ADMIN'],
      color: 'indigo'
    },
    {
      title: 'Gestion du Personnel',
      navLabel: 'Personnel',
      icon: '👔',
      route: '/employes',
      roles: ['ADMIN'],
      color: 'fuchsia'
    },
    {
      title: 'Garanties crédit',
      navLabel: 'Garanties',
      icon: '🔒',
      route: '/garanties',
      roles: ['ADMIN', 'CHEF_BUREAU', 'CONTROLEUR', 'RCI'],
      color: 'purple'
    },
    {
      title: 'Rapports & Analytics',
      navLabel: 'Rapports',
      icon: '📊',
      route: '/rapports',
      roles: ['ADMIN', 'GERANT_GENERAL', 'CHEF_BUREAU', 'GESTIONNAIRE'],
      color: 'indigo'
    },
    {
      title: 'Rapport des revenus',
      navLabel: 'Revenus',
      icon: '📈',
      route: '/rapports/revenus',
      roles: ['ADMIN', 'GERANT_GENERAL', 'COO', 'RCI', 'CHEF_BUREAU', 'CONTROLEUR'],
      color: 'emerald'
    },
    {
      title: 'Performances terrain',
      navLabel: 'Performances',
      icon: '📊',
      route: '/performances-terrain',
      roles: [],
      color: 'indigo'
    },
    {
      title: 'Réclamations',
      navLabel: 'Réclamations',
      icon: '💬',
      route: '/reclamations',
      roles: [],
      color: 'cyan'
    },
    {
      title: 'Mon Profil',
      navLabel: 'Mon Profil',
      icon: '👤',
      route: '/member-profile',
      roles: ['MEMBER'],
      color: 'blue'
    },
    {
      title: 'Audit & Logs',
      navLabel: 'Audit',
      icon: '🔐',
      route: '/audit/logs',
      roles: ['ADMIN', 'GERANT_GENERAL', 'RCI', 'CHEF_BUREAU', 'CONTROLEUR'],
      color: 'cyan'
    },
    {
      title: 'Contrôle Interne',
      navLabel: 'Supervision',
      icon: '🛡️',
      route: '/dashboard/controle-interne',
      roles: ['ADMIN', 'GERANT_GENERAL', 'RCI', 'CHEF_BUREAU', 'CONTROLEUR'],
      permissions: [PermissionCode.DASHBOARD_CONTROLE_INTERNE_READ],
      color: 'cyan'
    }
  ];

  ngOnInit(): void {
    this.authService.currentUser$.pipe(takeUntil(this.destroy$)).subscribe(user => {
      this.currentUser = user;
      this.updateVisibleModules();
      this.refreshTaskCount();
    });

    merge(this.workflowTaskService.refreshCount$, of(undefined))
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.refreshTaskCount());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  updateVisibleModules(): void {
    if (this.currentUser) {
      const effectiveRole = this.normalizeRole(this.currentUser.role);
      this.visibleModules = this.modules.filter(module => {
        const normalizedModuleRoles = (module.roles as string[]).map(role => this.normalizeRole(role));
        if (!normalizedModuleRoles.includes(effectiveRole)) {
          return false;
        }

        const requiredPermissions = module.permissions as string[] | undefined;
        if (!requiredPermissions || requiredPermissions.length === 0) {
          return true;
        }

        const userPermissions = this.currentUser?.permissions ?? [];
        if (userPermissions.length === 0) {
          return false;
        }

        return this.authService.hasAnyPermission(requiredPermissions);
      });
    } else {
      this.visibleModules = [];
    }
  }

  getHoverColor(item: any): string {
    const colorMap: { [key: string]: string } = {
      'blue': 'blue-300',
      'emerald': 'emerald-300',
      'green': 'green-300',
      'orange': 'orange-300',
      'yellow': 'yellow-300',
      'red': 'red-300',
      'fuchsia': 'fuchsia-300',
      'purple': 'purple-300',
      'indigo': 'indigo-300',
      'cyan': 'cyan-300'
    };
    return colorMap[item.color] || 'blue-300';
  }

  getNavItemClasses(item: any): any {
    return {
      'hover:text-blue-300': item.color === 'blue',
      'hover:text-emerald-300': item.color === 'emerald',
      'hover:text-green-300': item.color === 'green',
      'hover:text-orange-300': item.color === 'orange',
      'hover:text-yellow-300': item.color === 'yellow',
      'hover:text-red-300': item.color === 'red',
      'hover:text-fuchsia-300': item.color === 'fuchsia',
      'hover:text-purple-300': item.color === 'purple',
      'hover:text-indigo-300': item.color === 'indigo',
      'hover:text-cyan-300': item.color === 'cyan'
    };
  }

  getModuleRoute(item: any): string {
    const role = this.normalizeRole(this.currentUser?.role);
    if (role && item?.roleRoutes && item.roleRoutes[role]) {
      return item.roleRoutes[role];
    }
    return item.route;
  }

  getModuleQueryParams(item: any): any {
    return item?.queryParams || null;
  }

  canShowTaskArea(): boolean {
    if (!this.currentUser) {
      return false;
    }

    const role = this.normalizeRole(this.currentUser.role);
    return ['ADMIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU', 'GESTIONNAIRE', 'RCI', 'COO', 'GERANT_GENERAL'].includes(role);
  }

  private refreshTaskCount(): void {
    if (!this.currentUser || !this.canShowTaskArea()) {
      this.taskCount = 0;
      return;
    }

    this.workflowTaskService.getMyActionCount()
      .pipe(
        takeUntil(this.destroy$),
        catchError(() => {
          return this.workflowTaskService.getMyActions('A_FAIRE').pipe(
            map((actions) => ({ totalAFaire: actions.length })),
            catchError(() => of({ totalAFaire: 0 }))
          );
        })
      )
      .subscribe((count) => {
        this.taskCount = this.resolveTaskCount(count);
      });
  }

  private resolveTaskCount(count: { totalAFaire?: number; total?: number; countAFaire?: number } | number | null | undefined): number {
    if (typeof count === 'number') {
      return count;
    }
    return count?.totalAFaire ?? count?.countAFaire ?? count?.total ?? 0;
  }

  getRoleLabel(role: string, posteEmploye?: string): string {
    if (posteEmploye === 'GESTIONNAIRE') {
      return 'Gestionnaire';
    }

    const effectiveRole = this.normalizeRole(role);
    const roleLabels: { [key: string]: string } = {
      'ADMIN': 'Administrateur',
      'CHEF_BUREAU': 'Chef de Bureau',
      'GESTIONNAIRE': 'Gestionnaire',
      'AGENT_TERRAIN': 'Agent de Terrain',
      'CAISSIER': 'Caissier',
      'CONTROLEUR': 'Contrôleur',
      'RCI': 'Responsable Contrôle Interne',
      'GERANT_GENERAL': 'Gérant Général',
      'MEMBER': 'Membre'
    };
    return roleLabels[effectiveRole] || effectiveRole;
  }

  toggleMobileMenu() {
    this.mobileMenuOpen = !this.mobileMenuOpen;
    this.moreMenuOpen = false;
  }

  toggleMoreMenu() {
    this.moreMenuOpen = !this.moreMenuOpen;
    this.mobileMenuOpen = false;
  }

  closeMenus() {
    this.mobileMenuOpen = false;
    this.moreMenuOpen = false;
  }

  @HostListener('document:keydown.escape')
  closeMenusOnEscape() {
    this.closeMenus();
  }

  @HostListener('document:click')
  closeMenusOnOutsideClick() {
    this.closeMenus();
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}

