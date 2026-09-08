# Bible fonctionnelle et technique de l'application Mini-Crédit 3N

Date de génération : 2026-07-23

Périmètre analysé :
- Frontend Angular : `mini-credit-front`
- Backend Spring Boot : `C:\Users\admin\Documents\mini-credit`
- Analyse statique du code et des documents de contexte disponibles dans le workspace

Important : ce document décrit le fonctionnement actuel observé dans le code. Il ne corrige rien et n'invente pas de règle métier. Les règles fournies explicitement dans la demande sont marquées comme "règle métier officielle fournie". Les règles observées dans le code sont marquées comme "règle confirmée par le code". Les règles inférées par combinaison de routes, services ou statuts sont marquées comme "règle déduite du code".

## 1. Introduction

Mini-Crédit 3N est une application de gestion de micro-crédit, épargne, caisse, collecte terrain, personnel, paie, transport, financement propriétaire, rapports financiers et audit. L'application est structurée autour d'un frontend Angular standalone et d'un backend Spring Boot exposant des API REST sous `/api`.

La séparation des tâches est centrale : les opérations financières, la validation, le contrôle, la supervision et l'audit sont répartis entre Agent Terrain, Gestionnaire, Contrôleur, Caissier, Chef de Bureau, COO, RCI, Gérant Général et ADMIN.

## 2. Vue globale de l'application

Architecture observée :
- Angular avec routes protégées par `AuthGuard`, `RoleGuard` et parfois `PermissionGuard`.
- Backend Spring Boot avec contrôleurs REST, services métier, repositories JPA et entités de domaine.
- Sécurité basée sur JWT côté frontend, Spring Security côté backend, rôles `RoleCode` et permissions `PermissionCode`.
- Workflows transverses alimentés par `workflow_task` et affichés dans `/mes-actions`.
- Traçabilité partagée entre annotations `@Auditable`, `AuditService`, `JournalAudit` et champs métier tels que `createdBy`, `validePar`, `payePar`, `controlePar`, `completedBy`.

Modules fonctionnels principaux :
- Authentification et activation.
- Membres et profils.
- Crédit et garanties.
- Epargne et retraits.
- Caisse, sessions, opérations, dépenses et anomalies.
- Collecte terrain et recettes journalières.
- Personnel, salaires et transport.
- Apports propriétaire et financement.
- Rapports financiers et contrôle interne.
- Audit et Mes actions.

## 3. Rôles et responsabilités

Hiérarchie officielle fournie :
1. AGENT_TERRAIN
2. GESTIONNAIRE
3. CONTROLEUR
4. CAISSIER
5. CHEF_BUREAU
6. COO
7. RCI
8. GERANT_GENERAL
9. ADMIN comme rôle technique/supervision

Rôle additionnel observé :
- MEMBER : membre/client, accès limité à ses propres données.

Compatibilité legacy :
- `AGENT_BUREAU` existe encore dans `RoleCode` avec `@Deprecated` et est mappé vers `GESTIONNAIRE` dans certains chemins frontend/backend.
- `RESPONSABLE` existe encore avec `@Deprecated` et est mappé vers `CHEF_BUREAU`.
- Ces rôles ne doivent pas être considérés comme opérationnels. Leur présence est un risque de confusion RBAC si des données historiques les utilisent encore.

### AGENT_TERRAIN

Objectif : prospection, suivi terrain, saisie de collecte quotidienne, accès membres sur son périmètre.

Ecrans accessibles observés : `/membres`, `/membres/:id`, `/epargne`, `/epargne/membre/:membreId`, `/collectes/ma-collecte`, `/collectes/:id/edition`, `/collectes/mes-collectes`, `/credits`, `/credits/:creditId`.

Actions autorisées :
- Consulter et parfois créer des membres selon route frontend.
- Saisir sa collecte du jour.
- Saisir lignes de collecte : épargne, remboursement crédit, carnet, demande crédit selon modèle frontend/backend.
- Soumettre la collecte.

Actions interdites ou non visibles :
- Validation crédit, garantie, approbation, décaissement.
- Billetage caisse.
- Validation contrôleur.
- Paiement caisse.
- Administration.

Restrictions : périmètre antenne/site déduit du rattachement agent/utilisateur. Les tâches workflow ne sont pas principalement destinées à ce rôle dans le service actuel.

Impacts caisse : indirects via collecte validée, pas par opération caisse libre.

Risques : le menu crédit est visible à AGENT_TERRAIN, mais les routes de création de demande crédit ne l'autorisent pas actuellement côté frontend. Le backend a récemment renforcé le cas collecte : si un membre a déjà un crédit bloquant, la demande crédit via collecte est rejetée.

### GESTIONNAIRE

Objectif : pré-analyse crédit, supervision terrain et suivi administratif.

Ecrans accessibles : membres, épargne, crédits, demandes crédit, pré-analyses, recettes/collectes suivi terrain, rapports selon certaines routes.

Workflow principal :
- Reçoit une tâche `TRAITER_DEMANDE_CREDIT` pour une demande soumise.
- Réalise la pré-analyse.
- Si validation, le service crée ensuite une tâche contrôleur `CONTROLER_DEMANDE_CREDIT`.

Actions interdites : approbation finale, contrôle garantie opérationnel, décaissement, paiement caisse.

Risques : le menu `Agents` est visible uniquement ADMIN alors que la route `/admin/agents` accepte aussi GESTIONNAIRE. Incohérence frontend de navigation.

### CONTROLEUR

Objectif : contrôle crédit, validation retraits épargne, contrôle collecte/recette, contrôle caisse, garanties, audit opérationnel.

Ecrans accessibles : caisse lecture/contrôle, retraits épargne, crédits analyse/garantie, recettes validation, collectes à contrôler, rapports revenus, audit selon routes.

Actions :
- Analyse risque crédit.
- Contrôle garantie et validation/rejet garantie.
- Validation/rejet retrait épargne.
- Contrôle collecte après billetage.
- Validation contrôle session caisse.
- Lecture rapports et audit.

Actions interdites : décaissement crédit, paiement retrait, approbation Chef de Bureau, paiement dépenses sauf permissions spécifiques non observées comme rôle principal.

Impacts caisse : contrôle et validation, généralement pas création libre. Des générations internes peuvent produire opérations après validation selon service.

### CAISSIER

Objectif : opérations de caisse cadrées par workflows.

Ecrans accessibles : caisse, sessions caisse, ouverture/pré-clôture, journal session, dépenses, retraits épargne à payer, frais crédit à encaisser, billetage collecte, décaissement crédit.

Actions autorisées :
- Ouvrir une session caisse.
- Pré-clôturer/soumettre au contrôle une session.
- Confirmer le billetage d'une collecte soumise.
- Encaisser frais initiaux de demande crédit via écran dédié.
- Décaisser crédit approuvé.
- Payer retrait épargne validé.
- Payer dépense validée selon workflow.

Actions interdites :
- Créer une opération caisse libre. Le backend `OperationCaisseServiceImpl` interdit la création libre au CAISSIER hors workflows autorisés.
- Analyser crédit, valider garantie, approuver crédit.

Impacts caisse : forts. Tout paiement/encaissement doit être rattaché à session ouverte et caisse active.

Risques : certains écrans de caisse sont accessibles au Caissier en lecture/gestion, mais la création manuelle `/caisses/session/:sessionId/operations/nouveau` est réservée ADMIN/CHEF_BUREAU/RCI côté frontend.

### CHEF_BUREAU

Objectif : supervision antenne, validation/approbation, autorisation dépenses, rapports.

Actions :
- Approver demande crédit après contrôle garantie.
- Valider certaines dépenses caisse.
- Clôturer session après contrôle validé selon workflow task.
- Consulter rapports et audit.

Risques : `Organisation` et `Utilisateurs` routes autorisent CHEF_BUREAU mais la navbar les masque ou limite à ADMIN selon inventaire frontend.

### COO

Objectif : supervision transverse.

Ecrans accessibles : `/mes-actions`, rapports, revenus, dashboard contrôle interne selon routes/permissions, remboursement apport validation/rejet backend.

Actions observées : supervision et validation de remboursement apport propriétaire avec GERANT_GENERAL/ADMIN.

