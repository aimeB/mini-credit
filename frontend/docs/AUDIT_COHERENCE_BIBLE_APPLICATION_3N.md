# Audit de cohérence renforcé - Bible Mini-Crédit 3N

Date : 2026-07-23

Livrable lié : `docs/BIBLE_APPLICATION_MINI_CREDIT_3N.md`

Objet : ajouter une couche d'audit avec preuves code exactes pour les règles, actions, routes, endpoints, permissions et incohérences citées dans la Bible. Ce document ne modifie pas le code. Il documente l'état actuel observé.

## 1. Méthode et conventions

### Sources analysées

Frontend Angular :
- `src/app/app.routes.ts`
- `src/app/shared/components/navbar/navbar.component.ts`
- `src/app/features/credit/**`
- `src/app/features/recettes/pages/collecte-du-jour/collecte-du-jour.component.ts`
- `src/app/features/rapports/pages/rapport-revenus/rapport-revenus.component.ts`
- `src/app/features/caisse/**`
- `src/app/features/workflow/pages/mes-actions/**`

Backend Spring Boot :
- `C:\Users\admin\Documents\mini-credit\src\main\java\com\mini\credit\controller\**`
- `C:\Users\admin\Documents\mini-credit\src\main\java\com\mini\credit\service\impl\**`
- `C:\Users\admin\Documents\mini-credit\src\main\java\com\mini\credit\entity\**`
- `C:\Users\admin\Documents\mini-credit\src\main\java\com\mini\credit\enums\**`
- `C:\Users\admin\Documents\mini-credit\src\main\java\com\mini\credit\service\security\ScopeService.java`

Les lignes sont approximatives car le code évolue, mais elles correspondent aux recherches et lectures effectuées durant cet audit.

### Niveaux de preuve

- Preuve directe : route, annotation, méthode, constante ou condition visible dans le code.
- Preuve déduite : combinaison de route frontend + service backend + workflow task.
- Point à confirmer : règle métier attendue mais non confirmée dans les extraits analysés.

## 2. Priorité 1 - Crédit complet

### Règle auditée : workflow crédit complet

- Règle attendue : Demande -> Pré-analyse -> Analyse -> Garantie -> Approbation -> Décaissement -> Remboursement -> Clôture.
- Rôle concerné : GESTIONNAIRE, CONTROLEUR, CHEF_BUREAU, CAISSIER.
- Preuve backend : `WorkflowTaskServiceImpl`.
  - `onDemandeCreditSoumise`, lignes approx. 398-414 : crée `TRAITER_DEMANDE_CREDIT` pour `RoleCode.GESTIONNAIRE`.
  - `onDemandeCreditPreAnalyseValidee`, lignes approx. 418-442 : clôture tâche gestionnaire et crée `CONTROLER_DEMANDE_CREDIT` pour `CONTROLEUR`.
  - `onDemandeCreditAnalyseTerrainValidee`, lignes approx. 445-469 : clôture analyse risque et crée contrôle garantie pour `CONTROLEUR`.
  - `onDemandeCreditValidationChef`, lignes approx. 472-496 : crée `APPROUVER_DEMANDE_CREDIT` pour `CHEF_BUREAU`.
  - `onDemandeCreditApprouvee`, lignes approx. 499-523 : crée `DECAISSER_CREDIT` pour `CAISSIER`.
  - `onCreditDecaisse`, lignes approx. 526-533 : clôture tâche caissier.
- Preuve frontend : `src/app/app.routes.ts`.
  - `/credits/demandes`, ligne approx. 466 : `ADMIN`, `CHEF_BUREAU`, `GESTIONNAIRE`, `CONTROLEUR`.
  - `/credits/frais-a-encaisser`, ligne approx. 472 : `CAISSIER`, `ADMIN`, `CHEF_BUREAU`, `RCI`.
  - `/credits/demandes/:id/garantie`, ligne approx. 515 : `ADMIN`, `CONTROLEUR`, `CHEF_BUREAU`, `CAISSIER`, `RCI`.
  - `/credits/:creditId/decaissement`, ligne approx. 545 : `ADMIN`, `CHEF_BUREAU`, `GESTIONNAIRE`, `CAISSIER` route pointant sur composant liste/décaissement.
- Impact métier : le chemin métier principal est matérialisé par les tâches workflow et les routes.
- Impact sécurité : le frontend limite globalement les rôles par étape, mais certaines routes donnent une lecture ou un accès large (ex. garantie visible à CAISSIER/RCI alors que les mutations backend sont contrôleur/admin).
- Impact contrôle interne : séparation des tâches globalement respectée.
- Impact audit : les transitions sont traçables via `WorkflowTask` et certains audits métier, mais les statuts exacts avant/après doivent être vérifiés dans `DemandeCreditServiceImpl`.
- Recommandation : créer un test end-to-end métier crédit par rôle et statut, plus une matrice statuts réelle extraite des enums/services.

### Règle auditée : frais demande crédit encaissés par Caissier uniquement

- Règle attendue : le Caissier encaisse les frais de demande crédit via écran dédié; ce paiement crée une entrée caisse et ne concerne pas la garantie.
- Preuve frontend :
  - `src/app/app.routes.ts`, ligne approx. 472 : route `/credits/frais-a-encaisser` autorise `CAISSIER`, mais aussi `ADMIN`, `CHEF_BUREAU`, `RCI`.
  - `PaiementInitialDemandeCreditFormComponent`, lignes approx. 49-65 : statuts autorisés côté UI et champ `fraisPayes` requis.
  - `PaiementInitialDemandeCreditFormComponent`, lignes approx. 147-171 : payload avec `fraisPayes`, validation montant > 0, montant <= reste.
  - `PaiementInitialDemandeCreditFormComponent`, lignes approx. 219-223 : `fraisDemandeRestant = fraisDemande - fraisDemandePayes`.
