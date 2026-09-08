# Priorité 13 - Journal d'exécution recette manuelle

Statuts : `A tester`, `En cours`, `OK`, `KO`, `Bloqué`, `Non applicable`.

## 1. Préparation

| ID | Contrôle | Responsable recette | Statut | Preuve / note |
|---|---|---|---|---|
| P13-PREP-001 | Environnement frontend accessible | Copilot | OK | `http://localhost:4200` répond 200 le 2026-07-25 |
| P13-PREP-002 | Backend accessible | Copilot | OK | `http://localhost:8080/api` répond 403 protégé ; service joignable le 2026-07-25 |
| P13-PREP-003 | ADMIN connecté |  | A tester |  |
| P13-PREP-004 | Agence créée/vérifiée |  | A tester |  |
| P13-PREP-005 | Site créé/vérifié |  | A tester |  |
| P13-PREP-006 | Comptes par rôle actifs |  | A tester |  |
| P13-PREP-007 | Rattachements agence/site visibles |  | A tester |  |
| P13-PREP-008 | Membres A/B/C disponibles |  | A tester |  |
| P13-PREP-009 | Caisse active |  | A tester |  |
| P13-PREP-010 | Session caisse du jour ouverte |  | A tester |  |
| P13-PREP-011 | Solde caisse suffisant |  | A tester |  |
| P13-PREP-012 | Paramètres frais crédit/retrait/carnet vérifiés |  | A tester |  |
| P13-PREP-013 | Paramètre transport site vérifié |  | A tester |  |

## 2. Jour 1 - Connexion / menus / rôles / socle initial

| ID | Scénario | Rôle | Page | Statut | Anomalie | Preuve / note |
|---|---|---|---|---|---|---|
| P13-S01-AT | Connexion, dashboard, menus, interdits | Agent Terrain | multi-pages | A tester |  |  |
| P13-S01-GES | Connexion, dashboard, menus, interdits | Gestionnaire | multi-pages | A tester |  |  |
| P13-S01-CTRL | Connexion, dashboard, menus, interdits | Contrôleur | multi-pages | A tester |  |  |
| P13-S01-CAIS | Connexion, dashboard, menus, interdits | Caissier | multi-pages | A tester |  |  |
| P13-S01-CB | Connexion, dashboard, menus, interdits | Chef de Bureau | multi-pages | A tester |  |  |
| P13-S01-COO | Connexion, dashboard, menus, interdits | COO | multi-pages | A tester |  |  |
| P13-S01-RCI | Connexion, dashboard, menus, interdits | RCI | multi-pages | A tester |  |  |
| P13-S01-GG | Connexion, dashboard, menus, interdits | Gérant Général | multi-pages | A tester |  |  |
| P13-S01-ADMIN | Connexion, dashboard, menus, interdits | ADMIN | multi-pages | A tester |  |  |
| P13-S01-RCI-OPLIBRE | Vérifier absence bouton Ajouter opération session caisse | RCI | /caisses/session/:id | A tester |  | Point sensible |

## 3. Jour 2 - Collecte terrain / épargne / retrait

| ID | Etape | Rôle | Donnée | Statut | Anomalie | Preuve / note |
|---|---|---|---|---|---|---|
| P13-S02-001 | Créer collecte du jour | Agent Terrain | Membre A | A tester |  |  |
| P13-S02-002 | Ajouter ligne épargne | Agent Terrain | Membre A | A tester |  |  |
| P13-S02-003 | Ajouter ligne remboursement crédit si disponible | Agent Terrain | Membre A | A tester |  |  |
| P13-S02-004 | Ajouter carnet vendu si applicable | Agent Terrain | Membre A | A tester |  |  |
| P13-S02-005 | Soumettre collecte | Agent Terrain | Collecte jour | A tester |  |  |
| P13-S02-006 | Effectuer billetage | Caissier | Collecte soumise | A tester |  |  |
| P13-S02-007 | Contrôler collecte | Contrôleur | Collecte billetée | A tester |  |  |
| P13-S02-008 | Valider collecte | Contrôleur | Collecte contrôlée | A tester |  |  |
| P13-S02-009 | Vérifier génération opérations | Contrôleur/ADMIN | Audit + mouvements | A tester |  |  |
| P13-S02-010 | Vérifier idempotence double clic/refresh | Contrôleur/ADMIN | Collecte validée | A tester |  |  |
| P13-S05-001 | Créer demande retrait | Rôle autorisé | Membre B | A tester |  |  |
| P13-S05-002 | Valider retrait | Contrôleur | Membre B | A tester |  |  |
| P13-S05-003 | Payer retrait | Caissier | Membre B | A tester |  |  |
| P13-S05-004 | Vérifier frais/revenus/flux exclus/audit | ADMIN/RCI | Rapports + audit | A tester |  |  |

## 4. Jour 3 - Crédit complet