Risques : dans `WorkflowTaskServiceImpl`, COO avec permission de supervision ne voit que les tâches dont `roleDestinataire == COO`, sauf lecture globale dans un bloc puis filtre `isVisibleToConnectedRole`. Cela peut limiter la supervision effective.

### RCI

Objectif : contrôle interne, audit, investigation, supervision sans rôle opérationnel courant.

Ecrans : audit, rapports revenus, caisse contrôle, anomalies, écarts, rapports caisse, garanties lecture.

Actions interdites officiellement selon commentaire `RoleCode` : ne valide pas les opérations courantes, n'accepte pas les variances, n'administre pas le système.

Risque majeur : la route frontend `/caisses/session/:sessionId/operations/nouveau` autorise `RCI` à créer une opération caisse manuelle, alors que le commentaire métier du rôle RCI indique un rôle d'audit sans action opérationnelle. A vérifier/corriger métier.

### GERANT_GENERAL

Objectif : gouvernance globale et supervision de haut niveau.

Ecrans : rapports, revenus, audit, dashboard contrôle interne, Mes actions selon permissions.

Actions backend : validation/rejet remboursement apport propriétaire possible avec ADMIN/COO.

### ADMIN

Objectif : rôle technique et supervision complète.

Accès : majorité des modules, bypass de certaines permissions frontend (`permissionBypassRoles: ['ADMIN']` pour Mes actions).

Risques : normal qu'ADMIN ait accès large, mais il faut distinguer usage technique et opérations métier dans les audits.

### MEMBER

Objectif : accès client à ses propres données.

Ecrans : profil membre, création demande retrait épargne, détail retrait, mes demandes crédit côté backend.

Restrictions : accès scopé au membre connecté. Doit être vérifié côté backend à chaque endpoint.

## 4. Matrice complète des permissions

### Matrice rôles x modules

| Rôle | Module | Lecture | Création | Modification | Validation | Paiement | Suppression | Observation |
|---|---|---:|---:|---:|---:|---:|---:|---|
| AGENT_TERRAIN | Membres | Oui | Oui route nouveau | Non edit | Non | Non | Non | Périmètre terrain |
| AGENT_TERRAIN | Collecte | Oui | Oui | Oui brouillon | Non | Non | Ligne supprimable avant soumission | Saisie terrain |
| AGENT_TERRAIN | Crédit | Partiel | Via collecte seulement si autorisé | Non | Non | Non | Non | Création directe non autorisée frontend |
| GESTIONNAIRE | Crédit | Oui | Oui | Pré-analyse | Pré-analyse | Non | Non | Tâche Mes actions |
| GESTIONNAIRE | Terrain | Oui | Non | Suivi | Non | Non | Non | Supervision collectes |
| CONTROLEUR | Crédit | Oui | Non | Analyse/garantie | Oui | Non | Non | Contrôle risque/garantie |
| CONTROLEUR | Retrait épargne | Oui | Non | Non | Oui | Non | Non | Validation/rejet |
| CONTROLEUR | Caisse | Oui | Non | Contrôle | Oui contrôle | Non | Non | Session/écarts |
| CAISSIER | Caisse | Oui | Session, workflow | Pré-clôture | Non | Oui | Non | Opérations libres interdites |
| CAISSIER | Crédit | Frais/décaissement | Non demande | Non | Non | Décaissement | Non | Session ouverte requise |
| CAISSIER | Retrait épargne | Oui validés | Peut créer demande retrait | Non | Non | Oui | Non | Paiement retrait |
| CHEF_BUREAU | Crédit | Oui | Oui | Non | Approbation | Non | Non | Décision finale antenne |
| CHEF_BUREAU | Dépense caisse | Oui | Oui | Non | Validation | Non | Non | Autorisation dépense |
| COO | Rapports | Oui | Non | Non | Supervision | Non | Non | Pilotage transverse |
| RCI | Audit/caisse | Oui | Incohérence opération caisse manuelle route | Non | Audit | Non | Non | Contrôle interne |
| GERANT_GENERAL | Rapports/audit | Oui | Non | Non | Gouvernance | Non | Non | Supervision globale |
| ADMIN | Tous modules | Oui | Oui | Oui | Oui | Oui | Oui si endpoint | Rôle technique |
| MEMBER | Profil/retrait | Ses données | Demande retrait | Annulation selon statut | Non | Non | Non | Scope personnel |

## 5. Modules fonctionnels

### Authentification

Routes : `/auth/login`, `/auth/activate`, `/auth/change-password`.
Services : `AuthService`, `ActivationService`, `PasswordChangeService`, `UsernameService`.
Endpoints : `/api/auth/login`, `/api/auth/register`, `/api/auth/activate`, `/api/auth/change-password`.
Risques : `ActivationService` utilise `/api/auth` directement, alors que la majorité des services passent par `API_BASE_URL`.

### Membres

Objectif : gérer les membres, leurs comptes, crédits et rattachements.
Entité : `Membre`.
Routes : `/membres`, `/membres/nouveau`, `/membres/:id`, `/membres/:id/edit`, `/member-profile`.
Endpoints : `/api/membres`, `/api/membres/search`, `/api/membres/{id}`, `/api/membres/me`.

### Crédit

Objectif : demande, analyse, garantie, approbation, décaissement, remboursement et clôture.
Entités : `DemandeCredit`, `Credit`, `GarantieCredit`, `GarantieMaterielle`, `EcheanceCredit`, `RemboursementCredit`.
Routes principales : `/credits`, `/credits/demandes`, `/credits/demandes/nouveau`, `/credits/demandes/:id`, `/credits/demandes/:id/analyse`, `/credits/demandes/:id/garantie`, `/credits/frais-a-encaisser`, `/credits/:creditId/decaissement`, `/credits/:creditId/remboursement`, `/credits/:creditId/contrat`.

### Epargne

Objectif : comptes épargne, opérations, retraits via workflow, garanties.
Entités : `CompteEpargne`, `OperationEpargne`, `DemandeRetraitEpargne`.
Routes : `/epargne`, `/epargne/membre/:membreId`, `/epargne/demandes-retrait`, `/epargne/demandes-retrait/nouveau`, `/epargne/demandes-retrait/:id`.

### Caisse

Objectif : sessions de caisse, opérations, dépenses, journal, écarts, rapports.
Entités : `Caisse`, `SessionCaisse`, `OperationCaisse`, `DepenseCaisse`, `RemboursementApportProprietaire`.
Routes : `/caisses`, `/caisses/nouveau`, `/caisses/session/ouverture`, `/caisses/session/:id`, `/caisses/session/:id/cloture`, `/caisses/session/:sessionId/operations`, `/caisses/depenses`, `/caisses/controle`, `/caisses/anomalies`, `/caisses/ecarts`.

### Collecte terrain / recettes

Objectif : collecte agent terrain, billetage, contrôle et génération d'opérations.
Entités : `CollecteJournaliereTerrain`, `CollecteMembreLigne`, `RecetteJournaliereTerrain`, `FicheJournaliereAgentTerrain`.
Routes : `/collectes/ma-collecte`, `/collectes/mes-collectes`, `/collectes/soumises-billetage`, `/collectes/a-controler`, `/collectes/suivi-terrain`, `/recettes`.

### Personnel / paie / transport

Objectif : employés, salaires, primes, dépenses paie, transport agent terrain.
Entités : `Employe`, `PaiementSalaire`, `DepenseCaisse`, `TransportSiteParametre`.
Routes : `/employes`, `/admin/transport-sites`.

### Rapports financiers

Objectif : revenus réels, charges, flux exclus, position crédit, trésorerie, fonds membres, capital propriétaire, alertes.
Route : `/rapports/revenus`.
Backend : `RapportRevenusController`, `RapportRevenusServiceImpl`.

## 6. Workflows métier complets

### A. Crédit complet

Règle métier officielle fournie : Demande -> Pré-analyse -> Analyse -> Garantie -> Approbation -> Décaissement -> Remboursement -> Clôture.

Règle confirmée par le code : `WorkflowTaskServiceImpl` crée les tâches suivantes :

