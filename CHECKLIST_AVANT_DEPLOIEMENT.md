# Checklist avant déploiement Mini-Crédit 3N

## Technique
- [ ] Frontend démarre avec `npx ng serve`
- [ ] Backend compile avec `.\mvnw.cmd clean package -DskipTests`
- [ ] Backend démarre avec `.\mvnw.cmd spring-boot:run`
- [ ] Build production frontend réussi
- [ ] Jar backend généré
- [ ] Base de données sauvegardée

## Sécurité
- [ ] HTTPS prévu
- [ ] Swagger désactivé en production
- [ ] Clé JWT hors du code source
- [ ] CORS limité au domaine officiel
- [ ] Mots de passe production non stockés dans Git

## Métier
- [ ] Workflow crédit testé
- [ ] Workflow retrait épargne testé
- [ ] Workflow collecte terrain testé
- [ ] Workflow caisse testé
- [ ] RBAC par rôle testé
- [ ] Audit des opérations critiques vérifié