| ID | Etape | Rôle | Donnée | Statut | Anomalie | Preuve / note |
|---|---|---|---|---|---|---|
| P13-S03-A01 | Créer demande 100 000 FC, frais 7 500 FC | Rôle autorisé | Membre A | A tester |  |  |
| P13-S03-A02 | Vérifier frais/durée non forcés | Rôle autorisé | Demande A | A tester |  |  |
| P13-S03-B01 | Encaisser frais crédit | Caissier | Demande A | A tester |  |  |
| P13-S03-B02 | Vérifier opération caisse entrée + revenu | Caissier/ADMIN | Demande A | A tester |  |  |
| P13-S03-C01 | Pré-analyse | Gestionnaire | Demande A | A tester |  |  |
| P13-S03-D01 | Analyse risque | Contrôleur | Demande A | A tester |  |  |
| P13-S03-E01 | Garantie épargne 20% | Contrôleur | Demande A | A tester |  |  |
| P13-S03-E02 | Gage matériel OK >= 2x | Contrôleur | Demande A | A tester |  |  |
| P13-S03-E03 | Gage insuffisant refusé | Contrôleur | Membre C | A tester |  |  |
| P13-S03-F01 | Approbation crédit | Chef de Bureau | Demande A | A tester |  |  |
| P13-S03-G01 | Décaissement crédit | Caissier | Crédit A | A tester |  |  |
| P13-S03-G02 | Vérifier audit/quittance/tâche clôturée | Caissier/ADMIN | Crédit A | A tester |  |  |

## 5. Jour 4 - Remboursements / dépenses / salaires / transport

| ID | Etape | Rôle | Donnée | Statut | Anomalie | Preuve / note |
|---|---|---|---|---|---|---|
| P13-S04-A01 | Remboursement espèces | Caissier | Crédit A | A tester |  |  |
| P13-S04-B01 | Remboursement compte épargne | Rôle autorisé | Crédit A | A tester |  |  |
| P13-S04-C01 | Remboursement via collecte | Agent Terrain/Contrôleur | Crédit A | A tester |  |  |
| P13-S04-R01 | Refus paiement > reste dû | Rôle autorisé | Crédit A | A tester |  |  |
| P13-S04-R02 | Refus compte insuffisant | Rôle autorisé | Membre C | A tester |  |  |
| P13-S06-001 | Pré-clôture session caisse | Caissier | Session jour | A tester |  |  |
| P13-S06-002 | Contrôle caisse | Contrôleur | Session jour | A tester |  |  |
| P13-S06-003 | Vérifier écarts/rapports caisse | Contrôleur/RCI | Session jour | A tester |  |  |
| P13-S07-A01 | Dépense simple | Rôle autorisé | Session jour | A tester |  |  |
| P13-S07-B01 | Salaire partiel | Rôle autorisé | Employé | A tester |  |  |
| P13-S07-B02 | Salaire complet + anti-double paiement | Rôle autorisé | Employé | A tester |  |  |
| P13-S07-C01 | Transport Agent Terrain calculé | ADMIN/COO | Site | A tester |  |  |
| P13-S07-D01 | Apport propriétaire si disponible | ADMIN/COO/Caissier | Caisse | A tester |  |  |

## 6. Jour 5 - Rapports / Mes actions / supervision / audit

| ID | Etape | Rôle | Donnée | Statut | Anomalie | Preuve / note |
|---|---|---|---|---|---|---|
| P13-S08-001 | Revenus réels corrects | ADMIN/COO/RCI/Gérant | Période connue | A tester |  |  |
| P13-S08-002 | Flux exclus non comptés en revenus/charges | ADMIN/COO/RCI/Gérant | Période connue | A tester |  |  |
| P13-S08-003 | Charges salaires/primes/transport correctes | ADMIN/COO/RCI/Gérant | Période connue | A tester |  |  |
| P13-S08-004 | Filtres période/agence/site | ADMIN/COO/RCI/Gérant | Rapports | A tester |  |  |
| P13-S08-005 | Export/impression si disponible | ADMIN/COO/RCI/Gérant | Rapports | A tester |  |  |
| P13-S09-001 | Mes actions rôle par rôle | Tous rôles | Tâches créées | A tester |  |  |
| P13-S09-002 | Supervision sans traitement direct | COO/RCI/Gérant/ADMIN | Tâches autres rôles | A tester |  |  |
| P13-S10-001 | Audit création membre | ADMIN/RCI | Membre A/B/C | A tester |  |  |
| P13-S10-002 | Audit collecte/validation | ADMIN/RCI | Collecte jour | A tester |  |  |
| P13-S10-003 | Audit crédit complet | ADMIN/RCI | Crédit A | A tester |  |  |
| P13-S10-004 | Audit retrait | ADMIN/RCI | Membre B | A tester |  |  |
| P13-S10-005 | Audit caisse/clôture | ADMIN/RCI | Session jour | A tester |  |  |

## 7. Synthèse finale

| Indicateur | Valeur |
|---|---|
| Scénarios testés | 0 |
| Scénarios OK | 0 |
| Anomalies bloquantes | 0 |
| Anomalies majeures | 0 |
| Anomalies moyennes | 0 |
| Anomalies mineures / ergonomie | 0 |
| Décision pilote | Non décidée |
| Décideur | à renseigner |
| Date décision | à renseigner |