| Etape | Statut/événement | Rôle | Tâche Mes actions | Entité | Endpoint représentatif |
|---|---|---|---|---|---|
| Création/soumission | demande soumise | GESTIONNAIRE | `TRAITER_DEMANDE_CREDIT` | DEMANDE_CREDIT | `POST /api/demandes-credit` |
| Pré-analyse validée | pré-analyse terminée | CONTROLEUR | `CONTROLER_DEMANDE_CREDIT` | DEMANDE_CREDIT | `POST /api/demandes-credit/{id}/pre-analyse` |
| Analyse risque validée | analyse validée | CONTROLEUR | `CONTROLER_DEMANDE_CREDIT` | DEMANDE_CREDIT | `POST /api/demandes-credit/{id}/analyse-risque/valider` |
| Garantie contrôlée | validation chef attendue | CHEF_BUREAU | `APPROUVER_DEMANDE_CREDIT` | DEMANDE_CREDIT | `POST /api/demandes-credit/{id}/controle-garantie` |
| Approbation | crédit créé/approuvé | CAISSIER | `DECAISSER_CREDIT` | CREDIT | `POST /api/credits/demande/{demandeId}/approbation` |
| Décaissement | crédit décaissé | CAISSIER | tâche clôturée | CREDIT | `POST /api/credits/{creditId}/decaissement` |
| Remboursement | remboursements | CAISSIER/Admin/Chef/Membre selon endpoint | non détaillé en tâche | CREDIT | `POST /api/credits/{creditId}/remboursements` |

Garantie crédit :
- Règle métier officielle fournie : garantie épargne obligatoire 20%.
- Règle confirmée par le code : `TAUX_GARANTIE_3N = 20` dans `GarantieCreditWorkflowServiceImpl`.
- Règle métier officielle fournie : gage matériel complémentaire si implémenté.
- Règle confirmée par le code : `GarantieMaterielle` peut être ajoutée, acceptée ou refusée.
- Règle métier officielle fournie : valeur totale garanties matérielles acceptées >= 2 x montant demandé si règle présente.
- Règle confirmée par le code : `COEFFICIENT_MIN_GAGE_MATERIEL = 2`; le service vérifie les garanties matérielles lors de la validation.

Pénalités :
- Règle métier officielle fournie : 2 500 FC par jour.
- Règle confirmée par le code : `PenaliteCreditServiceImpl` calcule `jours_retard x PENALITE_RETARD_JOURNALIERE`, défaut `2500` si paramètre absent/invalide.

Conditions d'entrée principales : membre et demande existants, statut compatible, frais/garantie/analyse conformes selon services.
Conditions de sortie : crédit approuvé, décaissé, remboursé ou rejeté.

Impacts caisse :
- Frais demande/analyse crédit : revenu réel.
- Décaissement crédit : flux exclu du résultat, sortie caisse non charge.
- Remboursement : principal exclu, intérêts et pénalités revenus réels.

Risques :
- Le service de tâches logge `targetUrl=/credits/demandes` de manière générique, sans stocker clairement une URL cible par action dans l'extrait observé. A confirmer dans `WorkflowTaskMapper`/DTO.
- Les transitions exactes de statuts `DemandeCredit` doivent être auditées dans `DemandeCreditServiceImpl` si un audit réglementaire exige chaque statut avant/après.

### B. Retrait épargne

Règle métier officielle fournie : Demande -> Contrôle -> Validation Contrôleur -> Paiement Caissier.

Règle confirmée par le code :
- Création : `POST /api/demandes-retrait-epargne`.
- Validation/rejet : endpoints `/{id}/valider`, `/{id}/rejeter` par permission contrôleur.
- Paiement/décaissement : endpoint `/{id}/decaisser` avec permission opération caisse.
- `WorkflowTaskServiceImpl` crée `VALIDER_RETRAIT_EPARGNE` pour CONTROLEUR puis `PAYER_RETRAIT_EPARGNE` pour CAISSIER.

Statuts observés : `CREEE`, `EN_ATTENTE_VALIDATION`, `VALIDEE`, `DECAISSEE`, `REJETEE`, `ANNULEE`.

Impacts :
- Frais retrait épargne : revenu réel.
- Retrait épargne : flux exclu, pas charge.
- Compte épargne diminué lors du décaissement.

Risque : l'entité `DemandeRetraitEpargne` ne porte pas explicitement `createdBy/demandePar`, ce qui affaiblit la traçabilité directe de la demande malgré le workflow.

### C. Collecte terrain / recette journalière

Règle métier officielle fournie : Terrain -> Billetage -> Contrôle -> Validation -> Génération automatique.

Règle confirmée par le code :
- Agent Terrain crée collecte et lignes.
- Agent soumet collecte.
- `onCollecteSoumise` crée tâche `EFFECTUER_BILLETAGE` pour CAISSIER.
- Caissier confirme billetage.
- `onCollecteBilletageConfirme` crée tâche `CONTROLER_RECETTE_TERRAIN` pour CONTROLEUR.
- Contrôleur/Admin valide ou rejette.
- Service prévoit génération d'opérations après validation.

Types de lignes : épargne, remboursement crédit, carnet, demande crédit et autres types selon enum `TypeLigneCollecte`.

Impacts caisse/rapport :
- Epargne collectée : flux exclu.
- Remboursement principal : flux exclu.
- Frais, intérêts, pénalités, carnets : revenus réels.
- Carnets vendus alimentent marge carnet dans le rapport.

Risques : coexistence de `recette_journaliere_terrain`, `collecte_journaliere_terrain`, `recette_terrain_journaliere` peut créer une ambiguïté de modèle.

### D. Epargne

Actions : versement, retrait via workflow, blocage/déblocage garantie, ajustement, consultation historique.

Règles confirmées :
- `OperationEpargneServiceImpl` vérifie solde disponible ou bloqué selon opération.
- Blocage garantie diminue `soldeDisponible` et augmente `soldeBloque`.
- Déblocage garantie inverse le mouvement.

Impacts caisse : versements/retraits peuvent créer opérations caisse selon mode et session.

### E. Caisse

Workflow session :
1. Ouverture par CAISSIER/ADMIN/CHEF_BUREAU.
2. Session `OUVERTE` : tâche `PRE_CLOTURER_SESSION_CAISSE` pour CAISSIER.
3. Pré-clôture `PRE_CLOTUREE` : tâche `CONTROLER_SESSION_CAISSE` pour CONTROLEUR.
4. Contrôle validé `VALIDEE_CONTROLE` : tâche `CLOTURER_SESSION_CAISSE` pour CHEF_BUREAU.
5. Clôture : toutes tâches session clôturées.

Statuts : `OUVERTE`, `PRE_CLOTUREE`, `VALIDEE_CONTROLE`, `CLOTUREE`, `FERMEE`, `ANNULEE`, `ANNULEE_ADMINISTRATIVEMENT`.

Règles confirmées :
- Une session active par caisse/utilisateur selon service.
- Date comptable du jour et soldes non négatifs.
- Opérations caisse libres interdites au Caissier hors workflows.
- Solde physique/théorique et écarts sont contrôlés.

Risques : `CLOTUREE` et `FERMEE` semblent redondants. Certains écrans caisse rapports ont `data.roles` sans `canActivate` visible.

### F. Personnel / paie

Règles métier officielles fournies :
- Agent Terrain peut avoir primes automatiques : 1% épargne collectée validée, 2% remboursements crédit collectés validés, 200 FC par carnet vendu.
- Autres postes : prime motivation manuelle si implémentée, motif obligatoire si prime > 0.
- Transport Agent Terrain séparé du salaire.

Règles confirmées par le code :
- `Employe` porte salaire base, prime, bonus et poste.
- `RapportRevenusServiceImpl` calcule masse salariale attendue, salaires payés, restants, écart rémunération, motif.
- `DepenseCaisse` rattache des dépenses à paie/transport.
- `TransportSiteParametre` définit montant journalier par site.

Points à confirmer : les formules exactes 1%, 2% et 200 FC doivent être vérifiées dans le service de consolidation agent terrain/paie si requis. Elles ne sont pas confirmées dans les extraits lus.

### G. Transport Agent Terrain

Règle confirmée : transport prévu est calculé dans `RapportRevenusServiceImpl` via paramètres de site et agents terrain, puis comparé aux paiements transport. Alertes de cohérence générées si écart.

Impact : charge prévisionnelle/restante dans résultat après charges fixes.

