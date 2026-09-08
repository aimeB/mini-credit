import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { RoleGuard } from './core/guards/role.guard';
import { PermissionGuard } from './core/guards/permission.guard';
import { LoginComponent } from './auth/login/login.component';
import { ActivationComponent } from './auth/activation/activation.component';
import { ChangePasswordComponent } from './auth/change-password/change-password.component';
import { MembreListComponent } from './features/membres/pages/membre-list/membre-list.component';
import { MembreFormComponent } from './features/membres/pages/membre-form/membre-form.component';
import { MemberProfileComponent } from './features/membres/pages/member-profile/member-profile.component';
import { EpargneDashboardComponent } from './features/epargne/pages/epargne-dashboard/epargne-dashboard.component';
import { CaisseListComponent } from './features/caisse/pages/caisse-list/caisse-list.component';
import { CaisseFormComponent } from './features/caisse/pages/caisse-form/caisse-form.component';
import { SessionCaisseFormComponent } from './features/caisse/pages/session-caisse-form/session-caisse-form.component';
import { SessionCaisseDetailComponent } from './features/caisse/pages/session-caisse-detail/session-caisse-detail.component';
import { OperationCaisseListComponent } from './features/caisse/pages/operation-caisse-list/operation-caisse-list.component';
import { DepenseCaisseListComponent } from './features/caisse/pages/depense-caisse-list/depense-caisse-list.component';
import { CreditDashboardComponent } from './features/credit/pages/credit-dashboard/credit-dashboard.component';
import { DemandeCreditListComponent } from './features/credit/pages/demande-credit-list/demande-credit-list.component';
import { DemandeCreditFormComponent } from './features/credit/pages/demande-credit-form/demande-credit-form.component';
import { AnalyseRisqueFormComponent } from './features/credit/pages/analyse-risque-form/analyse-risque-form.component';
import { GarantieCreditPageComponent } from './features/credit/pages/garantie-credit-page/garantie-credit-page.component';
import { CreditDetailComponent } from './features/credit/pages/credit-detail/credit-detail.component';
import { CreditEnCoursComponent } from './features/credit/pages/credit-en-cours/credit-en-cours.component';
import { CreditRemboursesComponent } from './features/credit/pages/credit-rembourses/credit-rembourses.component';
import { CreditListComponent } from './features/credit/pages/credit-list/credit-list.component';
import { FraisCreditAEncaisserComponent } from './features/credit/pages/frais-credit-a-encaisser/frais-credit-a-encaisser.component';
import { RemboursementCreditFormComponent } from './features/credit/pages/remboursement-credit-form/remboursement-credit-form.component';
import { ContratCreditFormComponent } from './features/credit/pages/contrat-credit-form/contrat-credit-form.component';
import { MembreDetailComponent } from './features/membres/pages/membre-detail/membre-detail.component';
import { PaiementInitialDemandeCreditFormComponent } from './features/credit/pages/paiement-initial-demande-credit-form-component/paiement-initial-demande-credit-form-component.component';
import { AccessDeniedComponent } from './shared/components/access-denied/access-denied.component';
import { UtilisateurListComponent } from './features/utilisateurs/pages/utilisateur-list/utilisateur-list.component';
import { UtilisateurFormComponent } from './features/utilisateurs/pages/utilisateur-form/utilisateur-form.component';
import { EmployeListComponent } from './features/employes/pages/employe-list/list.component';
import { EmployeFormComponent } from './features/employes/pages/employe-form/form.component';
import { EmployeDetailComponent } from './features/employes/pages/employe-detail/detail.component';
import { GarantieListComponent } from './features/garanties/pages/garantie-list/garantie-list.component';
import { GarantieFormComponent } from './features/garanties/pages/garantie-form/garantie-form.component';
import { RapportsDashboardComponent } from './features/rapports/pages/rapports-dashboard/rapports-dashboard.component';
import { AuditListComponent } from './features/audit/pages/audit-list/audit-list.component';
import { AuditLogDetailComponent } from './features/audit/pages/audit-log-detail/audit-log-detail.component';
import { HistoriqueTransactionsComponent } from './features/historique/pages/historique-transactions/historique-transactions.component';
import { RecetteListComponent } from './features/recettes/pages/recette-list/recette-list.component';
import { RecetteFormComponent } from './features/recettes/pages/recette-form/recette-form.component';
import { RecetteDetailComponent } from './features/recettes/pages/recette-detail/recette-detail.component';
import { RecetteValidateComponent } from './features/recettes/pages/recette-validate/recette-validate.component';
import { RecetteDashboardComponent } from './features/recettes/pages/recette-dashboard/recette-dashboard.component';
import { CollecteDuJourComponent } from './features/recettes/pages/collecte-du-jour/collecte-du-jour.component';
import { MesCollectesComponent } from './features/recettes/pages/mes-collectes/mes-collectes.component';
import { CollectesSoumisesBilletageComponent } from './features/recettes/pages/collectes-soumises-billetage/collectes-soumises-billetage.component';
import { CollectesAControlerComponent } from './features/recettes/pages/collectes-a-controler/collectes-a-controler.component';
import { SuiviCollectesTerrainComponent } from './features/recettes/pages/suivi-collectes-terrain/suivi-collectes-terrain.component';
import { AgentListComponent, AgentFormComponent } from './admin/agent-terrain';
import { AgenceListComponent } from './features/organisation/pages/agence-list/agence-list.component';
import { AgenceFormComponent } from './features/organisation/pages/agence-form/agence-form.component';
import { AgenceDetailComponent } from './features/organisation/pages/agence-detail/agence-detail.component';
import { SiteListComponent } from './features/organisation/pages/site-list/site-list.component';
import { SiteFormComponent } from './features/organisation/pages/site-form/site-form.component';
import { SiteDetailComponent } from './features/organisation/pages/site-detail/site-detail.component';
import { OrganisationHomeComponent } from './features/organisation/pages/organisation-home/organisation-home.component';
import { PermissionCode } from './shared/enums/permission-code.enum';

