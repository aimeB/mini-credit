# Priorité 13 - Registre des anomalies de recette

Règle de priorité : aucune correction directe sans fiche anomalie renseignée.

## 1. Registre synthétique

| ID | Titre | Rôle | Page | Gravité | Impact | Décision | Statut | Référence correction |
|---|---|---|---|---|---|---|---|---|
| P13-001 | à renseigner |  |  |  |  |  | Nouveau |  |

Gravité autorisée : `Bloquant`, `Majeur`, `Moyen`, `Mineur`, `Ergonomie`.

Impact autorisé : `métier`, `sécurité`, `contrôle interne`, `audit`, `base de données`, `frontend`, `backend`.

Décision autorisée : `à corriger immédiatement`, `à corriger après recette`, `simple amélioration UI`, `règle non définie dans documents`, `non anomalie après arbitrage`.

Statut autorisé : `Nouveau`, `En diagnostic`, `Validé anomalie`, `Correction demandée`, `Corrigé`, `Re-test OK`, `Re-test KO`, `Clos`.

## 2. Modèle fiche anomalie

Copier/coller ce modèle pour chaque anomalie.

```markdown
## P13-XXX - Titre court et précis

Titre :

Rôle :

Page :

Données utilisées :
- Membre :
- Crédit :
- Caisse / session :
- Montant :
- Période :
- Autre :

Étapes :
1.
2.
3.

Résultat attendu :

Résultat obtenu :

Gravité : Bloquant / Majeur / Moyen / Mineur / Ergonomie

Impact : métier / sécurité / contrôle interne / audit / base de données / frontend / backend

Capture / logs :
- Capture écran :
- Console navigateur :
- Réponse API :
- Logs backend :

Diagnostic initial :
- Reproductible : Oui / Non / A confirmer
- Navigateur :
- Environnement :
- Date/heure :

Décision :
- à corriger immédiatement / à corriger après recette / simple amélioration UI / règle non définie dans documents

Correction demandée :

Re-test :
- Date :
- Résultat :
```

## 3. Points sensibles à surveiller dès le Jour 1

Ces points ne sont pas des anomalies tant qu'ils ne sont pas reproduits pendant la recette.

| Point | Pourquoi c'est sensible | Scénario |
|---|---|---|
| RCI et bouton Ajouter opération dans détail session caisse | RCI doit superviser/auditer sans créer d'opération caisse libre | S01 / S06 |
| Caissier et opération caisse libre | Le Caissier doit utiliser les workflows dédiés, pas une opération manuelle libre | S01 / S06 |
| Frais demande crédit forcés à 5 000 | La recette exige conservation de 7 500 | S03 |
| Durée crédit forcée à 1 mois | La recette exige 3 ou 6 mois conservés | S03 |
| Garantie matérielle < 2x acceptée | Violerait la règle gage matériel | S03 |
| Collecte validée générée deux fois | Risque comptable majeur | S02 |
| Epargne collectée comptée comme revenu | Risque rapport/résultat | S02 / S08 |
| Principal remboursé compté comme revenu | Risque rapport/résultat | S04 / S08 |
| Retrait épargne compté comme charge | Risque rapport/résultat | S05 / S08 |
| Supervision comptée comme Mes actions personnelles | Risque pilotage / workflow | S09 |
| Audit incomplet sur opération financière | Risque contrôle interne | S10 |

## 4. Prompt court de correction après fiche validée

Une fois une anomalie confirmée, utiliser un prompt court du type :

```text
Corriger l'anomalie P13-XXX documentée dans docs/PRIORITE_13_REGISTRE_ANOMALIES.md.
Respecter la décision indiquée, ne modifier que le périmètre concerné, préserver les endpoints/DTO/workflows sauf si la fiche demande explicitement une correction backend.
Ajouter ou ajuster les tests ciblés puis valider avec build/tests pertinents.
```