### H. Apports propriétaire / financement

Workflow confirmé :
- Demande remboursement apport : ADMIN/CHEF_BUREAU.
- Validation/rejet : ADMIN/COO/GERANT_GENERAL.
- Paiement : ADMIN/CAISSIER.
- Paiement crée opération caisse et impacte rapports.

Rapport : distingue approvisionnement caisse, apport propriétaire, remboursement apport et capital restant à récupérer.

### I. Rapports financiers

Règles métier officielles fournies et confirmées par code dans `RapportRevenusServiceImpl` :
- Frais demande crédit = revenu réel.
- Frais retrait épargne = revenu réel.
- Intérêts crédit = revenu réel.
- Pénalités crédit = revenu réel.
- Vente carnet = revenu réel.
- Epargne collectée = flux exclu, pas revenu.
- Principal crédit remboursé = flux exclu, pas revenu.
- Garantie = flux exclu, pas revenu.
- Approvisionnement caisse = financement, pas revenu.
- Décaissement crédit = flux exclu, pas charge.
- Retrait épargne = flux exclu, pas charge.
- Salaire, transport, fonctionnement, achat carnets = charges.

Indicateurs : total revenus, mouvements non revenus, charges, bénéfice net estimé, résultat prévisionnel après salaires, résultat après transport, fonds membres protégés, trésorerie prudente, position crédit, apports et capacité retrait propriétaire.

### J. Audit / journalisation

Sources :
- `@Auditable` sur contrôleurs/méthodes.
- `AuditService.logAction/logSuccess` dans services.
- `JournalAudit` table historique.
- Champs métier par entité (`createdBy`, `validePar`, `payePar`, `controlePar`, etc.).

Règle métier officielle fournie : tracer utilisateur, date, heure, antenne, commentaire éventuel, référence métier, action réalisée.

Etat actuel : partiellement confirmé. Beaucoup d'actions critiques tracent utilisateur/date/action/référence, mais toutes les entités n'ont pas antenne/commentaire et certaines FK sont stockées en `Long` simple.

## 7. Parcours détaillés par action

### Action : créer une demande crédit
- Rôle autorisé : ADMIN, GESTIONNAIRE, MEMBER côté backend; frontend ADMIN/CHEF_BUREAU/GESTIONNAIRE pour création directe.
- Ecran : formulaire demande crédit.
- Route frontend : `/credits/demandes/nouveau`.
- Endpoint : `POST /api/demandes-credit`.
- Données requises : membre, montant, durée, périodicité, taux/objet selon DTO.
- Contrôles : validation DTO, membre/site, règles service.
- Effets métier : crée `DemandeCredit`, probablement statut initial/soumis.
- Effets caisse : aucun direct.
- Effets audit : `DEMANDE_CREDIT_CREATED`.
- Tâche workflow : pré-analyse Gestionnaire après soumission.
- Risques : divergence frontend/backend sur rôle MEMBER.

### Action : encaisser frais crédit
- Rôle autorisé : CAISSIER.
- Route : `/credits/frais-a-encaisser`, `/credits/demandes/:id/paiement-initial`.
- Endpoint : `POST /api/demandes-credit/{demandeId}/paiement-initial`.
- Contrôles : session caisse ouverte du caissier, caisse active, frais > 0, statut demande autorisé, pas dépôt garantie sur cet écran.
- Effets caisse : crée `OperationCaisse` entrée.
- Effets rapport : revenu réel frais analyse/demande.
- Risques : dépendance forte à la session ouverte.

### Action : pré-analyser crédit
- Rôle : GESTIONNAIRE.
- Route : `/credits/demandes/:id` ou vue pré-analyse.
- Endpoint : `POST /api/demandes-credit/{id}/pre-analyse`.
- Effets : clôture tâche Gestionnaire, crée tâche Contrôleur.

### Action : analyser risque crédit
- Rôle : CONTROLEUR.
- Endpoint : `POST /api/demandes-credit/{id}/analyse-risque/valider` ou équivalent.
- Effets : demande passe à étape garantie, tâche Contrôleur de garantie créée.

### Action : contrôler garantie
- Rôle : CONTROLEUR.
- Route : `/credits/demandes/:id/garantie`.
- Endpoint : `/api/demandes-credit/{id}/garantie/**` et `/controle-garantie`.
- Effets : vérifie 20%, bloque épargne, ajoute/accepte/refuse gages, valide/rejette garantie.
- Audit : `GARANTIE_VERIFIED/BLOCKED/MATERIAL_*`.

### Action : approuver crédit
- Rôle : CHEF_BUREAU via permission `CREDIT_APPROVE`.
- Endpoint : `POST /api/credits/demande/{demandeId}/approbation`.
- Effets : crée/approuve `Credit`, échéances/contrat selon service, tâche décaissement Caissier.

### Action : décaisser crédit
- Rôle : CAISSIER.
- Route : `/credits/:creditId/decaissement`.
- Endpoint : `POST /api/credits/{creditId}/decaissement`.
- Contrôles : crédit approuvé, session/caisse, solde.
- Effets caisse : sortie flux exclu du résultat.
- Tâche : clôture `DECAISSER_CREDIT`.

### Action : créer/retrait épargne
- Rôle création : MEMBER, CAISSIER, ADMIN.
- Validation : CONTROLEUR.
- Paiement : CAISSIER.
- Endpoints : `/api/demandes-retrait-epargne`, `/{id}/valider`, `/{id}/decaisser`.
- Effets : solde épargne, opération caisse, frais retrait revenu.

### Action : ouvrir/pré-clôturer/contrôler/clôturer session caisse
- Rôles : CAISSIER ouvre et pré-clôture; CONTROLEUR contrôle; CHEF_BUREAU clôture finale selon workflow; ADMIN peut superviser.
- Endpoints : `/api/sessions-caisse/ouverture`, `/api/caisses/sessions/{id}/pre-cloturer`, validation contrôle, clôture finale.
- Effets : `SessionCaisse`, tâches workflow, audit, écarts.

### Action : créer/valider/payer dépense caisse
- Création : ADMIN, CAISSIER, CHEF_BUREAU selon routes.
- Validation : CHEF_BUREAU.
- Paiement : CAISSIER.
- Tâches : `DEPENSE_CAISSE_VALIDATE`, puis `DEPENSE_CAISSE_PAY`.
- Effets : `DepenseCaisse`, `OperationCaisse` sortie, charge rapport si catégorie charge.

## 8. Documentation frontend / routes Angular

Résumé des routes majeures :

| URL | Composant | Rôles frontend | Objectif | Risque |
|---|---|---|---|---|
| `/dashboard` | `DashboardComponent` | Auth | Accueil | Pas de RoleGuard spécifique |
| `/mes-actions` | `MesActionsComponent` | ADMIN, CAISSIER, CONTROLEUR, CHEF_BUREAU, GESTIONNAIRE, RCI, COO, GERANT_GENERAL | Tâches workflow | MEMBER exclu |
| `/membres` | `MembreListComponent` | ADMIN, CHEF_BUREAU, GESTIONNAIRE, AGENT_TERRAIN | Membres | Scope backend à vérifier |
| `/member-profile` | `MemberProfileComponent` | MEMBER | Profil membre | Accès limité |
| `/epargne` | `EpargneDashboardComponent` | ADMIN, CHEF_BUREAU, GESTIONNAIRE, AGENT_TERRAIN, CONTROLEUR | Epargne | Pas CAISSIER sur dashboard global |
| `/epargne/demandes-retrait` | lazy list | ADMIN, CHEF_BUREAU, CONTROLEUR, CAISSIER | Retraits | MEMBER voit détail seulement |
| `/collectes/ma-collecte` | `CollecteDuJourComponent` | AGENT_TERRAIN | Collecte agent | Critique caisse après validation |
| `/collectes/soumises-billetage` | `CollectesSoumisesBilletageComponent` | CAISSIER | Billetage | Rôle correct |
| `/collectes/a-controler` | `CollectesAControlerComponent` | CONTROLEUR, ADMIN | Contrôle collecte | RCI non autorisé |
| `/caisses` | `CaisseListComponent` | ADMIN, CHEF_BUREAU, CAISSIER, CONTROLEUR | Caisse | RCI absent sauf rapports/ecarts |
| `/caisses/session/:id` | `SessionCaisseDetailComponent` | ADMIN, CAISSIER, CONTROLEUR, CHEF_BUREAU | Session | RCI absent détail session |
| `/caisses/session/:sessionId/operations/nouveau` | `OperationCaisseFormComponent` | ADMIN, CHEF_BUREAU, RCI | Opération manuelle | RCI incohérent métier |
| `/caisses/controle` | `CaisseControleComponent` | ADMIN, CONTROLEUR, RCI | Contrôle caisse | OK audit |
| `/caisses/ecarts` | `EcartCaisseListComponent` | ADMIN, CONTROLEUR, RCI | Ecarts | Session route ajoute CAISSIER |
| `/credits/demandes` | `DemandeCreditListComponent` | ADMIN, CHEF_BUREAU, GESTIONNAIRE, CONTROLEUR | Demandes | Caissier via frais dédié |
| `/credits/frais-a-encaisser` | `FraisCreditAEncaisserComponent` | CAISSIER, ADMIN, CHEF_BUREAU, RCI | Frais crédit | RCI lecture/action à vérifier |
| `/credits/demandes/:id/garantie` | `GarantieCreditPageComponent` | ADMIN, CONTROLEUR, CHEF_BUREAU, CAISSIER, RCI | Garantie | Mutations backend limitées contrôleur/admin |
| `/rapports/revenus` | `RapportRevenusComponent` | ADMIN, GERANT_GENERAL, COO, RCI, CHEF_BUREAU, CONTROLEUR | Rapport revenus | Pas Gestionnaire |
| `/audit/logs` | `AuditListComponent` | ADMIN, GERANT_GENERAL, RCI, CHEF_BUREAU, CONTROLEUR | Audit | OK |

