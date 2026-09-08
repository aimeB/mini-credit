# Priorité 13 - Matrice de recette rôles / pages / actions

Cette matrice sert à noter les observations réelles pendant la recette. Elle ne modifie pas la matrice RBAC applicative.

Statuts recommandés : `OK`, `KO`, `Non applicable`, `A confirmer`, `Anomalie #P13-XXX`.

## 1. Connexion et navigation par rôle

| Rôle | Login OK | Dashboard OK | Menus visibles conformes | Menus interdits absents | URL interdites bloquées | 403/redirection propre | Déconnexion OK | Résultat |
|---|---|---|---|---|---|---|---|---|
| Agent Terrain | A tester | A tester | A tester | A tester | A tester | A tester | A tester | A tester |
| Gestionnaire | A tester | A tester | A tester | A tester | A tester | A tester | A tester | A tester |
| Contrôleur | A tester | A tester | A tester | A tester | A tester | A tester | A tester | A tester |
| Caissier | A tester | A tester | A tester | A tester | A tester | A tester | A tester | A tester |
| Chef de Bureau | A tester | A tester | A tester | A tester | A tester | A tester | A tester | A tester |
| COO | A tester | A tester | A tester | A tester | A tester | A tester | A tester | A tester |
| RCI | A tester | A tester | A tester | A tester | A tester | A tester | A tester | A tester |
| Gérant Général | A tester | A tester | A tester | A tester | A tester | A tester | A tester | A tester |
| ADMIN | A tester | A tester | A tester | A tester | A tester | A tester | A tester | A tester |

## 2. Pages critiques par rôle

| Page / module | Agent Terrain | Gestionnaire | Contrôleur | Caissier | Chef Bureau | COO | RCI | Gérant Général | ADMIN | Observation |
|---|---|---|---|---|---|---|---|---|---|---|
| Dashboard rôle | A tester | A tester | A tester | A tester | A tester | A tester | A tester | A tester | A tester |  |
| Mes actions | A tester | A tester | A tester | A tester | A tester | A tester | A tester | A tester | A tester |  |
| Membres | A tester | A tester | A tester | A tester | A tester | A tester | A tester | A tester | A tester |  |
| Collecte terrain | A tester | A tester | A tester | A tester | A tester | A tester | A tester | A tester | A tester |  |
| Billetage collecte | Interdit attendu | Interdit attendu | A tester | A tester | A confirmer | Interdit attendu | Interdit attendu | Interdit attendu | A tester |  |
| Contrôle collecte | Interdit attendu | Interdit attendu | A tester | Interdit attendu | A confirmer | Supervision | Supervision | Supervision | A tester |  |
| Demandes crédit | A confirmer | A tester | A tester | Partiel | A tester | Supervision | Supervision | Supervision | A tester |  |
| Pré-analyse crédit | Interdit attendu | A tester | Interdit attendu | Interdit attendu | Interdit attendu | Interdit attendu | Interdit attendu | Interdit attendu | A tester |  |
| Analyse / garantie crédit | Interdit attendu | Interdit attendu | A tester | Interdit attendu | Interdit attendu | Interdit attendu | Lecture/supervision | Interdit attendu | A tester |  |
| Approbation crédit | Interdit attendu | Interdit attendu | Interdit attendu | Interdit attendu | A tester | Interdit attendu | Interdit attendu | Supervision | A tester |  |
| Frais crédit à encaisser | Interdit attendu | Interdit attendu | Interdit attendu | A tester | Supervision | Interdit attendu | Interdit attendu | Interdit attendu | A tester |  |
| Décaissement crédit | Interdit attendu | Interdit attendu | Interdit attendu | A tester | Supervision | Interdit attendu | Interdit attendu | Interdit attendu | A tester |  |
| Remboursement crédit espèces | Interdit attendu | A confirmer | Interdit attendu | A tester | A confirmer | Interdit attendu | Interdit attendu | Interdit attendu | A tester |  |
| Retrait épargne demande | A confirmer | A confirmer | A confirmer | A confirmer | A confirmer | Interdit attendu | Interdit attendu | Interdit attendu | A tester |  |
| Retrait épargne validation | Interdit attendu | Interdit attendu | A tester | Interdit attendu | Interdit attendu | Interdit attendu | Supervision | Interdit attendu | A tester |  |
| Retrait épargne paiement | Interdit attendu | Interdit attendu | Interdit attendu | A tester | Supervision | Interdit attendu | Interdit attendu | Interdit attendu | A tester |  |
| Sessions caisse | Interdit attendu | Interdit attendu | Lecture/contrôle | A tester | A tester | Supervision | Supervision | Supervision | A tester |  |
| Opération caisse libre | Interdit attendu | Interdit attendu | Interdit attendu | Interdit attendu | A confirmer | Interdit attendu | Interdit attendu | Interdit attendu | A confirmer | Point RCI sensible |
| Dépenses | Interdit attendu | A confirmer | A confirmer | Paiement si validée | Validation attendue | Supervision | Audit | Supervision | A tester |  |
| Salaires / transport | Interdit attendu | A confirmer | A confirmer | Paiement si autorisé | Supervision | Supervision | Audit | Supervision | A tester |  |
| Rapports revenus | Interdit attendu | A confirmer | A tester | A confirmer | A tester | A tester | A tester | A tester | A tester |  |
| Audit | Interdit attendu | Interdit attendu | A tester | Interdit attendu | A tester | A tester | A tester | A tester | A tester |  |
| Organisation | Interdit attendu | A confirmer | Interdit attendu | Interdit attendu | A confirmer | Interdit attendu | Interdit attendu | Supervision | A tester |  |
| Utilisateurs | Interdit attendu | Interdit attendu | Interdit attendu | Interdit attendu | A confirmer | Interdit attendu | Interdit attendu | Interdit attendu | A tester |  |