- Preuve backend : `PaiementInitialDemandeCreditServiceImpl`.
  - `requireCurrentCaissier`, lignes approx. 236-244 : rôle exact `RoleCode.CAISSIER` requis.
  - `STATUTS_FRAIS_DEMANDE_AUTORISES`, lignes approx. 50-58 : statuts `SOUMISE`, `EN_ANALYSE`, `ANALYSE_TERRAIN_VALIDEE`, `VALIDATION_CHEF`, `VALIDATION_CONTROLEUR`, `APPROUVEE`.
  - `validatePaiementInitial`, lignes approx. 196-229 : frais > 0, dépôt garantie doit être 0, interdit de dépasser le reste.
  - `enregistrerPaiementInitial`, lignes approx. 103-150 : incrémente `fraisDemandePayes`, crée `OperationCaisse` entrée `FRAIS_DEMANDE_CREDIT`, `ReferenceMetier=DEMANDE_CREDIT:id`.
- Rôle concerné : CAISSIER.
- Impact métier : le montant saisi est conservé et cumulé, dans la limite du reste à payer.
- Impact sécurité : backend strict; frontend affiche la route à des rôles plus larges que le backend. Ceux-ci devraient être rejetés par l'endpoint s'ils tentent l'action.
- Impact contrôle interne : bon découplage garantie/frais; le dépôt garantie est explicitement refusé sur cet écran.
- Impact audit : `@Auditable(PAIEMENT_INITIAL_DEMANDE_CREATED)` et opération caisse avec référence métier.
- Recommandation : aligner la route frontend sur `CAISSIER` si l'écran n'est pas destiné à la consultation par `ADMIN/CHEF_BUREAU/RCI`, ou rendre les boutons d'action conditionnels.

### Règle auditée : durée crédit saisie conservée

- Règle attendue : la durée saisie (`dureeValeur`, `dureeUnite`) doit être transmise depuis la collecte ou la demande vers le dossier crédit.
- Preuve frontend collecte : `collecte-du-jour.component.ts`.
  - lignes approx. 105-106 : champs formulaire `dureeValeur`, `dureeUnite` initialisés.
  - lignes approx. 715-716 : validators `required`, min 1, max 60 et unité requise pour `DEMANDE_CREDIT`.
  - lignes approx. 737-760 : `buildLinePayload()` transmet `dureeValeur` et `dureeUnite`.
- Preuve backend collecte : `CollecteTerrainServiceImpl`.
  - lignes approx. 157-161 : `addLigne` mappe `request.getDureeValeur()` et `request.getDureeUnite()`.
  - lignes approx. 185-191 : `updateLigne` mappe les mêmes champs.
  - mémoire de session précédente : `buildDemandeCreditFromCollecteLine()` utilise `resolveDureeValeur(ligne)` et `resolveDureeUnite(ligne)`.
- Preuve frontend crédit : modèles `credit-response.ts`, `credit-detail-response.ts`, `credit-contrat-response.ts` portent `dureeValeur`/`dureeUnite`; composants affichent la durée.
- Impact métier : correction importante; évite le défaut 1 mois écrasant la saisie.
- Impact sécurité : faible.
- Impact contrôle interne : fiabilité du contrat et échéancier.
- Impact audit : durée contractuelle vérifiable dans dossier.
- Recommandation : ajouter test backend de transformation collecte -> demande crédit avec durée non standard (ex. 3 semaines, 6 mois).

### Règle auditée : garantie 20% + gage matériel 2x

- Règle attendue : garantie épargne obligatoire 20%; gage matériel complémentaire si présent; valeur matérielle acceptée >= 2 x montant demandé si règle présente.
- Preuve backend : `GarantieCreditWorkflowServiceImpl`.
  - lignes approx. 57-58 : `TAUX_GARANTIE_3N = 20`, `COEFFICIENT_MIN_GAGE_MATERIEL = 2`.
  - lignes approx. 72-104 : `verifier()` calcule garantie requise, disponible, déjà bloqué, manquant.
  - lignes approx. 107-172 : `bloquerEpargne()` transfère disponible vers bloqué, crée `OperationEpargne` `BLOCAGE_GARANTIE`.
  - lignes approx. 177-205 : `ajouterGarantieMaterielle()` crée gage matériel.
  - lignes approx. 229-240 : `valider()` appelle `verifierGarantieSuffisante()` et `verifierGarantiesMaterielles()` avant statut `VALIDEE`.
- Preuve frontend : `garantie-credit-page.component.ts`.
  - lignes approx. 65-72 : calcule `montantDemande * 20 / 100` si le backend ne renvoie pas le requis.
  - lignes approx. 96-113 : vérifie bloqué >= requis et solde disponible >= requis.
  - ligne approx. 135 : seuil gage = `montantDemande * 2`.
- Impact métier : règle 3N confirmée dans backend et UI.
- Impact sécurité : le backend est la source de vérité; UI cohérente.
- Impact contrôle interne : fort, sécurise le risque crédit.
- Impact audit : opérations d'audit `GARANTIE_VERIFIED`, `GARANTIE_BLOCKED`, `GARANTIE_MATERIAL_*`, `GARANTIE_VALIDATED/REJECTED`.
- Recommandation : vérifier le cas métier où aucun gage matériel n'est requis; documenter la condition exacte dans `verifierGarantiesMaterielles()`.

## 3. Priorité 2 - Mes actions

### Règle auditée : création et fermeture des tâches workflow

- Preuve backend : `WorkflowTaskServiceImpl`.
  - lignes approx. 72-77 : types d'entités (`SESSION_CAISSE`, `DEMANDE_CREDIT`, `CREDIT`, `RETRAIT_EPARGNE`, etc.).
  - lignes approx. 748-814 : `upsertRoleTask()` crée une tâche `A_FAIRE`, exige `antenneId`, construit `activeKey`, évite doublon actif.
  - lignes approx. 839-880 : `closeTasks()` passe `TERMINEE`, renseigne `completedBy`, `completedAt`, commentaire, vide `activeKey`.
  - lignes approx. 1512-1525 : `resolveEffectiveRole()` mappe legacy vers rôle officiel.