Navbar : visible selon `NavbarComponent.modules`. Incohérences observées :
- Menu Organisation visible ADMIN seulement alors que route accepte CHEF_BUREAU.
- Menu Utilisateurs visible ADMIN seulement alors que route accepte CHEF_BUREAU.
- Menu Agents visible ADMIN seulement alors que route accepte GESTIONNAIRE.
- Menu Collectes terrain inclut RCI vers `/collectes/suivi-terrain`, mais route n'accepte pas RCI.

Guards :
- `AuthGuard` protège le bloc applicatif.
- `RoleGuard` vérifie `data.roles`.
- `PermissionGuard` vérifie `data.permissions`.
- `permissionBypassRoles` existe pour ADMIN sur Mes actions.

## 9. Backend REST

Principaux contrôleurs et endpoints :

| Contrôleur | Endpoint principal | Rôles/permissions | Entités | Audit |
|---|---|---|---|---|
| `DemandeCreditController` | `/api/demandes-credit` | ADMIN, GESTIONNAIRE, MEMBER; contrôleur/chef selon actions | `DemandeCredit` | Oui sur création/validation/rejet selon méthode |
| `DemandeCreditPaiementController` | `/api/demandes-credit/{id}/paiement-initial` | CAISSIER | `DemandeCredit`, `OperationCaisse` | Partiel/service |
| `CreditController` | `/api/credits` | Permissions crédit, CAISSIER pour décaissement | `Credit`, `RemboursementCredit` | Oui approbation/remboursement |
| `GarantieCreditWorkflowController` | `/api/demandes-credit/{id}/garantie` | Lecture large, mutation ADMIN/CONTROLEUR | `GarantieCredit`, `GarantieMaterielle`, `CompteEpargne` | Oui |
| `OperationEpargneController` | `/api/operations-epargne` | ADMIN, CHEF_BUREAU, GESTIONNAIRE, CAISSIER | `OperationEpargne`, `CompteEpargne` | Oui |
| `DemandeRetraitEpargneController` | `/api/demandes-retrait-epargne` | MEMBER/CAISSIER/ADMIN, CONTROLEUR, CAISSIER | `DemandeRetraitEpargne`, `OperationEpargne` | Oui |
| `CaisseController` | `/api/caisses` | ADMIN création, lecture caisse | `Caisse` | Non explicite controller |
| `SessionCaisseController` | `/api/sessions-caisse`, `/api/caisses/sessions` | CAISSIER, CONTROLEUR, CHEF_BUREAU, ADMIN | `SessionCaisse` | Service/workflow |
| `OperationCaisseController` | `/api/operations-caisse` | Permissions caisse | `OperationCaisse` | Oui |
| `DepenseCaisseController` | `/api/depenses-caisse` | DEPENSE permissions/rôles | `DepenseCaisse`, `OperationCaisse` | Oui sur actions sensibles |
| `CollecteTerrainController` | `/api/collectes-terrain` | AGENT_TERRAIN, CAISSIER, CONTROLEUR, ADMIN | `CollecteJournaliereTerrain`, lignes | Workflow/audit service |
| `RapportRevenusController` | `/api/rapports/revenus` | ADMIN, GERANT_GENERAL, COO, RCI, CHEF_BUREAU, CONTROLEUR | Lecture agrégée | Pas audit explicite lecture |
| `WorkflowTaskController` | `/api/me/actions`, `/api/actions` | TASK_* | `WorkflowTask` | Oui tâche terminée |
| `EmployeController` | `/api/employes` | ADMIN, exceptions CHEF_BUREAU | `Employe` | Non explicite |
| `RemboursementApportProprietaireController` | `/api/remboursements-apport-proprietaire` | ADMIN/CHEF_BUREAU/COO/GERANT_GENERAL/CAISSIER | `RemboursementApportProprietaire` | Oui |
| `AuditController` | `/api/audit-logs` legacy | ADMIN/AUDIT_READ/RCI selon endpoints | `AuditLog` | Lecture |

## 10. Base de données / entités

| Table | Objectif | Champs clés | Relations | Statuts/enums | Risques |
|---|---|---|---|---|---|
| `utilisateur` | Compte applicatif | username, actif, enabled, locked | role, site, membre, employe | RoleCode | Booléens d'état redondants |
| `role` | Rôle RBAC | code, libelle, active | permissions | RoleCode | Legacy présent |
| `permissions` | Permission granulaire | code, active | role_permissions | PermissionCode | Nom pluriel |
| `role_permissions` | Association RBAC | role_id, permission_id | role/permission | - | OK |
| `membre` | Client | code, identité, statut | site, agent, comptes, crédits | StatutMembre | Scope à auditer |
| `compte_epargne` | Solde épargne | soldeDisponible, soldeBloque | membre | TypeCompte, StatutCompte | Verrou optimiste |
| `operation_epargne` | Mouvements épargne | type, sens, montant | compte, membre, crédit, session | TypeOperationEpargne | Impact caisse variable |
| `demande_credit` | Dossier crédit | montant, durée, frais, statut, garantie | membre, site, crédit | StatutDemandeCredit | Nombreuses transitions |
| `credit` | Crédit actif | montant, encours, statut | demande, membre, échéances | StatutCredit | Exposition financière |
| `garantie_credit` | Garantie épargne | requis, bloqué, manquant | demande, compte, membre | StatutGarantieCredit | 20% confirmé |
| `garantie_materielle` | Gage | type, valeur, statut | garantie_credit | StatutGarantieMaterielle | 2x montant confirmé si exigé |
| `echeance_credit` | Echéancier | principal, intérêt, pénalité | crédit | A_PAYER/PARTIEL/PAYE/RETARD | Retards/pénalités |
| `remboursement_credit` | Paiement crédit | principal, intérêt, pénalité | crédit, échéance, session | - | Principal vs revenus à distinguer |
| `demande_retrait_epargne` | Workflow retrait | montant, frais, statut | compte, membre, validePar | CREEE, VALIDEE... | Pas createdBy explicite |
| `caisse` | Caisse | code, devise, actif | agence, site, caissier | actif | Unicité active |
| `session_caisse` | Session | soldes, dates, écart | caisse, utilisateurs | OUVERTE, PRE_CLOTUREE... | CLOTUREE/FERMEE redondants |
| `operation_caisse` | Journal caisse | type, catégorie, source, montant | session, caisse, membre, crédit | Categorie/Source | Certaines FK en Long simple |
| `depense_caisse` | Dépense | catégorie, montant, statut | caisse, session, employé | BROUILLON..PAYEE | Workflow sensible |
| `collecte_journaliere_terrain` | Collecte agent | totaux, billetage, statut | agent, site, lignes | BROUILLON, SOUMISE... | Modèle récent |
| `recette_journaliere_terrain` | Recette terrain | date, montant, statut | agent, fiche | EN_ATTENTE... | Coexiste avec collecte |
| `workflow_task` | Mes actions | typeAction, module, role, activeKey | utilisateurs | A_FAIRE, EN_COURS, TERMINEE | TargetUrl à clarifier |
| `employe` | Personnel | poste, salaire, prime | utilisateur, agence, site | PosteEmploye | Paie/transport |
| `paiement_salaire` | Paiement salaire | montant, statut | employe | INITIE..ANNULE | Moins intégré que DepenseCaisse |
| `fiche_journaliere_agent_terrain` | Synthèse terrain | totaux, variance | agent, recettes | BROUILLON..ANNULEA | Typo ANNULEA |
| `journal_audit` | Audit | action, table, ancienne/nouvelle valeur | utilisateur | ActionAudit | Ne prolonge pas BaseEntity |
| `remboursement_apport_proprietaire` | Remboursement apport | montant, statut | antenne, utilisateurs, opération | DEMANDE..PAYEE | Sortie sensible |
| `transport_site_parametre` | Transport site | montantJournalier, période | site | actif | createdBy en Long |

