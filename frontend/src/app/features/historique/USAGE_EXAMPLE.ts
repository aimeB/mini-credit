// app.routes.ts - Exemple d'intégration du composant dans les routes

import { Routes } from '@angular/router';
import { HistoriqueTransactionsComponent } from '@features/historique';

export const routes: Routes = [
  // ... vos autres routes ...

  // Route pour afficher le composant de démonstration
  {
    path: 'demo/historique-transactions',
    component: HistoriqueTransactionsComponent,
    data: {
      title: 'Démo - Historique des Transactions',
      description: 'Exemple de composant pour afficher l\'historique des transactions'
    }
  },

  // Ou si vous préférez un chemin différent
  {
    path: 'historique',
    component: HistoriqueTransactionsComponent,
    data: { title: 'Historique des Transactions' }
  }
];

/*
 * INSTRUCTIONS D'UTILISATION :
 *
 * 1. Dans votre app.routes.ts ou routing module, importez le composant :
 *    import { HistoriqueTransactionsComponent } from '@features/historique';
 *
 * 2. Ajoutez la route ci-dessus à votre tableau de routes
 *
 * 3. Créez un lien vers la page dans votre menu ou navigation :
 *    <a routerLink="/historique">Historique des Transactions</a>
 *
 * 4. Pour intégrer avec une API :
 *    - Créez un service (voir le README dans features/historique/)
 *    - Modifiez le composant pour utiliser le service au lieu des données statiques
 *
 * DEMO FACILE :
 * - Accédez simplement à : http://localhost:4200/historique
 * - Le composant affichera les données statiques de démonstration
 */
