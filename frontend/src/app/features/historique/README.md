# Composant Historique des Transactions

Ce composant affiche l'historique des transactions bancaires de manière professionnelle et élégante, inspiré par le design de Belfius.

## 📋 Fonctionnalités

✅ **Sélection de compte** - Choisir parmi plusieurs comptes  
✅ **Onglets de navigation** - Opérations financières, Annexes, Extraits de compte  
✅ **Critères de recherche** - Filtrer par date et montant  
✅ **Tableau responsive** - Affichage des transactions avec tous les détails  
✅ **Statuts de transaction** - Complété, En traitement, Échoué  
✅ **Design professionnel** - Couleurs Belfius (rouge/rose #c41e3a)  
✅ **Données statiques** - Données de démonstration incluses  

## 🚀 Installation

### 1. Importer le composant

Dans votre module ou route Angular :

```typescript
import { HistoriqueTransactionsComponent } from '@features/historique/pages/historique-transactions';

// Dans le route
const routes: Routes = [
  {
    path: 'historique',
    component: HistoriqueTransactionsComponent
  }
];

// Ou dans un module
@NgModule({
  imports: [HistoriqueTransactionsComponent]
})
export class YourModule {}
```

### 2. Utiliser le composant dans un template

```html
<app-historique-transactions></app-historique-transactions>
```

## 📊 Structure des données

### Interface Compte

```typescript
interface Compte {
  id: string;                      // Identifiant unique
  numero: string;                  // Numéro de compte (IBAN)
  solde: number;                   // Solde du compte
  disponible: number;              // Montant disponible
  titulaire: string;              // Nom du titulaire
  type: 'PAIEMENT' | 'EPARGNE' | 'CREDIT';  // Type de compte
  devise: string;                 // Code devise (EUR, USD, etc.)
}
```

### Interface Transaction

```typescript
interface Transaction {
  id: string;                      // Identifiant unique
  date: Date;                     // Date de la transaction
  montant: number;                // Montant (négatif pour débit, positif pour crédit)
  type: 'DEBIT' | 'CREDIT';       // Type de mouvement
  description: string;            // Description du type de transaction
  status: 'COMPLETED' | 'PENDING' | 'FAILED';  // Statut
  compteContrepartie: string;     // Compte contrepartie / IBAN
  nomBeneficiaire: string;        // Nom du bénéficiaire/payeur
  reference?: string;             // Référence de la transaction
}
```

## 🔧 Intégration avec le backend

Pour intégrer avec votre API Java :

### 1. Créer un service

```typescript
// historique.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Compte, Transaction } from '../models';

@Injectable({ providedIn: 'root' })
export class HistoriqueService {
  private apiUrl = 'http://localhost:8080/api/historique';

  constructor(private http: HttpClient) {}

  getComptes(): Observable<Compte[]> {
    return this.http.get<Compte[]>(`${this.apiUrl}/comptes`);
  }

  getTransactions(compteId: string): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.apiUrl}/transactions/${compteId}`);
  }

  searchTransactions(filters: any): Observable<Transaction[]> {
    return this.http.post<Transaction[]>(`${this.apiUrl}/search`, filters);
  }
}
```

### 2. Modifier le composant

```typescript
// Dans historique-transactions.component.ts

constructor(private historiqueService: HistoriqueService) {}

ngOnInit(): void {
  // Au lieu de initializeData()
  this.loadComptes();
}

private loadComptes(): void {
  this.historiqueService.getComptes().subscribe({
    next: (comptes) => {
      this.comptes = comptes;
      if (this.comptes.length > 0) {
        this.selectCompte(this.comptes[0]);
      }
    },
    error: (error) => console.error('Erreur lors du chargement des comptes', error)
  });
}

selectCompte(compte: Compte): void {
  this.selectedCompte = compte;
  this.historiqueService.getTransactions(compte.id).subscribe({
    next: (transactions) => {
      this.transactions = transactions;
      this.applyFilters();
    },
    error: (error) => console.error('Erreur lors du chargement des transactions', error)
  });
}

applyFilters(): void {
  // Même logique mais peut aussi appeler le service
  const filters = this.searchCriteria;
  this.historiqueService.searchTransactions(filters).subscribe({
    next: (transactions) => {
      this.filteredTransactions = transactions;
    },
    error: (error) => console.error('Erreur lors de la recherche', error)
  });
}
```

## 🎨 Personnalisation

### Couleurs

Modifier les couleurs dans le fichier CSS :

```css
/* Couleur principale - Actuellement rouge Belfius */
--primary-color: #c41e3a;

/* Dans historique-transactions.component.css */
/* Chercher #c41e3a et remplacer par votre couleur */
```

### Données initiales

Modifier la fonction `initializeData()` dans le composant TypeScript.

### Colonnes du tableau

Modifier l'HTML et le CSS pour ajouter ou supprimer des colonnes.

## 🧪 Tests

Le composant inclut des tests unitaires. Pour les exécuter :

```bash
npm test
```

## 📱 Responsive Design

Le composant est entièrement responsive :
- ✅ Desktop (1024px+)
- ✅ Tablette (768px - 1024px)
- ✅ Mobile (<768px)

## 🚀 Exemple d'utilisation complète

```typescript
// app.routes.ts
import { Routes } from '@angular/router';
import { HistoriqueTransactionsComponent } from '@features/historique/pages/historique-transactions';

export const routes: Routes = [
  {
    path: 'historique',
    component: HistoriqueTransactionsComponent,
    data: { title: 'Historique des transactions' }
  }
];
```

```html
<!-- Dans votre layout -->
<nav>
  <a routerLink="/historique">Historique</a>
</nav>
```

## 📝 Notes

- Le composant utilise Angular standalone (importable directement)
- Pas de dépendances externes (pur CSS)
- Données statiques par défaut pour la démo
- Facilement adaptable pour intégration API
- Design professionnel et haute performance

## 🐛 Dépannage

**Les données n'apparaissent pas ?**
- Vérifier que le composant est correctement importé
- Vérifier la connexion HTTP si utilisation d'une API

**Les styles ne s'appliquent pas ?**
- Vérifier que le fichier CSS est chargé
- Vérifier que les chemins des fichiers sont corrects

**Les filtres ne fonctionnent pas ?**
- S'assurer que ngModel est disponible (FormsModule)
- Vérifier la console pour les erreurs

---

Créé pour la démonstration de Mini Credit - Application de gestion de microcrédit