- Preuve frontend :
  - `app.routes.ts`, lignes approx. 91-105 : route `/mes-actions`, `RoleGuard + PermissionGuard`, rôles autorisés, permissions `TASK_*`, bypass ADMIN.
  - `navbar.component.ts`, lignes approx. 66/139 : lien `Actions` affiché si `canShowTaskArea()`.
  - `navbar.component.ts`, lignes approx. 484-494 : `canShowTaskArea()` vérifie rôles et charge badge.
- Impact métier : les utilisateurs voient des actions par rôle et périmètre.
- Impact sécurité : dépend fortement des permissions `TASK_*` et du filtrage `isVisibleToConnectedRole`.
- Impact contrôle interne : centralise les validations à faire.
- Impact audit : tâche créée/terminée auditable.
- Recommandation : documenter `targetUrl` réel par type action et tester chaque tâche.

### Point obligatoire : Chef de Bureau accès depuis Mes actions

- Règle attendue : Chef de Bureau doit accéder aux tâches d'approbation crédit, validation dépense et clôture session.
- Preuve frontend : `app.routes.ts`, ligne approx. 97 : `CHEF_BUREAU` autorisé sur `/mes-actions`.
- Preuve backend :
  - `WorkflowTaskServiceImpl`, lignes approx. 472-496 : tâche `APPROUVER_DEMANDE_CREDIT` pour `CHEF_BUREAU`.
  - lignes approx. 262-276 : tâche `CLOTURER_SESSION_CAISSE` pour `CHEF_BUREAU` après contrôle.
  - lignes approx. 966-984 : tâche `DEPENSE_CAISSE_VALIDATE` pour `CHEF_BUREAU`.
- Impact métier : accès nécessaire confirmé.
- Risque : si `antenneId` est null, `upsertRoleTask` ignore la tâche; Chef de Bureau peut ne rien voir malgré dossiers existants.
- Recommandation : test métier Chef Bureau avec antenne renseignée et non renseignée.

### Incohérence : supervision Mes actions pour ADMIN/COO/Gérant Général

- ID incohérence : INC-008
- Titre : supervision Mes actions possiblement limitée aux tâches du même rôle
- Module : WorkflowTask / RBAC
- Gravité : Moyenne à haute
- Règle attendue : ADMIN, COO et GERANT_GENERAL devraient superviser transversalement les tâches si permission `TASK_SUPERVISE` ou `TASK_AUDIT`.
- Fonctionnement actuel observé : `findVisibleTasks()` peut charger globalement les tâches pour ADMIN/COO/GERANT_GENERAL, mais `isVisibleToConnectedRole()` retourne ensuite seulement les tâches dont `roleDestinataire == roleCode`.
- Preuve backend :
  - `WorkflowTaskServiceImpl`, lignes approx. 650-700 : branche `canSupervise` charge globalement pour ADMIN/COO/GERANT_GENERAL.
  - `WorkflowTaskServiceImpl`, lignes approx. 726-739 : `isVisibleToConnectedRole()` retourne `task.getRoleDestinataire() == roleCode`, même pour ADMIN/COO/GERANT_GENERAL.
- Preuve frontend : `app.routes.ts`, lignes approx. 91-105 : `/mes-actions` autorise ces rôles si permissions.
- Impact métier : superviseur peut ne voir aucune tâche si aucune tâche ne lui est directement destinée.
- Impact sécurité : réduction de visibilité plutôt qu'excès, mais peut masquer des alertes.
- Impact contrôle interne : supervision transverse incomplète.
- Impact audit : audit des tâches non consultables par la hiérarchie.
- Impact base de données : tâches existantes non visibles malgré permissions.
- Impact frontend : écran vide ou incomplet.
- Impact backend : logique de filtrage à clarifier.
- Correction recommandée : pour `TASK_SUPERVISE`, autoriser lecture des rôles subordonnés/périmètre; pour `TASK_AUDIT`, lecture seule globale ou caisse selon RCI.
- Tests métier à prévoir : ADMIN voit toutes tâches; COO voit tâches opérationnelles; GERANT_GENERAL voit tâches de gouvernance; RCI voit tâches caisse/audit selon règle.

## 4. Priorité 3 - Caisse

### Point obligatoire : RCI peut-il créer opération caisse manuelle ?

- ID incohérence : INC-001
- Titre : RCI autorisé à créer une opération caisse libre
- Module : Caisse / RBAC
- Gravité : Haute
- Règle attendue : RCI est un rôle d'audit/contrôle interne, sans opération financière courante.
- Fonctionnement actuel observé : frontend autorise RCI sur la route de saisie manuelle; backend autorise RCI dans `validerOperationLibreAutorisee` pour entrée/sortie libre.
- Preuve frontend :
  - `app.routes.ts`, lignes approx. 346-351 : route `/caisses/session/:sessionId/operations/nouveau`, rôles `ADMIN`, `CHEF_BUREAU`, `RCI`.
- Preuve backend :
  - `OperationCaisseServiceImpl`, lignes approx. 407-437 : `validerOperationLibreAutorisee()` interdit CAISSIER, puis réserve les opérations libres à `ADMIN`, `CHEF_BUREAU` ou `RCI`.
  - `OperationCaisseServiceImpl`, lignes approx. 520-545 : `verifierAccesCaisse()` autorise `ADMIN`, `CONTROLEUR`, `CHEF_BUREAU`, `RCI` à accéder à la caisse.
  - `RoleCode.java`, commentaires RCI lignes approx. 50-75 : RCI ne valide pas opérations courantes et n'administre pas.
