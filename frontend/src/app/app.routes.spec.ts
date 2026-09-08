import { routes } from './app.routes';

describe('Routes frontend roles', () => {
  const removedRoles = ['AGENT_BUREAU', 'RESPONSABLE', 'ROLE_AGENT_BUREAU', 'ROLE_RESPONSABLE'];

  function findRoute(path: string): any {
    const root = routes.find((r: any) => r.path === '');
    const children = root?.children || [];
    return children.find((r: any) => r.path === path);
  }

  function collectRoutes(routeList: any[]): any[] {
    return routeList.flatMap((route) => [route, ...collectRoutes(route.children || [])]);
  }

  it('utilise CHEF_BUREAU sur les routes de supervision', () => {
    const membres = findRoute('membres');
    const credits = findRoute('credits');
    const organisation = findRoute('organisation');

    expect(membres.data.roles).toContain('CHEF_BUREAU');
    expect(credits.data.roles).toContain('CHEF_BUREAU');
    expect(organisation.data.roles).toContain('CHEF_BUREAU');
    expect(membres.data.roles).toContain('GESTIONNAIRE');
  });

  it('interdit CHEF_BUREAU sur les routes reservees CONTROLEUR', () => {
    const controle = findRoute('caisses/controle');
    const ecarts = findRoute('caisses/ecarts');
    const collecteControle = findRoute('collectes/a-controler');
    const analyse = findRoute('credits/demandes/:id/analyse');

    expect(controle.data.roles).not.toContain('CHEF_BUREAU');
    expect(ecarts.data.roles).not.toContain('CHEF_BUREAU');
    expect(collecteControle.data.roles).not.toContain('CHEF_BUREAU');
    expect(analyse.data.roles).toContain('CONTROLEUR');
    expect(analyse.data.roles).not.toContain('CHEF_BUREAU');
    expect(analyse.data.roles).toContain('CONTROLEUR');
  });

  it('autorise CONTROLEUR sur les routes demandes credit de lecture et analyse', () => {
    const demandes = findRoute('credits/demandes');
    const demandeDetail = findRoute('credits/demandes/:id');
    const analyse = findRoute('credits/demandes/:id/analyse');
    const analyseRisque = findRoute('credits/demandes/:id/analyse-risque');
    const garantie = findRoute('credits/demandes/:id/garantie');

    expect(demandes.data.roles).toContain('CONTROLEUR');
    expect(demandeDetail.data.roles).toContain('CONTROLEUR');
    expect(analyse.data.roles).toContain('CONTROLEUR');
    expect(analyseRisque.data.roles).toContain('CONTROLEUR');
    expect(garantie.data.roles).toContain('CONTROLEUR');
    expect(garantie.data.roles).toContain('CHEF_BUREAU');
    expect(garantie.data.roles).not.toContain('CAISSIER');
    expect(garantie.data.roles).not.toContain('RCI');
  });

  it('autorise uniquement CAISSIER sur la route dédiée de décaissement crédit', () => {
    const decaissement = findRoute('credits/:creditId/decaissement');

    expect(decaissement).toBeTruthy();
    expect(decaissement.data.roles).toEqual(['CAISSIER']);
  });

  it('autorise GESTIONNAIRE sur les routes de pré-analyse crédit', () => {
    const demandes = findRoute('credits/demandes');
    const demandeDetail = findRoute('credits/demandes/:id');

    expect(demandes.data.roles).toContain('GESTIONNAIRE');
    expect(demandeDetail.data.roles).toContain('GESTIONNAIRE');
  });

  it('autorise GESTIONNAIRE en consultation terrain sans creation admin', () => {
    const sites = findRoute('sites');
    const siteDetail = findRoute('sites/:id');
    const siteCreate = findRoute('sites/nouveau');
    const agents = findRoute('admin/agents');
    const agentCreate = findRoute('admin/agents/nouveau');

    expect(sites.data.roles).toContain('GESTIONNAIRE');
    expect(siteDetail.data.roles).toContain('GESTIONNAIRE');
    expect(agents.data.roles).toContain('GESTIONNAIRE');
    expect(siteCreate.data.roles).not.toContain('GESTIONNAIRE');
    expect(agentCreate.data.roles).not.toContain('GESTIONNAIRE');
  });

  it('expose les routes de compatibilite du menu Gestionnaire', () => {
    const preAnalyses = findRoute('credits/pre-analyses');
    const performances = findRoute('performances-terrain');
    const reclamations = findRoute('reclamations');

    expect(preAnalyses.redirectTo).toBe('credits/demandes');
    expect(performances.redirectTo).toBe('rapports');
    expect(reclamations.redirectTo).toBe('mes-actions');
  });

  it('autorise CONTROLEUR sur la route epargne', () => {
    const epargne = findRoute('epargne');

    expect(epargne.data.roles).toContain('ADMIN');
    expect(epargne.data.roles).toContain('CHEF_BUREAU');
    expect(epargne.data.roles).toContain('CONTROLEUR');
    expect(epargne.data.roles).toContain('CHEF_BUREAU');
  });

  it('expose la route mes-actions pour le slice workflow caisse', () => {
    const mesActions = findRoute('mes-actions');

    expect(mesActions).toBeTruthy();
    expect(mesActions.canActivate?.length).toBe(2);
    expect(mesActions.data.roles).toContain('CAISSIER');
    expect(mesActions.data.roles).toContain('CONTROLEUR');
    expect(mesActions.data.roles).toContain('CHEF_BUREAU');
    expect(mesActions.data.roles).toContain('GESTIONNAIRE');
    expect(mesActions.data.roles).toContain('GESTIONNAIRE');
    expect(mesActions.data.permissions).toContain('TASK_READ_OWN');
    expect(mesActions.data.permissions).toContain('TASK_READ_ANTENNE');
    expect(mesActions.data.permissions).toContain('TASK_SUPERVISE');
    expect(mesActions.data.permissions).toContain('TASK_AUDIT');
    expect(mesActions.data.permissionBypassRoles).toEqual(['ADMIN']);
  });

  it('garde le workflow caisse sans casser la compatibilite CHEF_BUREAU', () => {
    const ouverture = findRoute('caisses/session/ouverture');
    const cloture = findRoute('caisses/session/:id/cloture');

    expect(ouverture.data.roles).toContain('CHEF_BUREAU');
    expect(cloture.data.roles).toContain('CHEF_BUREAU');
  });

  it('autorise CONTROLEUR sur le module caisse principal et lecture/controle', () => {
    const caisses = findRoute('caisses');
    const sessionDetail = findRoute('caisses/session/:id');
    const controle = findRoute('caisses/controle');
    const ecarts = findRoute('caisses/ecarts');

    expect(caisses.data.roles).toContain('CONTROLEUR');
    expect(sessionDetail.data.roles).toContain('CONTROLEUR');
    expect(controle.data.roles).toContain('CONTROLEUR');
    expect(ecarts.data.roles).toContain('CONTROLEUR');
  });

  it('n autorise pas CONTROLEUR sur les routes caisse operationnelles', () => {
    const ouverture = findRoute('caisses/session/ouverture');
    const cloture = findRoute('caisses/session/:id/cloture');
    const operationCreate = findRoute('caisses/session/:sessionId/operations/nouveau');

    expect(ouverture.data.roles).not.toContain('CONTROLEUR');
    expect(cloture.data.roles).not.toContain('CONTROLEUR');
    expect(operationCreate.data.roles).not.toContain('CONTROLEUR');
  });

  it('protege les rapports caisse avec RoleGuard et autorise la consultation RCI', () => {
    const rapports = [
      findRoute('caisses/rapports/session'),
      findRoute('caisses/rapports/journalier'),
      findRoute('caisses/rapports/periode'),
      findRoute('caisses/rapports/depenses'),
      findRoute('caisses/rapports/ecarts')
    ];

    for (const route of rapports) {
      expect(route).toBeTruthy();
      expect(route.canActivate).toBeTruthy();
      expect(route.data.roles).toContain('RCI');
      expect(route.data.roles).toContain('CONTROLEUR');
    }
  });

  it('autorise le CAISSIER sur les frais credit sans lui ouvrir les demandes completes', () => {
    const frais = findRoute('credits/frais-a-encaisser');
    const paiementInitial = findRoute('credits/demandes/:id/paiement-initial');
    const demandes = findRoute('credits/demandes');
    const creationDemande = findRoute('credits/demandes/nouveau');

    expect(frais.data.roles).toContain('CAISSIER');
    expect(frais.data.roles).not.toContain('RCI');
    expect(paiementInitial.data.roles).toEqual(['CAISSIER']);
    expect(demandes.data.roles).not.toContain('CAISSIER');
    expect(creationDemande.data.roles).not.toContain('CAISSIER');
  });

  it('garde RCI en consultation caisse sans action operationnelle libre', () => {
    const operationCreate = findRoute('caisses/session/:sessionId/operations/nouveau');
    const controle = findRoute('caisses/controle');
    const ecarts = findRoute('caisses/ecarts');
    const fraisCredit = findRoute('credits/frais-a-encaisser');

    expect(controle.data.roles).toContain('RCI');
    expect(ecarts.data.roles).toContain('RCI');
    expect(operationCreate.data.roles).not.toContain('RCI');
    expect(fraisCredit.data.roles).not.toContain('RCI');
  });

  it('ne reference aucun role supprime dans les routes', () => {
    const roles = collectRoutes(routes).flatMap((route) => route.data?.roles || []);
    const bypassRoles = collectRoutes(routes).flatMap((route) => route.data?.permissionBypassRoles || []);

    for (const removedRole of removedRoles) {
      expect(roles).not.toContain(removedRole);
      expect(bypassRoles).not.toContain(removedRole);
    }
  });
});