## 3. URL interdites à tester directement

Renseigner les URLs exactes après observation des routes réelles.

| Rôle | URL directe interdite | Résultat attendu | Résultat obtenu | Statut |
|---|---|---|---|---|
| Agent Terrain | /caisses/session/:id/operations/nouveau | 403 ou redirection propre | A tester | A tester |
| Agent Terrain | /credits/:id/approbation | 403 ou redirection propre | A tester | A tester |
| Gestionnaire | /credits/:id/decaissement | 403 ou redirection propre | A tester | A tester |
| Gestionnaire | /caisses/session/:id/operations/nouveau | 403 ou redirection propre | A tester | A tester |
| Contrôleur | /credits/:id/decaissement | 403 ou redirection propre | A tester | A tester |
| Caissier | /credits/demandes/:id/analyse | 403 ou redirection propre | A tester | A tester |
| Caissier | /caisses/session/:id/operations/nouveau | 403 ou redirection propre | A tester | A tester |
| RCI | /caisses/session/:id/operations/nouveau | 403 ou absence bouton/action | A tester | A tester |
| COO | action métier opérationnelle directe | 403 ou lecture seule | A tester | A tester |
| Gérant Général | action métier opérationnelle directe | 403 ou lecture seule | A tester | A tester |

## 4. Mes actions et supervision

| Rôle connecté | Tâche attendue | Entité | Peut ouvrir | Peut traiter | Supervision visible | Compteur personnel correct | Statut |
|---|---|---|---|---|---|---|---|
| Gestionnaire | Pré-analyse crédit | Demande crédit A | A tester | A tester | Non applicable | A tester | A tester |
| Contrôleur | Analyse crédit | Demande crédit A | A tester | A tester | Non applicable | A tester | A tester |
| Contrôleur | Validation retrait | Retrait Membre B | A tester | A tester | Non applicable | A tester | A tester |
| Caissier | Encaissement frais | Demande crédit A | A tester | A tester | Non applicable | A tester | A tester |
| Caissier | Décaissement crédit | Crédit A | A tester | A tester | Non applicable | A tester | A tester |
| Caissier | Paiement retrait | Retrait Membre B | A tester | A tester | Non applicable | A tester | A tester |
| Chef de Bureau | Approbation crédit | Demande crédit A | A tester | A tester | A tester | A tester | A tester |
| COO | Supervision | tâches autres rôles | A tester | Non attendu | A tester | Ne doit pas compter comme personnelle | A tester |
| RCI | Supervision/audit | tâches autres rôles | A tester | Non attendu | A tester | Ne doit pas compter comme personnelle | A tester |
| Gérant Général | Supervision | tâches autres rôles | A tester | Non attendu | A tester | Ne doit pas compter comme personnelle | A tester |
| ADMIN | Supervision technique | toutes tâches | A tester | selon règle existante | A tester | A vérifier | A tester |