- Impact métier : séparation entre audit et opération financière rompue.
- Impact sécurité : un rôle audit peut potentiellement générer une entrée/sortie caisse libre.
- Impact contrôle interne : conflit d'intérêt; l'auditeur peut créer l'objet qu'il contrôle.
- Impact audit : opération créée par RCI serait tracée, mais la règle métier est discutable.
- Impact base de données : `operation_caisse` peut contenir des opérations libres créées par RCI.
- Impact frontend : bouton/page accessible via URL et route.
- Impact backend : contrôle autorise explicitement RCI.
- Correction recommandée : retirer `RCI` des opérations libres backend et de la route; conserver lecture/audit écarts/rapports.
- Tests métier à prévoir : RCI peut consulter journal/écarts; RCI reçoit 403/BusinessException sur création opération libre; ADMIN/CHEF_BUREAU restent autorisés si règle validée.

### Règle auditée : Caissier opérations libres interdites

- Preuve backend : `OperationCaisseServiceImpl`, lignes approx. 429-431 : exception explicite "Entrée/sortie libre interdite au CAISSIER...".
- Preuve frontend : `app.routes.ts`, lignes approx. 346-351 : route création manuelle n'inclut pas CAISSIER.
- Impact métier : Caissier doit passer par workflows validés.
- Impact contrôle interne : règle solide côté frontend et backend.
- Recommandation : conserver et tester.

### Point obligatoire : routes rapports caisse sans RoleGuard

- ID incohérence : INC-002
- Titre : routes rapports caisse avec `data.roles` sans `RoleGuard`
- Module : Frontend routes / RBAC
- Gravité : Moyenne
- Règle attendue : toute route avec restrictions de rôles doit utiliser `RoleGuard`.
- Fonctionnement actuel observé : les routes `/caisses/rapports/session`, `/journalier`, `/periode` définissent `data.roles`, mais l'inventaire ne montre pas `canActivate: [RoleGuard]` sur ces trois entrées.
- Preuve frontend :
  - `app.routes.ts`, lignes approx. 389-407 : routes rapports session/journalier/période avec `data.roles`.
  - Absence observée de `canActivate: [RoleGuard]` dans ces entrées, contrairement à `/caisses/rapports/depenses` lignes approx. 409-415 et `/caisses/rapports/ecarts` lignes approx. 417-423.
- Preuve backend : non tranchée dans cet audit; les endpoints rapports caisse doivent être vérifiés côté controllers.
- Impact métier : accès direct possible par utilisateur authentifié si backend ne compense pas.
- Impact sécurité : risque de contournement frontend.
- Impact contrôle interne : visibilité de rapports caisse sensible non maîtrisée côté UI.
- Impact audit : accès rapports potentiellement non audité.
- Impact base de données : lecture uniquement.
- Impact frontend : `data.roles` donne une fausse impression de protection.
- Impact backend : dépend de la protection endpoint.
- Correction recommandée : ajouter `canActivate: [RoleGuard]` ou `RoleGuard + PermissionGuard`; vérifier `@PreAuthorize` backend.
- Tests métier à prévoir : utilisateur non autorisé authentifié tape l'URL; doit être refusé frontend et backend.

## 5. Priorité 4 - Collecte terrain

### Règle auditée : collecte terrain -> billetage -> contrôle -> validation

- Preuve backend : `CollecteTerrainServiceImpl`.
  - lignes approx. 121-150 : création collecte brouillon, antenne obligatoire, `createdBy` Long.
  - lignes approx. 225-253 : `soumettre()` exige lignes, observation si écart, statut `SOUMISE`, appelle `workflowTaskService.onCollecteSoumise`.
  - lignes approx. 257-315 : `confirmerBilletage()` exige rôle `CAISSIER`, statut `SOUMISE`, périmètre antenne, trace audit, appelle `onCollecteBilletageConfirme`.
  - lignes approx. 319-340+ : `valider()` exige `assertCanValidate`, contrôle périmètre contrôleur, statut `SOUMISE`.
- Preuve frontend :
  - `app.routes.ts`, lignes approx. 229-247 : routes agent `/collectes/ma-collecte`, caissier `/collectes/soumises-billetage`, contrôleur `/collectes/a-controler`.
  - `collecte-du-jour.component.ts`, lignes approx. 90-91 : cache `DEMANDE_CREDIT` si crédit bloquant.
  - lignes approx. 335-365 : charge crédits bloquants et invalide le type sélectionné si besoin.
- Impact métier : workflow terrain bien matérialisé.
- Impact sécurité : rôle et antenne contrôlés côté backend pour billetage/validation.
- Impact contrôle interne : billetage séparé de l'agent, contrôle séparé du caissier.
- Impact audit : billetage trace user, antenne, date, commentaire; validation trace à vérifier plus loin dans service.
- Recommandation : auditer génération automatique opérations pour vérifier absence de double comptage.

### Point obligatoire : modèles collecte/recette multiples

- ID incohérence : INC-005
- Titre : coexistence de plusieurs modèles terrain/recette
- Module : Collecte terrain / Recettes
- Gravité : Moyenne
- Règle attendue : un modèle source officiel de recette/collecte doit être identifié.
- Fonctionnement actuel observé : le backend contient `CollecteJournaliereTerrain`, `CollecteMembreLigne`, `RecetteJournaliereTerrain`, `FicheJournaliereAgentTerrain`, et possiblement `RecetteTerrainJournaliere`.
- Preuve backend :
  - `CollecteTerrainServiceImpl`, lignes approx. 1-80 : service moderne utilise `CollecteJournaliereTerrain`, `CollecteMembreLigne`, `CollecteOperationGeneree`.
  - `WorkflowTaskServiceImpl`, lignes approx. 289-392 : tâches distinctes pour `RECETTE_TERRAIN` et `COLLECTE_TERRAIN`.
  - Entités listées dans la Bible : `RecetteJournaliereTerrain`, `CollecteJournaliereTerrain`, `RecetteTerrainJournaliere`, `FicheJournaliereAgentTerrain`.