## 11. Mes actions / workflow_task

Fonctionnement :
- `/api/me/actions` charge les tâches visibles et déclenche une synchronisation opportuniste selon rôle et antenne.
- `/api/me/actions/count` compte les tâches `A_FAIRE`.
- `/api/actions/{id}/marquer-vue` passe `A_FAIRE -> EN_COURS`.
- `/api/actions/{id}/terminer` passe `TERMINEE`, renseigne `completedBy`, `completedAt`, commentaire et vide `activeKey`.
- `activeKey` évite les doublons actifs pour même module/action/entité/rôle/antenne.
- Si `antenneId` est null, `upsertRoleTask` ignore la création de tâche.

Types d'actions observés :

| Type action | Rôle destinataire | Déclencheur | Fermeture |
|---|---|---|---|
| `TRAITER_DEMANDE_CREDIT` | GESTIONNAIRE | demande crédit soumise | pré-analyse validée/rejet |
| `CONTROLER_DEMANDE_CREDIT` | CONTROLEUR | pré-analyse ou analyse validée | étape suivante/rejet |
| `APPROUVER_DEMANDE_CREDIT` | CHEF_BUREAU | contrôle garantie terminé | approbation/rejet |
| `DECAISSER_CREDIT` | CAISSIER | demande approuvée/crédit créé | décaissement ou crédit non éligible |
| `VALIDER_RETRAIT_EPARGNE` | CONTROLEUR | retrait créé/en attente | validation/rejet |
| `PAYER_RETRAIT_EPARGNE` | CAISSIER | retrait validé | paiement |
| `EFFECTUER_BILLETAGE` | CAISSIER | collecte soumise | billetage confirmé |
| `CONTROLER_RECETTE_TERRAIN` | CONTROLEUR | recette/collecte à contrôler | validation/rejet |
| `PRE_CLOTURER_SESSION_CAISSE` | CAISSIER | session ouverte | pré-clôture |
| `CONTROLER_SESSION_CAISSE` | CONTROLEUR | session pré-clôturée | contrôle validé |
| `CLOTURER_SESSION_CAISSE` | CHEF_BUREAU | contrôle validé | clôture |
| `DEPENSE_CAISSE_VALIDATE` | CHEF_BUREAU | dépense en attente validation | validation/rejet |
| `DEPENSE_CAISSE_PAY` | CAISSIER | dépense validée non payée | paiement |

Risques :
- Les rôles de supervision COO/GERANT_GENERAL/ADMIN sont filtrés par rôle destinataire dans `isVisibleToConnectedRole`, ce qui peut empêcher de voir toutes les tâches malgré permission supervise.
- Le log mentionne `targetUrl=/credits/demandes` de manière générique; la documentation doit confirmer si `targetUrl` est réellement stockée dans DTO ou résolue côté frontend.

## 12. Sécurité / RBAC

Rôles officiels dans `RoleCode` : ADMIN, CHEF_BUREAU, GESTIONNAIRE, AGENT_TERRAIN, CONTROLEUR, CAISSIER, COO, RCI, GERANT_GENERAL, MEMBER.

Legacy : RESPONSABLE, AGENT_BUREAU, tous deux `@Deprecated`.

Frontend :
- `AuthGuard` vérifie session authentifiée.
- `RoleGuard` vérifie rôles de route.
- `PermissionGuard` vérifie permissions granulaires.
- Navbar filtre par rôle normalisé, avec mapping legacy.

Backend :
- Annotations `@PreAuthorize` par rôle/permission.
- `ScopeService` et méthodes de scope pour lecture membres/crédits/antennes.
- Restrictions caisse au niveau service, notamment interdiction d'opération libre pour CAISSIER.

Incohérences RBAC à auditer :
1. Routes rapports caisse avec `data.roles` sans `canActivate` visible.
2. Navbar masque des routes autorisées : Organisation, Utilisateurs, Agents.
3. Navbar donne RCI vers suivi terrain mais route refuse RCI.
4. RCI autorisé frontend sur opération caisse manuelle, contradictoire avec rôle audit.
5. Backend peut autoriser MEMBER à créer demande crédit alors que frontend direct ne l'expose pas.

## 13. Audit et traçabilité

Actions importantes avec audit confirmé ou probable :
- Création demande crédit : `@Auditable DEMANDE_CREDIT_CREATED`.
- Approbation crédit : `@Auditable CREDIT_APPROVED`.
- Remboursement crédit : `@Auditable REMBOURSEMENT_CREATED`.
- Garantie : `AuditService.logSuccess` pour vérification, blocage, matériel, validation, rejet.
- Opération épargne : `@Auditable OPERATION_EPARGNE_CREATED`.
- Retrait épargne : audits create/validate/reject/cancel observés dans controller/service.
- Opération caisse : `@Auditable OPERATION_CAISSE_CREATED`.
- Tâche workflow : création/terminaison auditées via `auditTaskAction` ou `AuditService.logAction`.
- Remboursement apport : `@Auditable` et log business event.

Lacunes possibles :
- Lecture de rapport revenus non auditée explicitement.
- Certaines entités n'ont pas créateur explicite.
- Certaines relations métier en `Long` simple compliquent les jointures d'audit.

## 14. Caisse et impacts financiers

### Matrice flux financiers

| Opération | Rôle | Entrée/sortie | Revenu réel | Charge réelle | Flux exclu | Impact caisse | Impact rapport |
|---|---|---|---:|---:|---:|---|---|
| Frais demande/analyse crédit | CAISSIER | Entrée | Oui | Non | Non | + caisse | revenus crédit |
| Frais retrait épargne | CAISSIER | Entrée | Oui | Non | Non | + caisse | revenus épargne |
| Intérêts crédit | CAISSIER/collecte | Entrée | Oui | Non | Non | + caisse | revenus crédit |
| Pénalités crédit | CAISSIER/collecte | Entrée | Oui | Non | Non | + caisse | revenus crédit |
| Vente carnet | Agent/collecte | Entrée | Oui | Non | Non | + caisse | revenus carnet, marge |
| Epargne collectée | Agent/collecte | Entrée | Non | Non | Oui | + caisse | fonds membres/flux exclu |
| Principal remboursé | CAISSIER/collecte | Entrée | Non | Non | Oui | + caisse | position crédit/flux exclu |
| Garantie bloquée | Contrôleur | Mouvement épargne | Non | Non | Oui | Pas revenu | fonds bloqués |
| Approvisionnement caisse | Admin/Chef | Entrée | Non | Non | Financement | + caisse | apports/financement |
| Décaissement crédit | CAISSIER | Sortie | Non | Non | Oui | - caisse | capital dehors |
| Retrait épargne | CAISSIER | Sortie | Non | Non | Oui | - caisse | fonds membres |
| Salaire | CAISSIER/paie | Sortie | Non | Oui | Non | - caisse | charges |
| Transport | CAISSIER | Sortie | Non | Oui | Non | - caisse | charges fixes |
| Fonctionnement | Caisse | Sortie | Non | Oui | Non | - caisse | charges |
| Achat carnets | Caisse | Sortie | Non | Oui | Non | - caisse | coût carnets |
| Remboursement apport propriétaire | CAISSIER | Sortie | Non | Non | Financement | - caisse | capital propriétaire |

