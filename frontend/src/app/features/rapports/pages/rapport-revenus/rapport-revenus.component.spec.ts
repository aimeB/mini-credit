import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { RapportRevenusComponent } from './rapport-revenus.component';
import { RapportRevenusService } from '../../services/rapport-revenus.service';
import { AgenceService } from '../../../employes/services/agence.service';
import { AuthService } from '../../../../core/services/auth.service';
import { RapportRevenusResponse } from '../../models/rapport-revenus.model';

describe('RapportRevenusComponent', () => {
  let fixture: ComponentFixture<RapportRevenusComponent>;
  let component: RapportRevenusComponent;

  const report: RapportRevenusResponse = {
    dateDebut: '2026-07-01',
    dateFin: '2026-07-31',
    totalRevenus: 120000,
    totalCharges: 45000,
    beneficeNetEstime: 75000,
    resultatPrevisionnelApresSalairesAPayer: 55000,
    resultatPrevisionnelApresChargesFixes: 50000,
    chargesGlobalesSiege: 30000,
    resultatApresChargesGlobalesSiege: 20000,
    kpis: {
      fraisAnalyseCredit: 50000,
      fraisRetraitEpargne: 0,
      interetsCredit: 70000,
      penalitesCredit: 0,
      carnetsVendus: 0,
      revenusDivers: 0
    },
    parAntenne: [],
    parCategorie: [
      { categorie: 'FRAIS_ANALYSE_CREDIT', montant: 50000 },
      { categorie: 'INTERETS_CREDIT', montant: 70000 }
    ],
    apportsFinancements: {
      totalApprovisionnements: 90000,
      apportsProprietaire: 80000,
      transfertsInternes: 0,
      pretsRecus: 0,
      remboursementsAvance: 0,
      autresFinancements: 10000,
      approvisionnementsNonQualifies: 0,
      capitalInjecteARecuperer: 80000,
      remboursementsApportPayes: 20000,
      capitalInjecteRestantARecuperer: 60000,
      capitalInjecteIndicatif: false,
      commentairePedagogique: null
    },
    capaciteRetraitProprietaire: {
      capitalInjecteCumule: 80000,
      remboursementsApportPayes: 20000,
      capitalInjecteRestantARecuperer: 60000,
      soldeCaisseTheoriqueActif: 140000,
      fondsMembresAProteger: 17000,
      tresorerieApresProtectionMembres: 123000,
      engagementsCourtTerme: 32000,
      salairesRestantAPayer: 25000,
      transportRestantAPayer: 7000,
      fondsMinimumSecurite: 10000,
      margePrudence: 5000,
      tresorerieRecuperablePrudente: 78000,
      tresoreriePotentiellementRecuperable: 91000,
      montantRecuperableConseille: 60000,
      retraitDeconseille: false,
      alerte: null,
      alerteTresorerie: null,
      alerteCredit: null,
      commentairePedagogique: null
    },
    tresorerieDisponible: {
      soldeCaisseTheoriqueActif: 140000,
      retraitsEpargneValidesNonPayes: 2000,
      fondsMembresAProteger: 17000,
      tresorerieApresProtectionMembres: 123000,
      creditsApprouvesNonDecaisses: 0,
      depensesValideesNonPayees: 0,
      salairesRestantAPayer: 25000,
      transportRestantAPayer: 7000,
      fondsMinimumSecurite: 10000,
      margePrudence: 5000,
      totalEngagementsCourtTerme: 32000,
      tresorerieDisponibleApresEngagements: 91000,
      tresorerieRecuperablePrudente: 78000,
      soldeCaisseIndicatif: false,
      commentairePedagogique: null
    },
    fondsMembresProteges: {
      epargneDisponibleMembres: 10000,
      epargneBloqueeGaranties: 5000,
      retraitsEpargneValidesNonPayesNonInclus: 2000,
      totalFondsMembres: 17000,
      commentairePedagogique: null
    },
    comparaisonAgences: {
      totalRevenusAgences: 120000,
      totalChargesAgences: 70000,
      totalResultatAgences: 50000,
      chargesGlobalesSiege: 30000,
      resultatGlobalApresChargesSiege: 20000,
      agences: [{
        agenceId: 1,
        agenceNom: 'Delvaux',
        revenusReels: 120000,
        chargesConnues: 70000,
        resultatNetEstime: 50000,
        margePourcentage: 41.67,
        commissionsRetrait: 10000,
        fraisCredit: 50000,
        interetsCredit: 60000,
        penalitesCredit: 0,
        carnetsVendus: 0,
        autresRevenus: 0,
        salairesPayes: 20000,
        salairesRestantAPayer: 25000,
        primes: 5400,
        transportTerrain: 3000,
        transportRestantAPayer: 7000,
        fonctionnement: 15000,
        achatCarnets: 0,
        autresCharges: 0,
        collectesValidees: 4,
        membresActifs: 28,
        creditsActifs: 9,
        alerteDeficit: false,
        alerteChargesElevees: false
      }],
      agencePlusRevenus: {
        agenceId: 1,
        agenceNom: 'Delvaux',
        revenusReels: 120000,
        chargesConnues: 70000,
        resultatNetEstime: 50000,
        margePourcentage: 41.67,
        commissionsRetrait: 10000,
        fraisCredit: 50000,
        interetsCredit: 60000,
        penalitesCredit: 0,
        carnetsVendus: 0,
        autresRevenus: 0,
        salairesPayes: 20000,
        salairesRestantAPayer: 25000,
        primes: 5400,
        transportTerrain: 3000,
        transportRestantAPayer: 7000,
        fonctionnement: 15000,
        achatCarnets: 0,
        autresCharges: 0,
        collectesValidees: 4,
        membresActifs: 28,
        creditsActifs: 9,
        alerteDeficit: false,
        alerteChargesElevees: false
      },
      agencePlusRentable: null,
      agencePlusCharges: null,
      agencesDeficitaires: []
    },
    mouvementsNonRevenus: [],
    chargesParCategorie: [{ categorie: 'MASSE_SALARIALE', montant: 45000 }],
    chargeDetails: [],
    mouvementsNonRevenusParAntenne: [],
    controlesCoherence: [],
    details: [],
    masseSalariale: {
      masseSalarialeMensuellePrevue: 45000,
      salairesPayes: 20000,
      salairesRestantAPayer: 25000,
      resultatPrevisionnelApresSalairesAPayer: 55000,
      nombreEmployesActifs: 1,
      periodePaie: '2026-07',
      detailsEmployes: [{
        employeId: 11,
        matricule: 'AT-001',
        nomComplet: 'Agent Terrain Test',
        poste: 'AGENT_TERRAIN',
        salaireBase: 30000,
        totalEpargneCollecteeValidee: 100000,
        primeEpargne: 1000,
        totalRemboursementCollecteValide: 200000,
        primeRemboursement: 4000,
        nombreCarnetsVendus: 2,
        bonusCarnets: 400,
        totalPrimesAgentTerrain: 5400,
        primesBonusJustifies: 0,
        remunerationAttendueTotale: 35400,
        salairePrevu: 35400,
        montantPaye: 20000,
        salairesPartielsPayes: 20000,
        avancesPayees: 0,
        retenues: 0,
        primes: 0,
        commissions: 0,
        regularisations: 0,
        resteAPayer: 15400,
        statutPaie: 'PARTIEL'
      }]
    },
    transportFixePrevu: {
      totalTransportPrevu: 10000,
      totalTransportPaye: 3000,
      totalTransportRestant: 7000,
      nombreAgentsTerrain: 1,
      nombreSitesConfigures: 1,
      nombreSitesSansMontant: 0,
      nombreJoursPeriode: 20,
      periodeCharge: '2026-07',
      detailsParSite: [{
        siteId: 2,
        siteNom: 'Site Terrain',
        montantJournalierParAgent: 500,
        nombreAgentsTerrainActifs: 1,
        nombreJoursPeriode: 20,
        transportPrevuSite: 10000,
        transportPayeSite: 3000,
        transportRestantSite: 7000
      }],
      details: [{
        employeId: 11,
        matricule: 'AT-001',
        nomComplet: 'Agent Terrain Test',
        siteNom: 'Site Terrain',
        montantJournalierParAgent: 500,
        nombreJoursPeriode: 20,
        montantPrevu: 10000,
        montantPaye: 3000,
        resteAPayer: 7000,
        statut: 'PARTIEL'
      }]
    }
  };

  const rapportRevenusServiceStub = {
    getRapport: jasmine.createSpy('getRapport').and.returnValue(of(report))
  };

  const agenceServiceStub = {
    getAll: jasmine.createSpy('getAll').and.returnValue(of([]))
  };

  const authServiceStub = {
    getCurrentUser: jasmine.createSpy('getCurrentUser').and.returnValue({ role: 'ADMIN' })
  };

  beforeEach(async () => {
    rapportRevenusServiceStub.getRapport.calls.reset();
    agenceServiceStub.getAll.calls.reset();
    authServiceStub.getCurrentUser.and.returnValue({ role: 'ADMIN' });

    await TestBed.configureTestingModule({
      imports: [RapportRevenusComponent],
      providers: [
        { provide: RapportRevenusService, useValue: rapportRevenusServiceStub },
        { provide: AgenceService, useValue: agenceServiceStub },
        { provide: AuthService, useValue: authServiceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RapportRevenusComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function pageText(): string {
    return fixture.nativeElement.textContent || '';
  }

  it('affiche les nouveaux libelles prudents', () => {
    component.activeTab = 'decision';
    fixture.detectChanges();

    const text = pageText();
    expect(text).toContain('Resultat net estime');
    expect(text).toContain('Resultat apres salaires restant a payer');
    expect(text).toContain('Fonds membres proteges');
    expect(text).toContain('Capital proprietaire non encore rembourse');
    expect(text).toContain('Estimation prudente recuperable');
    expect(text).toContain('Montant indicatif a valider');
  });

  it('affiche resultat net estime au lieu de benefice net', () => {
    component.activeTab = 'revenus';
    fixture.detectChanges();

    const text = pageText();
    expect(text).toContain('Resultat net estime');
    expect(text).not.toContain('Benefice net estime');
  });

  it('affiche fonds membres proteges non recuperables', () => {
    component.activeTab = 'decision';
    fixture.detectChanges();

    const text = pageText();
    expect(text).toContain('Les fonds membres proteges ne sont pas recuperables par le proprietaire.');
  });

  it('affiche capital proprietaire non encore rembourse', () => {
    component.activeTab = 'decision';
    fixture.detectChanges();

    const text = pageText();
    expect(text).toContain('Capital proprietaire non encore rembourse');
    expect(text).toContain('Le capital proprietaire non encore rembourse correspond aux apports restants a rembourser.');
  });

  it('affiche estimation prudente recuperable', () => {
    component.activeTab = 'decision';
    fixture.detectChanges();

    const text = pageText();
    expect(text).toContain('Estimation prudente recuperable');
    expect(text).toContain("L'estimation prudente recuperable est une aide de gestion, pas une autorisation automatique.");
  });

  it('n affiche pas retrait proprietaire conseille', () => {
    component.activeTab = 'financement';
    fixture.detectChanges();

    expect(pageText()).not.toContain('Retrait proprietaire conseille');
  });

  it('n affiche pas peut retirer', () => {
    component.activeTab = 'decision';
    fixture.detectChanges();

    expect(pageText().toLowerCase()).not.toContain('peut retirer');
  });

  it('affiche aide interpretation', () => {
    component.activeTab = 'decision';
    fixture.detectChanges();

    const text = pageText();
    expect(text).toContain("Aide a l'interpretation");
    expect(text).toContain('Ce que cela signifie');
    expect(text).toContain("Les revenus reels representent les gains de l'institution.");
  });

  it('affiche analyse proprietaire', () => {
    component.activeTab = 'decision';
    fixture.detectChanges();

    const text = pageText();
    expect(text).toContain('Analyse proprietaire');
    expect(text).toContain('A valider par gestion');
    expect(text).toContain('Cette section est une aide interne a la decision.');
    expect(text).toContain('Elle ne constitue pas une regle officielle 3N ni une autorisation automatique de retrait.');
  });

  it('primes terrain restent charges pas revenus', () => {
    component.activeTab = 'paie';
    fixture.detectChanges();

    const text = pageText();
    expect(text).toContain('Masse salariale mensuelle');
    expect(text).toContain('Prime épargne 1%');
    expect(text).toContain('Prime remboursement 2%');
    expect(text).toContain('Total primes terrain');
    expect(text).toContain('Agent Terrain Test');
    expect(report.parCategorie.map((item) => item.categorie)).not.toContain('PRIMES_AGENT_TERRAIN');
  });

  it('transport reste separe', () => {
    component.activeTab = 'paie';
    fixture.detectChanges();

    const text = pageText();
    expect(text).toContain('Transport terrain par site');
    expect(text).toContain('Transport journalier total prévu');
    expect(text).toContain('Transport restant à payer');
    expect(text).toContain('Site Terrain');
  });

  it('montants kpi restent affiches', () => {
    component.activeTab = 'decision';
    fixture.detectChanges();

    const text = pageText();
    expect(text).toContain(component.formatMoney(report.totalRevenus));
    expect(text).toContain(component.formatMoney(report.totalCharges || 0));
    expect(text).toContain(component.formatMoney(report.beneficeNetEstime || 0));
    expect(text).toContain(component.formatMoney(report.fondsMembresProteges?.totalFondsMembres || 0));
    expect(text).toContain(component.formatMoney(report.capaciteRetraitProprietaire?.montantRecuperableConseille || 0));
  });

  it('affiche le sous texte transport dans tresorerie engagements', () => {
    component.activeTab = 'credit';
    fixture.detectChanges();

    const text = pageText();
    expect(text).toContain('Disponible / engagements');
    expect(text).toContain('Transport terrain restant à payer');
    expect(text).toContain('Transport prévu des Agents Terrain selon montant journalier par site, diminué des paiements déjà effectués.');
  });

  it('affiche la comparaison des agences et les charges siege', () => {
    component.activeTab = 'comparaison';
    fixture.detectChanges();

    const text = pageText();
    expect(text).toContain('Comparaison des agences');
    expect(text).toContain('Rentabilite par agence');
    expect(text).toContain('Delvaux');
    expect(text).toContain('Charges globales / siège');
    expect(text).toContain('Resultat global apres charges siège');
    expect(text).toContain('Membres actifs');
    expect(text).toContain('Credits actifs');
  });

  it('masque les actions de correction pour RCI', () => {
    authServiceStub.getCurrentUser.and.returnValue({ role: 'RCI' });

    expect(component.canQualifyFinancement({ operationId: 1, natureFinancement: 'Approvisionnement non qualifie', montant: 1000 })).toBeFalse();
    expect(component.canRattacherPaie({ depenseId: 1, categorieTechnique: 'SALAIRE', statut: 'PAYEE', montant: 1000 })).toBeFalse();
    expect(component.canRattacherTransport({ depenseId: 1, categorieTechnique: 'TRANSPORT', statut: 'PAYEE', montant: 1000 })).toBeFalse();
  });
});