- Preuve frontend : routes `/recettes/**` et `/collectes/**` coexistent dans `app.routes.ts`, lignes approx. 190-266.
- Impact métier : risque de confusion entre ancienne recette et collecte détaillée.
- Impact sécurité : règles de rôles peuvent diverger entre modèles.
- Impact contrôle interne : risque de double validation ou génération d'opérations depuis mauvais modèle.
- Impact audit : traces réparties sur plusieurs entités.
- Impact base de données : tables multiples similaires.
- Impact frontend : deux familles d'écrans.
- Impact backend : services et tâches séparés.
- Correction recommandée : décider modèle officiel, marquer legacy les autres, verrouiller génération caisse sur une seule source.
- Tests métier à prévoir : une collecte validée ne génère qu'une série d'opérations; une recette legacy ne double pas le rapport.

## 6. Priorité 5 - Retraits épargne

### Règle auditée : demande -> validation contrôleur -> paiement caissier

- Preuve backend : `WorkflowTaskServiceImpl`.
  - lignes approx. 539-556 : `onRetraitDemande()` crée `VALIDER_RETRAIT_EPARGNE` pour `CONTROLEUR`.
  - lignes approx. 559-577 : `onRetraitApprouve()` clôture contrôleur et crée `PAYER_RETRAIT_EPARGNE` pour `CAISSIER`.
  - lignes approx. 580-599 : paiement/rejet clôturent les tâches.
  - lignes approx. 1016-1049 : synchronisation retrouve `CREEE`, `EN_ATTENTE_VALIDATION`, `VALIDEE` selon rôle.
- Preuve frontend : `app.routes.ts`.
  - lignes approx. 153-185 : routes demandes retrait, création MEMBER/CAISSIER/ADMIN, détail ADMIN/CHEF_BUREAU/CONTROLEUR/CAISSIER/MEMBER.
  - navbar `Retraits épargne`, lignes approx. 222-230 : menu caissier vers demandes validées.
- Impact métier : workflow officiel respecté.
- Impact sécurité : paiement réservé par tâche/rôle; endpoints backend à confirmer sur annotations.
- Impact contrôle interne : séparation validation/paiement.
- Impact audit : audits retrait et opération caisse attendus.
- Recommandation : renforcer traçabilité créateur.

### Point obligatoire : createdBy incomplet sur demande retrait

- ID incohérence : INC-006
- Titre : traçabilité créateur incomplète sur certaines entités
- Module : Audit / Base
- Gravité : Moyenne
- Règle attendue : toute opération doit tracer utilisateur, date, heure, antenne, commentaire, référence métier, action.
- Fonctionnement actuel observé : certaines entités ont `createdBy`, d'autres seulement des champs partiels. `DemandeRetraitEpargne` a `validePar`, mais l'inventaire entité n'a pas confirmé `createdBy/demandePar`.
- Preuve backend :
  - `CollecteTerrainServiceImpl`, lignes approx. 139-145 : `CollecteJournaliereTerrain.createdBy` stocke l'ID utilisateur en `Long`.
  - `OperationCaisseServiceImpl`, lignes approx. 139-164 : `OperationCaisse` enregistre `createdBy`, `utilisateur`, `roleUtilisateur`, `site`, `referenceMetier`.
  - Entité `DemandeRetraitEpargne` observée dans inventaire : relation `validePar`, mais pas créateur explicite confirmé.
  - `TransportSiteParametre` inventaire : `createdBy/updatedBy` en `Long`, pas relation JPA.
- Preuve frontend : formulaires permettent observation/commentaire selon écrans, mais ne garantissent pas tous les champs audit.
- Impact métier : responsabilité initiale parfois difficile à prouver.
- Impact sécurité : faible à moyen, mais gêne l'imputabilité.
- Impact contrôle interne : suivi incomplet de la chaîne de validation.
- Impact audit : non-conformité potentielle avec règle officielle de traçabilité.
- Impact base de données : relations audit hétérogènes.
- Impact frontend : affichage audit incomplet.
- Impact backend : services doivent reconstruire l'acteur via SecurityContext ou logs.
- Correction recommandée : standardiser `createdBy`, `createdAt`, `updatedBy`, `antenneId`, `siteId`, `referenceMetier`, `commentaireAudit` sur entités critiques.
- Tests métier à prévoir : créer demande retrait, vérifier auteur/date/antenne/référence en DB et audit log.

## 7. Priorité 6 - Paie / transport

### Point obligatoire : primes Agent Terrain réellement appliquées

- ID incohérence : INC-009
- Titre : formules primes Agent Terrain non confirmées comme appliquées automatiquement
- Module : Personnel / Paie
- Gravité : Moyenne
- Règle attendue : Agent Terrain peut avoir primes automatiques 1% épargne validée, 2% remboursements crédit validés, 200 FC par carnet vendu.
- Fonctionnement actuel observé : le rapport paie sait lire primes/bonus/commissions/régularisations et produire alertes, mais les extraits analysés ne confirment pas le calcul automatique exact 1%/2%/200 FC.
- Preuve backend :
  - `RapportRevenusServiceImpl`, lignes approx. 1200-1230 : `PayrollBreakdown` contient `primes`, `commissions`, `regularisations`.
  - lignes approx. 1232-1305 : `buildPaieControls()` compare payé vs rémunération attendue et signale prime/bonus.
  - lignes approx. 1320-1365 : `buildTransportControls()` traite transport séparément.
  - Recherche backend : mentions `primeMobilisationEpargne`, primes/bonus dans `DepenseCaisse`, mais pas preuve suffisante des constantes 1%, 2%, 200 FC dans les extraits.
