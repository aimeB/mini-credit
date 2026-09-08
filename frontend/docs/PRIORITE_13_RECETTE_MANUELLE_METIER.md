# Priorité 13 - Recette manuelle métier complète Mini-Crédit 3N

Date de lancement : 2026-07-25

## 1. Objectif

Cette priorité valide l'application comme un utilisateur réel, rôle par rôle, page par page et workflow par workflow.

Cette phase ne consiste pas à développer. Toute anomalie observée doit être documentée avant correction dans le registre dédié : [PRIORITE_13_REGISTRE_ANOMALIES.md](PRIORITE_13_REGISTRE_ANOMALIES.md).

## 2. Règles de recette

- Ne pas corriger directement pendant l'exécution sans fiche anomalie.
- Ne pas modifier les règles métier, endpoints, DTO, calculs, workflows ou permissions RBAC pendant la recette.
- Ne jamais réintroduire `AGENT_BUREAU` ou `RESPONSABLE` comme rôles opérationnels.
- Tester avec des comptes distincts par rôle.
- Capturer les preuves : capture écran, URL, réponse API, console navigateur, logs backend si possible.
- Distinguer clairement une anomalie métier d'une amélioration UI.

## 3. Environnement et points de départ

| Elément | Valeur à renseigner |
|---|---|
| Frontend | http://localhost:4200 ou URL recette |
| Backend API | à confirmer selon environnement |
| Base de données | à confirmer |
| Date comptable de recette | 2026-07-25 ou date métier choisie |
| Navigateur | Chrome recommandé |
| Compte ADMIN de préparation | à renseigner hors document si secret |

Validation technique disponible au lancement :

- Tests Angular globaux : 418 SUCCESS.
- Build Angular développement : OK.
- Design harmonisé avec classes `mc-*`.
- Scan frontend : pas de réintroduction applicative des rôles supprimés.

## 4. Comptes à utiliser

Ne pas stocker les mots de passe dans ce fichier. Renseigner uniquement les identifiants non sensibles.

| Rôle | Identifiant recette | Employé lié | Agence | Site | Statut compte | Résultat |
|---|---|---|---|---|---|---|
| Agent Terrain | à renseigner | à renseigner | à renseigner | à renseigner | Actif | A tester |
| Gestionnaire | à renseigner | à renseigner | à renseigner | à renseigner | Actif | A tester |
| Contrôleur | à renseigner | à renseigner | à renseigner | à renseigner | Actif | A tester |
| Caissier | à renseigner | à renseigner | à renseigner | à renseigner | Actif | A tester |
| Chef de Bureau | à renseigner | à renseigner | à renseigner | à renseigner | Actif | A tester |
| COO | à renseigner | à renseigner | à renseigner | à renseigner | Actif | A tester |
| RCI | à renseigner | à renseigner | à renseigner | à renseigner | Actif | A tester |
| Gérant Général | à renseigner | à renseigner | à renseigner | à renseigner | Actif | A tester |
| ADMIN | à renseigner | optionnel | à renseigner | à renseigner | Actif | A tester |

Hiérarchie officielle à conserver :

1. Agent Terrain
2. Gestionnaire
3. Contrôleur
4. Caissier
5. Chef de Bureau
6. COO
7. RCI
8. Gérant Général
9. ADMIN comme rôle technique / supervision

## 5. Données de test à préparer

### 5.1 Organisation

| Donnée | Minimum attendu | Identifiant recette | Statut |
|---|---:|---|---|
| Agence | 1 | à renseigner | A vérifier |
| Site | 1 | à renseigner | A vérifier |
| Antenne / bureau selon modèle existant | 1 si applicable | à renseigner | A vérifier |
| Rattachement utilisateurs à antenne/site | tous rôles opérationnels | à renseigner | A vérifier |

### 5.2 Membres

| Membre | Usage | Identifiant | Préconditions |
|---|---|---|---|
| Membre A | collecte + crédit complet | à renseigner | compte épargne actif, dossier complet |
| Membre B | retrait épargne | à renseigner | solde épargne disponible suffisant |
| Membre C | refus / cas insuffisant | à renseigner | solde ou garanties insuffisants selon scénario |

### 5.3 Caisse

| Donnée | Attendu | Statut |
|---|---|---|
| Caisse active | au moins 1 | A vérifier |
| Session caisse du jour | ouverte par le Caissier | A créer/vérifier |
| Solde disponible | suffisant pour décaissement crédit et retrait épargne | A vérifier |
| Billetage | disponible pour clôture/contrôle | A vérifier |

### 5.4 Paramètres

| Paramètre | Valeur de recette attendue | Statut |
|---|---:|---|
| Frais demande crédit | 7 500 FC pour le test | A vérifier |
| Frais retrait épargne | selon paramétrage existant | A vérifier |
| Transport site Agent Terrain | montant journalier par site | A vérifier |
| Prix carnet | si module actif | A vérifier |
| Coût carnet | si module actif | A vérifier |
| Règles paie | visibles si écran disponible | A vérifier |

### 5.5 Crédit