## 15. Rapports et indicateurs

Rapport revenus calcule :
- Revenus réels.
- Charges connues.
- Mouvements non revenus.
- Marge carnets.
- Position crédit.
- Apports/financement.
- Fonds membres protégés.
- Trésorerie disponible prudente.
- Masse salariale.
- Transport fixe prévu.
- Contrôles de cohérence.

Point important : résultat net ≠ trésorerie disponible. Le rapport distingue revenus/charges comptables internes et flux de caisse exclus.

## 16. Contrôles internes

Contrôles observés :
- Statuts et transitions workflows.
- Session caisse ouverte requise pour actions de paiement.
- Solde caisse suffisant pour sorties.
- Solde épargne disponible/bloqué suffisant.
- Garantie 20% et gages matériels.
- Interdiction des opérations caisse libres au Caissier.
- Validation contrôleur avant paiement retrait.
- Validation Chef de Bureau avant paiement dépense.
- Alertes rapport : masse salariale, transport, positions, cohérence.

## 17. Cas d'erreur et messages utilisateur

Exemples observés :
- "Solde disponible insuffisant. Dépôt complémentaire requis..."
- "Permission TASK_COMPLETE requise"
- "Tâche non accessible dans ce périmètre"
- "La date de fin doit être supérieure ou égale à la date de début"
- "Session active déjà existante" ou équivalents service.
- "Caisse inactive" / session absente selon paiement.
- "Le commentaire est obligatoire" pour rejets/garantie.
- Erreurs frontend : chargement impossible, accès refusé, aucun élément trouvé.

## 18. Incohérences détectées

### RCI autorisé à créer opération caisse manuelle
- Module : Caisse/RBAC
- Gravité : Haute
- Description : route `/caisses/session/:sessionId/operations/nouveau` autorise `RCI`, alors que `RoleCode` décrit RCI comme audit/investigation sans validation/opération courante.
- Impact métier : confusion séparation des tâches.
- Impact sécurité : possibilité d'action financière par rôle audit si backend l'autorise.
- Recommandation : confirmer métier et aligner frontend/backend.

### Routes rapports caisse sans RoleGuard visible
- Module : Frontend routes
- Gravité : Moyenne
- Description : certaines routes `/caisses/rapports/session`, `/journalier`, `/periode` portent `data.roles` mais pas `canActivate: [RoleGuard]` dans l'inventaire.
- Impact : accès direct possible si backend ne protège pas suffisamment.
- Recommandation : ajouter/valider guard et protections backend.

### Navbar et routes non alignées
- Module : Frontend RBAC
- Gravité : Moyenne
- Description : routes autorisent certains rôles mais menu ne les affiche pas, ou inversement.
- Impact : utilisateurs bloqués par navigation ou accès direct refusé.
- Recommandation : générer matrice route/menu unique.

### Rôles legacy encore présents
- Module : Sécurité
- Gravité : Moyenne
- Description : `AGENT_BUREAU` et `RESPONSABLE` existent encore avec mapping automatique.
- Impact : migration incomplète, ambiguïté audits historiques.
- Recommandation : section compatibilité stricte et migration base.

### Modèles terrain multiples
- Module : Collecte/recette
- Gravité : Moyenne
- Description : coexistence `RecetteJournaliereTerrain`, `CollecteJournaliereTerrain`, `RecetteTerrainJournaliere`.
- Impact : risque de double comptage ou génération d'opérations depuis mauvais modèle.
- Recommandation : clarifier modèle source officiel.

### Traçabilité créateur inégale
- Module : Audit/base
- Gravité : Moyenne
- Description : certaines entités critiques n'ont pas `createdBy` explicite.
- Impact : audit incomplet.
- Recommandation : harmoniser champs audit.

### Statuts redondants ou typés incorrectement
- Module : Base
- Gravité : Faible/Moyenne
- Description : `SessionCaisse.CLOTUREE/FERMEE`; `FicheJournaliereAgentTerrain.ANNULEA`.
- Impact : confusion reporting/workflow.
- Recommandation : confirmer statut cible, migration si besoin.

### Supervision Mes actions possiblement limitée
- Module : WorkflowTask
- Gravité : Moyenne
- Description : `isVisibleToConnectedRole` filtre par rôle destinataire même pour ADMIN/COO/GERANT_GENERAL.
- Impact : supervision transversale incomplète.
- Recommandation : vérifier intention métier.

### Formules primes Agent Terrain non confirmées dans les extraits
- Module : Paie
- Gravité : Moyenne
- Description : règles officielles 1%, 2%, 200 FC non confirmées dans les services lus.
- Impact : paie/charges potentiellement incomplètes.
- Recommandation : auditer service de consolidation fiches/paie complet.

## 19. Points à confirmer métier

1. RCI peut-il créer une opération caisse manuelle ou seulement consulter/investiguer ?
2. Le MEMBER peut-il créer une demande crédit en self-service ou uniquement une demande retrait épargne ?
3. Quel modèle terrain est officiel : collecte détaillée ou recette journalière historique ?
4. `FERMEE` et `CLOTUREE` doivent-ils coexister ?
5. Les primes Agent Terrain sont-elles effectivement implémentées dans le service de paie actuel ?
6. Le Chef de Bureau doit-il voir Utilisateurs/Organisation dans la navbar ?
7. Les routes rapports caisse doivent-elles être accessibles au Caissier ou seulement lecture limitée ?
8. Les lectures de rapports sensibles doivent-elles être auditées ?
9. La capacité de retrait propriétaire est-elle une aide de gestion ou une règle officielle contraignante ?
10. Les garanties matérielles 2x sont-elles obligatoires pour tous crédits ou seulement en complément selon risque ?

## 20. Recommandations d'amélioration

1. Produire automatiquement une matrice route frontend x endpoint backend x permission depuis code.
2. Ajouter tests RBAC frontend/backend pour chaque rôle officiel.
3. Supprimer ou isoler strictement rôles legacy.
4. Clarifier le modèle unique de collecte/recette terrain.
5. Harmoniser audit : utilisateur, date/heure, antenne, site, référence métier, commentaire, action sur toutes opérations critiques.
6. Auditer les endpoints de rapport et les accès directs URL.
7. Ajouter journal d'accès aux rapports sensibles.
8. Formaliser `targetUrl` des tâches workflow et tester chaque tâche Mes actions.
9. Mettre en place un dictionnaire statuts par entité.
10. Ajouter tests métiers sur flux financiers du rapport revenus.

## 21. Annexes

### Fichiers frontend analysés

- `src/app/app.routes.ts`
- `src/app/app.config.ts`
- `src/app/core/guards/auth.guard.ts`
- `src/app/core/guards/role.guard.ts`
- `src/app/core/guards/permission.guard.ts`
- `src/app/shared/components/navbar/navbar.component.ts`
- `src/app/shared/components/dashboard/dashboard.component.ts`
- `src/app/shared/enums/permission-code.enum.ts`
- Services HTTP sous `src/app/core/services`, `src/app/shared/services`, `src/app/features`, `src/app/admin/agent-terrain/services`
- Pages récemment inspectées : rapport revenus, session caisse, écarts caisse, Mes actions

### Fichiers backend analysés