- Preuve frontend : rapport revenus affiche détail masse salariale enrichi; pas preuve du calcul automatique.
- Impact métier : possible écart entre règle officielle et paie calculée.
- Impact sécurité : faible.
- Impact contrôle interne : risque de paiement incorrect ou non justifié.
- Impact audit : justification primes incomplète.
- Impact base de données : champs primes présents mais origine de calcul à confirmer.
- Impact frontend : alertes peuvent signaler primes sans prouver le calcul.
- Impact backend : service de consolidation paie à auditer plus profondément.
- Correction recommandée : localiser ou créer un service unique de calcul primes Agent Terrain; documenter constantes paramétrables; tester 1%/2%/200 FC.
- Tests métier à prévoir : collecte validée avec épargne, remboursement, carnets -> fiche paie agent -> rapport revenus.

### Règle auditée : transport Agent Terrain séparé du salaire

- Preuve backend :
  - `RapportRevenusServiceImpl`, lignes approx. 1320-1365 : `buildTransportControls()` séparé de `buildPaieControls()`.
  - `TransportSiteParametreServiceImpl`, lignes approx. 80-95 dans recherche : DTO expose montant journalier et `createdBy`.
  - `RapportRevenusServiceImpl`, lignes approx. 1370-1415 : accumulateur site transport calcule prévu/payé/restant.
- Impact métier : règle séparée confirmée au niveau rapport/contrôle.
- Recommandation : vérifier paiement transport via `DepenseCaisse` et ses statuts.

## 8. Priorité 7 - Rapports financiers

### Règle auditée : revenus vs charges vs flux exclus

- Preuve backend : `RapportRevenusServiceImpl`.
  - lignes approx. 96-111 : catégories revenus `FRAIS_ANALYSE_CREDIT`, `FRAIS_RETRAIT_EPARGNE`, `INTERETS_CREDIT`, `PENALITES_CREDIT`, `CARNETS_VENDUS`.
  - lignes approx. 260-300 : `addOperationCaisseRevenus()` lit catégories caisse de revenus : `FRAIS_DEMANDE_CREDIT`, `FRAIS_DEMANDE`, `FRAIS_RETRAIT_EPARGNE`, `ENTREE_DIVERSE`.
  - lignes approx. 306-360 : `addCreditRevenus()` ajoute intérêts et pénalités, pas principal.
  - lignes approx. 380+ : `addCollecteLineRevenus()` ajoute carnets/frais selon lignes collecte.
  - lignes approx. 200-230 : `buildCharges()`, `buildMouvementsNonRevenus()`, position crédit, apports, fonds membres et trésorerie sont calculés séparément.
  - lignes approx. 1420-1460 : capacité retrait propriétaire = aide de gestion, avec commentaire explicite "pas une règle métier 3N officielle".
- Preuve frontend : `rapport-revenus.component.ts` documente/affiche onglets revenus, crédit/trésorerie, financement, audit; récente refonte regroupe flux exclus séparément.
- Impact métier : classification financière alignée avec règles officielles demandées.
- Impact sécurité : lecture rapports réservée par route à rôles de supervision.
- Impact contrôle interne : réduit le risque de confondre résultat et trésorerie.
- Impact audit : rapport expose détails, contrôles et incohérences.
- Recommandation : ajouter tests automatisés de classification pour chaque catégorie caisse.

### Point obligatoire : frais demande crédit montant saisi conservé

- Voir section Crédit / frais demande. Backend incrémente `fraisDemandePayes` du montant saisi, refuse dépassement. Preuve : `PaiementInitialDemandeCreditServiceImpl`, lignes approx. 103-115 et 220-229.
- Impact : règle confirmée.
- Recommandation : test paiement partiel puis second paiement.

## 9. Priorité 8 - RBAC frontend/backend

### Incohérence : navbar et routes non alignées

- ID incohérence : INC-003
- Titre : menus visibles et routes autorisées divergents
- Module : Frontend RBAC
- Gravité : Moyenne
- Règle attendue : un rôle autorisé par route doit pouvoir accéder par menu si la fonctionnalité est opérationnelle; un menu visible doit pointer vers une route autorisée.
- Fonctionnement actuel observé : plusieurs divergences.
- Preuve frontend :
  - `app.routes.ts`, lignes approx. 567-585 : `/organisation` et `/agences` acceptent `ADMIN`, `CHEF_BUREAU`.
  - `navbar.component.ts`, lignes approx. 311-312 : menu Organisation `/agences` visible seulement `ADMIN`.
  - `app.routes.ts`, lignes approx. 609 : `/admin/agents` accepte `ADMIN`, `GESTIONNAIRE`.
  - `navbar.component.ts`, lignes approx. 319-320 : menu Agents visible seulement `ADMIN`.
  - `app.routes.ts`, lignes approx. 265 : `/collectes/suivi-terrain` accepte `GESTIONNAIRE`, `CHEF_BUREAU`, `ADMIN`.
  - `navbar.component.ts`, ligne approx. 271 : menu Ma collecte/Collectes terrain inclut `RCI` vers cette famille de routes.
- Preuve backend : non applicable directement; le risque est frontend/navigation.
- Impact métier : utilisateurs autorisés ne voient pas des fonctions; utilisateurs non autorisés voient des liens menant à refus.
- Impact sécurité : risque faible si backend protège; UX et contrôle interne affectés.
- Impact contrôle interne : actions peuvent être retardées faute de menu.
- Impact audit : traçabilité des accès refusés non documentée.
- Impact base de données : aucun direct.
- Impact frontend : incohérence navigation.
- Impact backend : aucun direct.
- Correction recommandée : générer menu depuis une matrice unique routes/permissions ou harmoniser manuellement.
- Tests métier à prévoir : snapshot menus par rôle + navigation directe.

### Point obligatoire : Caissier accès frais crédit uniquement

- Règle attendue : Caissier encaisse les frais crédit via vue dédiée, pas analyse/approbation.
- Preuve frontend :
  - `navbar.component.ts`, lignes approx. 230-238 : menu `Frais crédit` visible `CAISSIER`.
  - `app.routes.ts`, ligne approx. 472 : `/credits/frais-a-encaisser` inclut CAISSIER.
  - `app.routes.ts`, lignes approx. 466/478/484/515 : autres routes crédit donnent surtout lecture/détail/garantie selon rôles; analyse contrôleur/gestionnaire séparée.