| Elément | Valeur recette |
|---|---:|
| Montant demande OK | 100 000 FC |
| Frais demande | 7 500 FC |
| Durée | 3 mois ou 6 mois |
| Garantie épargne | 20 000 FC pour 100 000 FC |
| Gage matériel OK | valeur >= 200 000 FC |
| Gage matériel insuffisant | valeur < 200 000 FC |

### 5.6 Collecte

Prévoir une collecte terrain avec :

- épargne collectée ;
- remboursement crédit collecté si crédit actif ;
- carnet vendu si applicable ;
- billetage ;
- contrôle et validation.

## 6. Scénarios de recette

Chaque scénario doit être exécuté avec preuves et résultat dans [PRIORITE_13_JOURNAL_EXECUTION.md](PRIORITE_13_JOURNAL_EXECUTION.md).

### S01 - Connexion, accès, menus, pages interdites

Pour chaque rôle :

1. Se connecter.
2. Vérifier dashboard affiché.
3. Vérifier menus visibles.
4. Vérifier menus interdits absents.
5. Tester accès direct par URL aux pages interdites.
6. Vérifier message 403 ou redirection propre.
7. Vérifier Mes actions.
8. Vérifier supervision si applicable.
9. Se déconnecter.

Points spécifiques :

- Agent Terrain : voit collecte terrain, ne voit pas caisse libre, ne valide pas crédit.
- Gestionnaire : voit pré-analyse, ne valide pas financièrement, ne fait pas caisse, ne valide pas garantie.
- Contrôleur : voit contrôles, valide collecte/retrait/garantie, ne paie pas caisse libre.
- Caissier : voit caisse, frais crédit, retraits à payer, décaissement ; ne voit pas analyse/approbation complète ; ne crée pas opération libre.
- Chef de Bureau : approuve crédit, supervise antenne, opération libre seulement si règle actuelle l'autorise.
- COO / Gérant Général : supervision/rapports, pas de traitement direct sauf règle existante.
- RCI : audit/contrôle/supervision, pas de tâche métier opérationnelle, vérifier le bouton Ajouter opération dans détail session caisse.
- ADMIN : administration et supervision technique.

### S02 - Collecte terrain complète

Workflow : Terrain -> Billetage -> Contrôle -> Validation -> Génération automatique.

1. Agent Terrain crée une collecte du jour.
2. Ajouter lignes : épargne membre, remboursement crédit, carnet vendu si applicable.
3. Vérifier montant total, membre, référence, site/antenne, agent terrain, statut.
4. Soumettre la collecte.
5. Vérifier tâche ou vue billetage.
6. Caissier effectue le billetage si le workflow actuel le prévoit.
7. Contrôleur contrôle la collecte.
8. Contrôleur valide.
9. Vérifier génération automatique : opération épargne, remboursement crédit, opération caisse liée aux revenus réels, carnet, audit.
10. Vérifier idempotence : actualisation ou double clic ne génère pas deux fois.
11. Vérifier rapport : épargne/principal exclus, intérêts/pénalités/carnets en revenus, primes terrain seulement après validation.

Résultat attendu : collecte validée, mouvements générés, aucune erreur 403, aucune double génération, traçabilité complète.

### S03 - Demande crédit complète

Workflow : Demande -> Pré-analyse -> Analyse -> Garantie -> Approbation -> Décaissement -> Remboursement -> Clôture.

A. Création demande :

1. Créer demande crédit pour Membre A.
2. Montant 100 000 FC.
3. Frais 7 500 FC.
4. Durée 3 ou 6 mois.
5. Objet et gage renseignés.
6. Soumettre.
7. Vérifier frais conservés, durée conservée, statut, tâche Gestionnaire, aucune valeur forcée à 5 000 ou 1 mois.

B. Encaissement frais :

1. Se connecter Caissier.
2. Aller à Frais crédit à encaisser.
3. Encaisser les frais.
4. Vérifier pas d'IDs techniques, session/caisse/caissier en lecture seule, montant prérempli, pas de dépôt garantie.
5. Valider.
6. Vérifier frais payés, opération caisse entrée, revenu frais demande, audit.

C. Pré-analyse Gestionnaire :

1. Se connecter Gestionnaire.
2. Aller dans Mes actions.
3. Ouvrir demande.
4. Faire pré-analyse.
5. Vérifier tâche Contrôleur créée.

D. Analyse Contrôleur :

1. Se connecter Contrôleur.
2. Analyser demande.
3. Valider analyse.
4. Vérifier passage à l'étape garantie.

E. Garantie :

1. Vérifier garantie épargne = 20% du montant demandé.
2. Vérifier gage matériel accepté si valeur >= 2 x montant demandé.
3. Tester cas OK.
4. Tester cas insuffisant sur autre demande.
5. Valider garantie si conditions OK.

F. Approbation :

1. Se connecter Chef de Bureau.
2. Vérifier tâche approbation.
3. Approuver crédit.
4. Vérifier crédit créé / approuvé.

G. Décaissement :

1. Se connecter Caissier.
2. Vérifier tâche décaissement.
3. Vérifier session caisse ouverte.
4. Décaisser.
5. Vérifier opération caisse sortie, statut crédit, quittance, audit, tâche clôturée.