- Contrôleurs : `DemandeCreditController`, `DemandeCreditPaiementController`, `CreditController`, `GarantieCreditWorkflowController`, `OperationEpargneController`, `DemandeRetraitEpargneController`, `CaisseController`, `SessionCaisseController`, `OperationCaisseController`, `DepenseCaisseController`, `CollecteTerrainController`, `RapportRevenusController`, `WorkflowTaskController`, `EmployeController`, `RemboursementApportProprietaireController`, `AuditController`.
- Services : `DemandeCreditServiceImpl`, `CreditService`, `GarantieCreditWorkflowServiceImpl`, `OperationEpargneServiceImpl`, `DemandeRetraitEpargneServiceImpl`, `CaisseServiceImpl`, `SessionCaisseServiceImpl`, `OperationCaisseServiceImpl`, `DepenseCaisseService`, `CollecteTerrainServiceImpl`, `RapportRevenusServiceImpl`, `WorkflowTaskServiceImpl`, `EmployeServiceImpl`, `RemboursementApportProprietaireServiceImpl`, `PaiementInitialDemandeCreditServiceImpl`, `AuditService`.
- Entités : `Utilisateur`, `Role`, `Permission`, `RolePermission`, `Membre`, `CompteEpargne`, `OperationEpargne`, `DemandeRetraitEpargne`, `DemandeCredit`, `Credit`, `GarantieCredit`, `GarantieMaterielle`, `EcheanceCredit`, `RemboursementCredit`, `Caisse`, `SessionCaisse`, `OperationCaisse`, `DepenseCaisse`, `CollecteJournaliereTerrain`, `RecetteJournaliereTerrain`, `FicheJournaliereAgentTerrain`, `WorkflowTask`, `Employe`, `PaiementSalaire`, `JournalAudit`, `RemboursementApportProprietaire`, `TransportSiteParametre`.

### Limites de cette version

- Analyse statique, sans exécution de tests backend ni navigation exhaustive écran par écran dans navigateur.
- Certaines règles profondes dans mappers, repositories, handlers d'erreurs ou services non listés peuvent compléter ce document.
- Les permissions exactes par endpoint dépendent aussi de la configuration Spring Security globale et des permissions en base.
- Les documents métier historiques du workspace n'ont pas tous été relus intégralement; les règles officielles marquées comme telles proviennent de la demande utilisateur et des commentaires explicites du code.

## 22. Matrices de synthèse obligatoires

### A. Rôles x modules

Voir section 4 pour la matrice détaillée. Les droits exacts doivent être confrontés à la base `role_permissions` en environnement actif.

### B. Rôles x workflows

| Workflow | Etape | Rôle responsable | Action | Statut avant | Statut après | Tâche Mes actions | Endpoint |
|---|---|---|---|---|---|---|---|
| Crédit | Pré-analyse | GESTIONNAIRE | Pré-analyser | SOUMISE | EN_ANALYSE déduit | Oui | `/api/demandes-credit/{id}/pre-analyse` |
| Crédit | Analyse | CONTROLEUR | Valider analyse risque | EN_ANALYSE | ANALYSE_TERRAIN_VALIDEE déduit | Oui | `/api/demandes-credit/{id}/analyse-risque/valider` |
| Crédit | Garantie | CONTROLEUR | Contrôler/valider garantie | ANALYSE_TERRAIN_VALIDEE | VALIDATION_CHEF déduit | Oui | `/api/demandes-credit/{id}/controle-garantie` |
| Crédit | Approbation | CHEF_BUREAU | Approuver | VALIDATION_CHEF | APPROUVEE | Oui | `/api/credits/demande/{id}/approbation` |
| Crédit | Décaissement | CAISSIER | Décaisser | APPROUVE | DECAISSE/EN_COURS | Oui | `/api/credits/{id}/decaissement` |
| Retrait épargne | Validation | CONTROLEUR | Valider/rejeter | CREEE/EN_ATTENTE_VALIDATION | VALIDEE/REJETEE | Oui | `/api/demandes-retrait-epargne/{id}/valider` |
| Retrait épargne | Paiement | CAISSIER | Décaisser | VALIDEE | DECAISSEE | Oui | `/api/demandes-retrait-epargne/{id}/decaisser` |
| Collecte | Billetage | CAISSIER | Confirmer billetage | SOUMISE | SOUMISE billetée | Oui | `/api/collectes-terrain/{id}/billetage/confirmer` |
| Collecte | Contrôle | CONTROLEUR | Valider/rejeter | SOUMISE billetée | VALIDEE/REJETEE | Oui | `/api/collectes-terrain/{id}/valider` |
| Caisse | Pré-clôture | CAISSIER | Soumettre contrôle | OUVERTE | PRE_CLOTUREE | Oui | `/api/caisses/sessions/{id}/pre-cloturer` |
| Caisse | Contrôle | CONTROLEUR | Valider contrôle | PRE_CLOTUREE | VALIDEE_CONTROLE | Oui | validation contrôle session |
| Caisse | Clôture finale | CHEF_BUREAU | Clôturer | VALIDEE_CONTROLE | CLOTUREE/FERMEE | Oui | clôture finale session |
| Dépense | Validation | CHEF_BUREAU | Valider/rejeter | EN_ATTENTE_VALIDATION | VALIDEE/REJETEE | Oui | `/api/depenses-caisse/{id}/valider` |
| Dépense | Paiement | CAISSIER | Payer | VALIDEE | PAYEE | Oui | `/api/depenses-caisse/{id}/payer` |

### C. Endpoints x permissions

Voir section 9. Pour audit formel, exporter aussi la table `role_permissions` de la base active.

### D. Flux financiers

Voir section 14.

### E. Matrice statuts

| Entité | Statut | Signification | Rôle avançant | Prochain statut possible | Conditions |
|---|---|---|---|---|---|
| DemandeCredit | SOUMISE | Attend pré-analyse | GESTIONNAIRE | EN_ANALYSE / rejet | Dossier complet |
| DemandeCredit | EN_ANALYSE | Attend analyse contrôleur | CONTROLEUR | ANALYSE_TERRAIN_VALIDEE / rejet | Pré-analyse validée |
| DemandeCredit | ANALYSE_TERRAIN_VALIDEE | Attend garantie | CONTROLEUR | VALIDATION_CHEF / rejet | Analyse validée |
| DemandeCredit | VALIDATION_CHEF | Attend décision | CHEF_BUREAU | APPROUVEE / REJETEE | Garantie conforme |
| Credit | APPROUVE | Attend décaissement | CAISSIER | DECAISSE/EN_COURS | Session caisse, solde |
| Credit | EN_COURS | Crédit actif | CAISSIER/Membre | REMBOURSE/EN_RETARD | Remboursements |
| DemandeRetraitEpargne | CREEE | Demande créée | CONTROLEUR | VALIDEE/REJETEE | Solde disponible |
| DemandeRetraitEpargne | VALIDEE | Attend paiement | CAISSIER | DECAISSEE | Session caisse |
| CollecteJournaliereTerrain | BROUILLON | Saisie agent | AGENT_TERRAIN | SOUMISE | Lignes valides |
| CollecteJournaliereTerrain | SOUMISE | Attend billetage/contrôle | CAISSIER/CONTROLEUR | VALIDEE/REJETEE | Billetage confirmé |
| SessionCaisse | OUVERTE | Session active | CAISSIER | PRE_CLOTUREE | Contrôle mouvements |
| SessionCaisse | PRE_CLOTUREE | Attend contrôleur | CONTROLEUR | VALIDEE_CONTROLE | Solde/écarts analysés |
| SessionCaisse | VALIDEE_CONTROLE | Attend clôture | CHEF_BUREAU | CLOTUREE/FERMEE | Contrôle validé |
| DepenseCaisse | EN_ATTENTE_VALIDATION | Dépense soumise | CHEF_BUREAU | VALIDEE/REJETEE | Motif/pièces |
| DepenseCaisse | VALIDEE | Attend paiement | CAISSIER | PAYEE | Session/solde |
| WorkflowTask | A_FAIRE | Action assignée | Rôle destinataire | EN_COURS/TERMINEE | Visibilité RBAC |
| WorkflowTask | EN_COURS | Vue/prise en charge | Rôle destinataire | TERMINEE | Action réalisée |

## 23. Conclusion

L'application Mini-Crédit 3N possède déjà une architecture métier riche, avec séparation des tâches, workflow task, caisse contrôlée, garanties crédit, rapports financiers différenciant revenus/charges/flux exclus, et audit partiel. Les risques principaux ne sont pas des absences massives de règles, mais des désalignements entre frontend, backend, rôles officiels, rôles legacy, menus et règles d'audit.

La priorité d'audit recommandée est :
1. RBAC route/menu/endpoint.
2. Workflow crédit et Mes actions.
3. Caisse et opérations libres.
4. Rapport revenus et classification des flux.
5. Traçabilité complète des opérations financières.
6. Migration/neutralisation des rôles legacy.