- Preuve backend : `PaiementInitialDemandeCreditServiceImpl.requireCurrentCaissier()`, lignes approx. 236-244.
- Impact métier : encaissement cadré.
- Risque : route frais inclut aussi ADMIN/CHEF_BUREAU/RCI côté frontend; backend protège l'action mais la lecture peut être trop large selon métier.
- Recommandation : distinguer lecture liste des frais et action d'encaissement.

## 10. Priorité 9 - Audit / traçabilité

### Règle auditée : opération caisse trace auteur, référence, rôle, site

- Preuve backend : `OperationCaisseServiceImpl`, lignes approx. 133-165 : construit `OperationCaisse` avec `createdBy`, `referenceMetier`, `observation`, `commentaire`, `utilisateur`, `roleUtilisateur`, `site`.
- Preuve backend audit : lignes approx. 170-176 : sauvegarde puis `auditerOperationP2`.
- Impact audit : opération caisse bien instrumentée.
- Recommandation : aligner toutes entités financières sur ce niveau de traçabilité.

### Incohérence : statuts redondants ou erronés

- ID incohérence : INC-007
- Titre : statuts session redondants et typo statut fiche terrain
- Module : Base / Workflow
- Gravité : Faible à moyenne
- Règle attendue : statuts doivent être non ambigus et orthographiés correctement.
- Fonctionnement actuel observé : `SessionCaisse` utilise `CLOTUREE` et `FERMEE`; `FicheJournaliereAgentTerrain` a un statut `ANNULEA` selon inventaire.
- Preuve backend :
  - `OperationCaisseServiceImpl`, lignes approx. 484-494 : refus spécifique si session `CLOTUREE` ou `FERMEE`.
  - `WorkflowTaskReconciliationServiceImpl`, lignes approx. 79-80 : réconciliation sur `StatutSessionCaisse.CLOTUREE`.
  - Inventaire entités : `FicheJournaliereAgentTerrain` statuts `BROUILLON`, `SOUMISE`, `VALIDEE`, `REJETEE`, `ANNULEA`.
- Preuve frontend : statuts session affichés dans pages caisse; pas de mapping complet audité.
- Impact métier : confusion de cycle de vie.
- Impact sécurité : faible.
- Impact contrôle interne : rapprochement et clôture peuvent dépendre d'un statut plutôt que l'autre.
- Impact audit : filtres audit par statut ambigus.
- Impact base de données : valeurs historiques hétérogènes.
- Impact frontend : libellés et actions conditionnelles peuvent diverger.
- Impact backend : conditions doivent tester plusieurs statuts.
- Correction recommandée : décider statut final unique; migrer valeurs; corriger `ANNULEA` si confirmé en enum.
- Tests métier à prévoir : session finale ne permet plus aucune opération; fiche annulée filtre correctement.

## 11. Priorité 10 - Rôles legacy

### Incohérence : AGENT_BUREAU / RESPONSABLE encore présents

- ID incohérence : INC-004
- Titre : rôles legacy encore actifs par mapping
- Module : Sécurité / RBAC
- Gravité : Moyenne
- Règle attendue : `AGENT_BUREAU` et `RESPONSABLE` ne sont pas des rôles opérationnels; ils doivent être compatibles lecture historique seulement.
- Fonctionnement actuel observé : les rôles existent dans `RoleCode`, sont `@Deprecated`, et plusieurs services les remappent automatiquement.
- Preuve backend :
  - `RoleCode.java`, lignes approx. 20-35 : `RESPONSABLE` et `AGENT_BUREAU` annotés `@Deprecated`.
  - `WorkflowTaskServiceImpl`, lignes approx. 1517-1523 : remappe `AGENT_BUREAU -> GESTIONNAIRE`, `RESPONSABLE -> CHEF_BUREAU`.
  - `ScopeService.java`, lignes approx. 439-446 : même remapping.
- Preuve frontend : `navbar.component.ts`, lignes approx. 178-187 : `normalizeRole()` remappe `AGENT_BUREAU` et `RESPONSABLE`.
- Impact métier : données historiques utilisables, mais migration incomplète.
- Impact sécurité : un utilisateur conservant un rôle legacy peut obtenir des droits opérationnels via mapping.
- Impact contrôle interne : ambiguïté hiérarchique.
- Impact audit : anciennes actions peuvent apparaître sous rôles non officiels.
- Impact base de données : valeurs legacy encore possibles dans `role`/`utilisateur`.
- Impact frontend : menus normalisés silencieusement.
- Impact backend : scopes et tâches utilisent rôles effectifs.
- Correction recommandée : migrer DB, désactiver rôles legacy, conserver mapping uniquement pour lecture historique ou migration contrôlée.
- Tests métier à prévoir : utilisateur legacy ne peut pas se connecter opérationnellement ou est forcé à migration; audit historique reste lisible.

## 12. Fiches complémentaires par priorité obligatoire

### INC-010 - Durée crédit conservée

- ID incohérence : non incohérence, point de contrôle renforcé
- Titre : durée crédit saisie conservée dans collecte et modèles crédit
- Module : Crédit / Collecte
- Gravité : Contrôle important
- Règle attendue : ne pas écraser la durée saisie par 1 mois.
- Fonctionnement actuel observé : frontend collecte transmet durée; backend ligne collecte stocke durée; modèles crédit l'exposent.
- Preuve backend : `CollecteTerrainServiceImpl`, lignes approx. 157-161 et 185-191.
- Preuve frontend : `collecte-du-jour.component.ts`, lignes approx. 105-106, 715-716, 737-760; modèles crédit `credit-response.ts`, `credit-detail-response.ts`, `credit-contrat-response.ts`.
- Impacts : contrat/échéancier fiables; audit positif.
- Recommandation : ajouter test de non-régression.