export const routes: Routes = [
  // Route de login (non protégée)
  { path: 'auth/login', component: LoginComponent },
  { path: 'auth/activate', component: ActivationComponent },
  { path: 'auth/change-password', component: ChangePasswordComponent },
  { path: 'login', redirectTo: 'auth/login', pathMatch: 'full' },
  { path: 'activate', redirectTo: 'auth/activate', pathMatch: 'full' },

  // Routes protégées par AuthGuard
  {
    path: '',
    canActivate: [AuthGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./shared/components/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      {
        path: 'dashboard/controle-interne',
        loadComponent: () =>
          import('./features/rapports/pages/dashboard-controle-interne/dashboard-controle-interne.component')
            .then(m => m.DashboardControleInterneComponent),
        canActivate: [RoleGuard],
        data: {
          roles: ['ADMIN', 'GERANT_GENERAL', 'CHEF_BUREAU', 'CONTROLEUR', 'RCI'],
          permissions: [PermissionCode.DASHBOARD_CONTROLE_INTERNE_READ]
        }
      },
      {
        path: 'mes-actions',
        loadComponent: () =>
          import('./features/workflow/pages/mes-actions/mes-actions.component')
            .then(m => m.MesActionsComponent),
        canActivate: [RoleGuard, PermissionGuard],
        data: {
          roles: ['ADMIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU', 'GESTIONNAIRE', 'RCI', 'COO', 'GERANT_GENERAL'],
          permissions: [
            PermissionCode.TASK_READ_OWN,
            PermissionCode.TASK_READ_ANTENNE,
            PermissionCode.TASK_SUPERVISE,
            PermissionCode.TASK_AUDIT
          ],
          permissionBypassRoles: ['ADMIN']
        }
      },

      // Membres
      {
        path: 'membres',
        component: MembreListComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN'] }
      },
      {
        path: 'membres/nouveau',
        component: MembreFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'AGENT_TERRAIN'] }
      },
      {
        path: 'membres/:id',
        component: MembreDetailComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN'] }
      },
      {
        path: 'membres/:id/edit',
        component: MembreFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE'] }
      },
      {
        path: 'member-profile',
        component: MemberProfileComponent,
        canActivate: [RoleGuard],
        data: { roles: ['MEMBER'] }
      },

      // Epargne
      {
        path: 'epargne',
        component: EpargneDashboardComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN', 'CONTROLEUR'] }
      },
      {
        path: 'epargne/membre/:membreId',
        component: EpargneDashboardComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN', 'CONTROLEUR'] }
      },

      // Demandes de Retrait Épargne (PHASE 6B.3)
      {
        path: 'epargne/demandes-retrait',
        loadComponent: () =>
          import('./features/epargne/pages/demande-retrait-epargne-list/demande-retrait-epargne-list.component')
            .then(m => m.DemandeRetraitEpargneListComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'CONTROLEUR', 'CAISSIER'] }
      },
      {
        path: 'epargne/demandes-retrait/new',
        loadComponent: () =>
          import('./features/epargne/pages/demande-retrait-epargne-form/demande-retrait-epargne-form.component')
            .then(m => m.DemandeRetraitEpargneFormComponent),
        canActivate: [RoleGuard],
        data: { roles: ['MEMBER', 'CAISSIER', 'ADMIN'] }
      },
      {
        path: 'epargne/demandes-retrait/nouveau',
        loadComponent: () =>
          import('./features/epargne/pages/demande-retrait-epargne-form/demande-retrait-epargne-form.component')
            .then(m => m.DemandeRetraitEpargneFormComponent),
        canActivate: [RoleGuard],
        data: { roles: ['MEMBER', 'CAISSIER', 'ADMIN'] }
      },
      {
        path: 'epargne/demandes-retrait/:id',
        loadComponent: () =>
          import('./features/epargne/pages/demande-retrait-epargne-detail/demande-retrait-epargne-detail.component')
            .then(m => m.DemandeRetraitEpargneDetailComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'CONTROLEUR', 'CAISSIER', 'MEMBER'] }
      },

      // Recettes Terrain
      {
        path: 'recettes',
        component: RecetteListComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'GESTIONNAIRE', 'CONTROLEUR', 'CHEF_BUREAU', 'COO', 'RCI', 'GERANT_GENERAL'] }
      },
      {
        path: 'recettes/nouveau',
        component: RecetteFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'recettes/:id',
        component: RecetteDetailComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'GESTIONNAIRE', 'CONTROLEUR', 'CHEF_BUREAU', 'COO', 'RCI', 'GERANT_GENERAL'] }
      },
      {
        path: 'recettes/:id/edit',
        component: RecetteFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'recettes/:id/valider',
        component: RecetteValidateComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CONTROLEUR'] }
      },
      {
        path: 'recettes/dashboard/kpi',
        component: RecetteDashboardComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'GESTIONNAIRE', 'CONTROLEUR', 'CHEF_BUREAU', 'COO', 'RCI', 'GERANT_GENERAL'] }
      },
      {
        path: 'collectes/ma-collecte',
        component: CollecteDuJourComponent,
        canActivate: [RoleGuard],
        data: { roles: ['AGENT_TERRAIN'] }
      },
      {
        path: 'collectes/:id/edition',
        component: CollecteDuJourComponent,
        canActivate: [RoleGuard],
        data: { roles: ['AGENT_TERRAIN'] }
      },
      {
        path: 'collectes/mes-collectes',
        component: MesCollectesComponent,
        canActivate: [RoleGuard],
        data: { roles: ['AGENT_TERRAIN'] }
      },
      {
        path: 'collectes/soumises-billetage',
        component: CollectesSoumisesBilletageComponent,
        canActivate: [RoleGuard],
        data: { roles: ['CAISSIER'] }
      },
      {
        path: 'collectes/billetage',
        component: CollectesSoumisesBilletageComponent,
        canActivate: [RoleGuard],
        data: { roles: ['CAISSIER'] }
      },
      {
        path: 'collectes/a-controler',
        component: CollectesAControlerComponent,
        canActivate: [RoleGuard],
        data: { roles: ['CONTROLEUR', 'ADMIN'] }
      },
      {
        path: 'collectes/suivi-terrain',
        component: SuiviCollectesTerrainComponent,
        canActivate: [RoleGuard],
        data: { roles: ['GESTIONNAIRE', 'CHEF_BUREAU', 'ADMIN'] }
      },
      { path: 'recettes-terrain', redirectTo: 'collectes/suivi-terrain', pathMatch: 'full' },

      // Caisse
      {
        path: 'caisses',
        component: CaisseListComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'CAISSIER', 'CONTROLEUR'] }
      },
      {
        path: 'caisses/nouveau',
        component: CaisseFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'CAISSIER'] }
      },
      {
        path: 'caisses/session/ouverture',
        component: SessionCaisseFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'CAISSIER'] }
      },
      {
        path: 'caisses/session/:id',
        component: SessionCaisseDetailComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU'] }
      },
      {
        path: 'caisses/depenses',
        component: DepenseCaisseListComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CAISSIER', 'CHEF_BUREAU', 'CONTROLEUR', 'RCI', 'CHEF_BUREAU'] }
      },
      {
        path: 'caisses/depenses/nouvelle',
        loadComponent: () =>
          import('./features/caisse/pages/depense-caisse-form/depense-caisse-form.component')
            .then(m => m.DepenseCaisseFormComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CAISSIER', 'CHEF_BUREAU'] }
      },
      {
        path: 'caisses/depenses/:id',
        loadComponent: () =>
          import('./features/caisse/pages/depense-caisse-validation/depense-caisse-validation.component')
            .then(m => m.DepenseCaisseValidationComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CAISSIER', 'CHEF_BUREAU', 'CONTROLEUR', 'RCI', 'CHEF_BUREAU'] }
      },
      {
        path: 'caisses/session/:id/cloture',
        loadComponent: () =>
          import('./features/caisse/pages/session-caisse-cloture/session-caisse-cloture.component')
            .then(m => m.SessionCaisseClotureComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'CAISSIER'] }
      },
        {
          path: 'caisses/sessions/:id',
          component: SessionCaisseDetailComponent,
          canActivate: [RoleGuard],
          data: { roles: ['ADMIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU'] }
        },
        {
          path: 'caisses/sessions/:id/cloture',
          loadComponent: () =>
            import('./features/caisse/pages/session-caisse-cloture/session-caisse-cloture.component')
              .then(m => m.SessionCaisseClotureComponent),
          canActivate: [RoleGuard],
          data: { roles: ['ADMIN', 'CHEF_BUREAU', 'CAISSIER'] }
        },
      {
        path: 'caisses/session/:sessionId/operations',
        component: OperationCaisseListComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU'] }
      },
      {
        // PATCH 7 — Saisie manuelle d'une opération caisse
        path: 'caisses/session/:sessionId/operations/nouveau',
        loadComponent: () =>
          import('./features/caisse/pages/operation-caisse-form/operation-caisse-form.component')
            .then(m => m.OperationCaisseFormComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU'] }
      },

      {
        path: 'caisses/sessions',
        loadComponent: () =>
          import('./features/caisse/pages/session-caisse-list/session-caisse-list.component')
            .then(m => m.SessionCaisseListComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU'] }
      },

      // ✅ Journal de caisse (lazy load)
      {
        path: 'caisses/journal',
        loadComponent: () =>
          import('./features/caisse/pages/journal-caisse/journal-caisse.component')
            .then(m => m.JournalCaisseComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU'] }
      },
      {
        path: 'caisses/controle',
        loadComponent: () =>
          import('./features/caisse/pages/caisse-controle/caisse-controle.component')
            .then(m => m.CaisseControleComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CONTROLEUR', 'RCI'] }
      },
      {
        path: 'caisses/anomalies',
        loadComponent: () =>
          import('./features/caisse/pages/session-caisse-anomalies/session-caisse-anomalies.component')
            .then(m => m.SessionCaisseAnomaliesComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'CONTROLEUR', 'RCI', 'CAISSIER'] }
      },
      {
        path: 'caisses/rapports/session',
        loadComponent: () =>
          import('./features/caisse/pages/rapport-caisse-session/rapport-caisse-session.component')
            .then(m => m.RapportCaisseSessionComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU', 'RCI', 'CHEF_BUREAU'] }
      },
      {
        path: 'caisses/rapports/journalier',
        loadComponent: () =>
          import('./features/caisse/pages/rapport-caisse-journalier/rapport-caisse-journalier.component')
            .then(m => m.RapportCaisseJournalierComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU', 'RCI', 'CHEF_BUREAU'] }
      },
      {
        path: 'caisses/rapports/periode',
        loadComponent: () =>
          import('./features/caisse/pages/rapport-caisse-periode/rapport-caisse-periode.component')
            .then(m => m.RapportCaissePeriodeComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU', 'RCI', 'CHEF_BUREAU'] }
      },
      {
        path: 'caisses/rapports/depenses',
        loadComponent: () =>
          import('./features/caisse/pages/rapport-caisse-depenses/rapport-caisse-depenses.component')
            .then(m => m.RapportCaisseDepensesComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU', 'RCI', 'CHEF_BUREAU'] }
      },
      {
        path: 'caisses/rapports/ecarts',
        loadComponent: () =>
          import('./features/caisse/pages/rapport-caisse-ecarts/rapport-caisse-ecarts.component')
            .then(m => m.RapportCaisseEcartsComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU', 'RCI', 'CHEF_BUREAU'] }
      },

      // PATCH 11 — Écarts de caisse (routes mises à jour avec RCI)
      {
        // PATCH 11: RCI accède à la liste globale dans le cadre de son audit
        path: 'caisses/ecarts',
        loadComponent: () =>
          import('./features/caisse/pages/ecart-caisse-list/ecart-caisse-list.component')
            .then(m => m.EcartCaisseListComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CONTROLEUR', 'RCI'] }
      },
      {
        // PATCH 11: RCI peut consulter le détail d'un écart (audit)
        path: 'caisses/ecarts/:id',
        loadComponent: () =>
          import('./features/caisse/pages/ecart-caisse-detail/ecart-caisse-detail.component')
            .then(m => m.EcartCaisseDetailComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CONTROLEUR', 'CAISSIER', 'RCI'] }
      },
      {
        // PATCH 11: RCI peut consulter les écarts d'une session
        path: 'caisses/session/:sessionId/ecarts',
        loadComponent: () =>
          import('./features/caisse/pages/ecart-caisse-list/ecart-caisse-list.component')
            .then(m => m.EcartCaisseListComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CONTROLEUR', 'CAISSIER', 'RCI'] }
      },

      // Crédit
      {
        path: 'credits',
        component: CreditDashboardComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN', 'CONTROLEUR', 'CAISSIER'] }
      },
      {
        path: 'credits/demandes',
        component: DemandeCreditListComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'CONTROLEUR'] }
      },
      {
        path: 'credits/frais-a-encaisser',
        component: FraisCreditAEncaisserComponent,
        canActivate: [RoleGuard],
        data: { roles: ['CAISSIER', 'ADMIN', 'CHEF_BUREAU'] }
      },
      {
        path: 'credits/demandes/nouveau',
        component: DemandeCreditFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE'] }
      },
      {
        path: 'credits/demandes/nouveau/membre/:membreId',
        component: DemandeCreditFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE'] }
      },
      {
        path: 'credits/demandes/:id/paiement-initial',
        component: PaiementInitialDemandeCreditFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['CAISSIER'] }
      },
      {
        path: 'credits/demandes/:id',
        component: AnalyseRisqueFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'GESTIONNAIRE', 'CONTROLEUR'] }
      },
      {
        path: 'credits/demandes/:id/analyse',
        component: AnalyseRisqueFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CONTROLEUR'] }
      },
      { path: 'credits/pre-analyses', redirectTo: 'credits/demandes', pathMatch: 'full' },
      {
        path: 'credits/demandes/:id/analyse-risque',
        component: AnalyseRisqueFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CONTROLEUR'] }
      },
      {
        path: 'credits/demandes/:id/garantie',
        component: GarantieCreditPageComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CONTROLEUR', 'CHEF_BUREAU'] }
      },
      {
        path: 'credits/liste',
        component: CreditListComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'CAISSIER', 'CONTROLEUR'] }
      },
      {
        path: 'credits/en-cours',
        component: CreditEnCoursComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'CAISSIER', 'CONTROLEUR'] }
      },
      {
        path: 'credits/rembourses',
        component: CreditRemboursesComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'CONTROLEUR', 'COO', 'RCI', 'GERANT_GENERAL'] }
      },
      {
        path: 'credits/:creditId/decaissement',
        component: CreditListComponent,
        canActivate: [RoleGuard],
        data: { roles: ['CAISSIER'] }
      },
      {
        path: 'credits/:creditId/remboursement',
        component: RemboursementCreditFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'CAISSIER'] }
      },
      {
        path: 'credits/:creditId/contrat',
        component: ContratCreditFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE'] }
      },
      {
        path: 'credits/:creditId',
        component: CreditDetailComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'CAISSIER', 'CONTROLEUR', 'AGENT_TERRAIN'] }
      },

      // Organisation — Agences & Sites (ADMIN only — prérequis pour tout le reste)
      { path: 'performances-terrain', redirectTo: 'rapports', pathMatch: 'full' },
      { path: 'reclamations', redirectTo: 'mes-actions', pathMatch: 'full' },
      {
        path: 'organisation',
        component: OrganisationHomeComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU'] }
      },
      {
        path: 'agences',
        component: AgenceListComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'agences/nouveau',
        component: AgenceFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'agences/:id',
        component: AgenceDetailComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU'] }
      },
      {
        path: 'agences/:id/modifier',
        component: AgenceFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'sites',
        component: SiteListComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'GESTIONNAIRE'] }
      },
      {
        path: 'sites/nouveau',
        component: SiteFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'sites/:id',
        component: SiteDetailComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE'] }
      },
      {
        path: 'sites/:id/modifier',
        component: SiteFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] }
      },

      // Utilisateurs (Admin only)
      {
        path: 'utilisateurs',
        component: UtilisateurListComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU'] }
      },
      {
        path: 'utilisateurs/nouveau',
        component: UtilisateurFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'utilisateurs/:id/edit',
        component: UtilisateurFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] }
      },

      // 👤 Admin: Gestion des Agents Terrain (Phase 5.3)
      {
        path: 'admin/agents',
        component: AgentListComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'GESTIONNAIRE'] }
      },
      {
        path: 'admin/agents/nouveau',
        component: AgentFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/agents/:id/edit',
        component: AgentFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/transport-sites',
        loadComponent: () =>
          import('./features/parametres/pages/transport-site-parametres/transport-site-parametres.component')
            .then(m => m.TransportSiteParametresComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'COO', 'GERANT_GENERAL'] }
      },

      // Employés (Admin only)
      {
        path: 'employes',
        component: EmployeListComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'employes/nouveau',
        component: EmployeFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'employes/:id',
        component: EmployeDetailComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'employes/:id/edit',
        component: EmployeFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] }
      },

      // Garanties
      {
        path: 'garanties',
        component: GarantieListComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'CONTROLEUR', 'RCI'] }
      },
      {
        path: 'garanties/nouveau',
        component: GarantieFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'CONTROLEUR', 'RCI'] }
      },
      {
        path: 'garanties/:id/edit',
        component: GarantieFormComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'CONTROLEUR', 'RCI'] }
      },

      // Rapports & Analytics
      {
        path: 'rapports',
        component: RapportsDashboardComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'GERANT_GENERAL', 'CHEF_BUREAU', 'GESTIONNAIRE', 'COO', 'RCI'] }
      },
      {
        path: 'rapports/revenus',
        loadComponent: () =>
          import('./features/rapports/pages/rapport-revenus/rapport-revenus.component')
            .then(m => m.RapportRevenusComponent),
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'GERANT_GENERAL', 'COO', 'RCI', 'CHEF_BUREAU', 'CONTROLEUR'] }
      },

      // Audit & Logs
      {
        path: 'audit/logs',
        component: AuditListComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'GERANT_GENERAL', 'RCI', 'CHEF_BUREAU', 'CONTROLEUR'] }
      },
      {
        path: 'audit/logs/:id',
        component: AuditLogDetailComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'GERANT_GENERAL', 'RCI', 'CHEF_BUREAU', 'CONTROLEUR'] }
      },
      {
        path: 'audit',
        redirectTo: 'audit/logs',
        pathMatch: 'full'
      },

      // Historique des Transactions
      {
        path: 'historique',
        component: HistoriqueTransactionsComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'CHEF_BUREAU', 'CAISSIER'] }
      },

      // Demo Belfius History (Static)
      {
        path: 'demo/belfius-history',
        loadComponent: () =>
          import('./features/demo/pages/belfius-history-demo/belfius-history-demo.component')
            .then(m => m.BelfiusHistoryDemoComponent)
      },

      // Page d'accès refusé
      { path: 'access-denied', component: AccessDeniedComponent }
    ]
  },

  // Redirection par défaut vers login
  { path: '**', redirectTo: 'auth/login' }
];