Résultat attendu : workflow complet sans contournement, chaque rôle agit seulement à son étape, traçabilité complète.

### S04 - Remboursement crédit

Tester si disponibles :

- Remboursement direct espèces : session obligatoire, opération caisse entrée, reste à payer, pénalités, audit.
- Remboursement depuis compte épargne : pas de session caisse, débit épargne, solde disponible suffisant, solde bloqué intact, pas d'opération caisse espèces, audit.
- Remboursement via collecte terrain : génération remboursement après validation collecte, prime Agent Terrain 2% si règle active.

Cas de refus : paiement supérieur au reste dû, compte insuffisant, crédit non décaissé, utilisateur non autorisé.

### S05 - Retrait épargne

Workflow : Demande -> Contrôle -> Validation Contrôleur -> Paiement Caissier.

1. Créer demande retrait pour Membre B.
2. Vérifier frais retrait si applicable.
3. Contrôleur valide.
4. Caissier paie.
5. Vérifier sortie caisse, entrée frais si applicable, compte débité, session, référence, audit.

Cas de refus : paiement sans validation, solde insuffisant, utilisateur non autorisé, absence session caisse.

### S06 - Caisse

Tester : ouverture session, consultation, opérations liées métier, opération libre si autorisée, refus Caissier/RCI opération libre, pré-clôture, billetage/clôture, contrôle caisse, écarts, rapports caisse.

Vérifier : solde théorique, solde physique, écarts, source opération, référence métier, utilisateur, antenne, commentaire.

### S07 - Dépenses, salaires, transport

1. Dépense simple : créer, valider si workflow, payer, vérifier sortie caisse et charge.
2. Salaire : employé, période, salaire base, prime fixe, bonus, primes Agent Terrain, payé, reste, anti-double paiement.
3. Transport Agent Terrain : montant journalier x agents actifs x jours, prévu/payé/restant, séparé salaire/primes.
4. Apport propriétaire si module disponible : financement non revenu, remboursement non charge.

### S08 - Rapports revenus / résultat

Vérifier sur période connue :

- Revenus réels : frais crédit, frais retrait, intérêts, pénalités, carnets.
- Flux exclus : épargne collectée, principal remboursé, garanties, décaissements, retraits, apports.
- Charges : salaires, primes Agent Terrain, transport, fonctionnement, achat carnets.
- Indicateurs : capital dehors, principal récupéré, crédits actifs/remboursés, capital propriétaire, capacité retrait propriétaire, alertes, filtres, export/impression.

Résultat attendu : aucune confusion entrée caisse / revenu, pas de double comptage collecte, transport séparé, financement propriétaire séparé.

### S09 - Mes actions / supervision

Pour chaque rôle opérationnel :

1. Vérifier Mes actions.
2. Vérifier tâche attendue.
3. Ouvrir dossier.
4. Faire action.
5. Vérifier tâche terminée ou suivante créée.

Pour superviseurs :

1. ADMIN / COO / RCI / Gérant Général consultent supervision.
2. Vérifier absence bouton Terminer si tâche d'un autre rôle.
3. Vérifier compteur personnel : ne compte pas la supervision.

### S10 - Audit / traçabilité

Pour chaque opération importante, vérifier : utilisateur, rôle, date, heure, antenne, site, référence métier, type action, commentaire, montant financier, avant/après si disponible.

Opérations à couvrir : création membre, collecte, validation collecte, frais crédit, pré-analyse, analyse, garantie, approbation, décaissement, remboursement, retrait, dépense, salaire, transport, opération caisse, clôture session.

## 7. Ordre recommandé

| Jour | Périmètre | Livrable attendu |
|---|---|---|
| Jour 1 | Connexion, menus, rôles, organisation, utilisateurs, caisse initiale | Matrice rôles renseignée, données prêtes |
| Jour 2 | Collecte terrain, épargne, retrait épargne | Collecte et retrait validés ou anomalies documentées |
| Jour 3 | Crédit complet, frais, garantie, décaissement | Crédit décaissé ou anomalies documentées |
| Jour 4 | Remboursements, dépenses, salaires, transport | Flux financiers contrôlés |
| Jour 5 | Rapports, Mes actions, supervision, audit, revue anomalies | Décision pilote |

## 8. Critères de décision pilote

Feu vert pilote possible si :

- aucune anomalie bloquante ouverte ;
- aucune anomalie majeure sécurité/RBAC ouverte ;
- les workflows collecte, crédit, retrait, caisse passent au moins une fois de bout en bout ;
- les rapports ne confondent pas revenus réels et flux exclus ;
- l'audit trace les opérations critiques ;
- les comptes de rôles supprimés ne réapparaissent pas dans l'application.

Feu rouge si :

- contournement RBAC confirmé ;
- double génération financière ;
- erreur de calcul crédit/garantie/caisse/rapport ;
- action opérationnelle disponible pour RCI ou rôle superviseur sans règle explicite ;
- absence de traçabilité sur opération financière critique.