### INC-011 - Frais demande crédit conservés

- ID incohérence : non incohérence, point de contrôle renforcé
- Titre : montant des frais crédit payé conservé et borné
- Module : Crédit / Caisse
- Gravité : Contrôle important
- Règle attendue : montant saisi par Caissier est conservé; pas de dépassement du reste.
- Fonctionnement actuel observé : montant `fraisPayes` incrémente `fraisDemandePayes`; dépassement interdit.
- Preuve backend : `PaiementInitialDemandeCreditServiceImpl`, lignes approx. 103-115 et 220-229.
- Preuve frontend : `PaiementInitialDemandeCreditFormComponent`, lignes approx. 147-171 et 219-223.
- Recommandation : tests paiement partiel et complet.

### INC-012 - Garantie 20% et gage 2x

- ID incohérence : non incohérence, point de contrôle renforcé
- Titre : garantie 20% confirmée; gage matériel 2x présent
- Module : Crédit / Garantie
- Gravité : Contrôle majeur
- Preuve backend : `GarantieCreditWorkflowServiceImpl`, lignes approx. 57-58, 72-172, 229-240.
- Preuve frontend : `garantie-credit-page.component.ts`, lignes approx. 65-72 et 135.
- Recommandation : test avec garantie insuffisante, garantie bloquée, gage refusé, gage accepté.

### INC-013 - Flux financiers bien séparés

- ID incohérence : non incohérence, point de contrôle renforcé
- Titre : revenus/charges/flux exclus distingués dans rapport revenus
- Module : Rapports financiers
- Gravité : Contrôle majeur
- Preuve backend : `RapportRevenusServiceImpl`, lignes approx. 96-111, 260-360, 200-230, 1420-1460.
- Preuve frontend : `rapport-revenus.component.ts`, onglets décision/revenus/credit/financement/audit, refonte locale.
- Recommandation : tests automatisés par catégorie `CategorieOperationCaisse`.

## 13. Synthèse des recommandations priorisées

1. Retirer RCI de la création d'opération caisse libre, ou obtenir validation métier écrite si RCI doit exceptionnellement corriger.
2. Ajouter `RoleGuard` aux routes rapports caisse qui ont seulement `data.roles`.
3. Aligner navbar et routes avec une matrice unique de permissions.
4. Finaliser migration des rôles legacy `AGENT_BUREAU` et `RESPONSABLE`.
5. Clarifier le modèle officiel collecte/recette et bloquer double génération caisse.
6. Standardiser la traçabilité `createdBy`, antenne, site, référence métier et commentaire.
7. Unifier statuts session finale et corriger typo `ANNULEA` si confirmée.
8. Corriger la logique de supervision `Mes actions` pour ADMIN/COO/GERANT_GENERAL selon permission.
9. Prouver ou implémenter les primes Agent Terrain 1%/2%/200 FC dans un service dédié et testé.
10. Ajouter tests de non-régression sur frais crédit, durée crédit, garantie 20%, flux financiers.

## 14. Tests métier à prévoir

### Crédit complet

- Demande créée par Gestionnaire -> tâche Gestionnaire créée.
- Pré-analyse validée -> tâche Contrôleur créée.
- Analyse validée -> tâche garantie Contrôleur créée.
- Garantie 20% insuffisante -> blocage impossible avec message de dépôt complémentaire.
- Garantie 20% bloquée -> demande peut avancer.
- Gage matériel accepté inférieur à 2x -> validation rejetée si règle applicable.
- Approbation Chef -> tâche Caissier créée.
- Décaissement Caissier -> tâche clôturée, sortie caisse flux exclu.

### Mes actions

- Chef Bureau voit approbation crédit, validation dépense, clôture session.
- ADMIN voit ou supervise selon règle corrigée.
- COO/Gérant Général voient tâches de supervision attendues.
- RCI voit uniquement audit/caisse si règle confirmée.
- `activeKey` empêche doublons.

### Caisse

- CAISSIER ne peut pas créer opération libre.
- RCI ne peut pas créer opération libre après correction.
- Session clôturée/refusée empêche opération.
- Rapport caisse route sans rôle est refusée après guard.

### Collecte terrain

- Agent soumet collecte avec lignes.
- Caissier confirme billetage seulement sur même antenne.
- Contrôleur valide seulement sur même antenne.
- Validation génère opérations une seule fois.
- `DEMANDE_CREDIT` disparaît si crédit bloquant.

### Retrait épargne

- Demande créée -> tâche contrôleur.
- Validation -> tâche caissier.
- Paiement -> opération épargne/caisse, tâche clôturée.
- Créateur, antenne, référence audit visibles.

### Paie / transport

- Prime Agent Terrain calculée sur épargne/remboursements/carnets validés.
- Prime motivation manuelle exige motif si > 0.
- Transport Agent Terrain séparé du salaire.
- Rapport détecte partiel/surpaye/transport non configuré.

### Rapports financiers

- Frais demande crédit = revenu.
- Intérêt/pénalité = revenu.
- Principal remboursé = flux exclu.
- Epargne collectée = flux exclu.
- Décaissement crédit = flux exclu, pas charge.
- Salaire/transport/fonctionnement/achat carnets = charges.

## 15. Limites de cet audit renforcé

- Audit statique sans exécution de tests.
- Certaines lignes backend proviennent de recherches PowerShell hors workspace VS Code; elles sont approximatives mais rattachées à fichiers/méthodes.
- Les permissions exactes en production dépendent de la table `role_permissions`, non exportée ici.
- Les endpoints rapports caisse doivent encore être vérifiés côté contrôleurs backend pour conclure sur la protection réelle.
- Les formules primes Agent Terrain nécessitent une passe dédiée dans les services de paie/consolidation si l'équipe veut une preuve définitive.
