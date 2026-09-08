export type RevenuCategorie =
  | 'FRAIS_ANALYSE_CREDIT'
  | 'FRAIS_RETRAIT_EPARGNE'
  | 'INTERETS_CREDIT'
  | 'PENALITES_CREDIT'
  | 'CARNETS_VENDUS'
  | 'REVENUS_DIVERS';

export type RevenuSource = 'CAISSE' | 'CREDIT' | 'COLLECTE';

export interface RevenuKpiDto {
  fraisAnalyseCredit: number;
  fraisRetraitEpargne: number;
  interetsCredit: number;
  penalitesCredit: number;
  carnetsVendus: number;
  revenusDivers: number;
}

export interface RevenuParAntenneDto extends RevenuKpiDto {
  antenneId?: number | null;
  antenneNom?: string | null;
  totalRevenus: number;
}

export interface RevenuParCategorieDto {
  categorie: RevenuCategorie | string;
  sousCategorie?: string | null;
  montant: number;
}

export interface RevenuDetailDto {
  date?: string | null;
  antenneId?: number | null;
  antenne?: string | null;
  categorie: RevenuCategorie | string;
  nature?: string | null;
  sousCategorie?: string | null;
  source: RevenuSource | string;
  reference?: string | null;
  membre?: string | null;
  montant: number;
  utilisateur?: string | null;
  observation?: string | null;
}

export interface ChargeDetailDto {
  depenseId?: number | null;
  caisseId?: number | null;
  date?: string | null;
  antenneId?: number | null;
  antenne?: string | null;
  categorie?: string | null;
  categorieTechnique?: string | null;
  reference?: string | null;
  beneficiaire?: string | null;
  montant: number;
  employeId?: number | null;
  employeMatricule?: string | null;
  employeNomComplet?: string | null;
  employePoste?: string | null;
  periodePaie?: string | null;
  typePaiementPersonnel?: string | null;
  montantRemunerationReference?: number | null;
  montantEcartRemuneration?: number | null;
  motifEcartRemuneration?: string | null;
  naturePaiementPaie?: string | null;
  montantSalaireDu?: number | null;
  montantDejaPaye?: number | null;
  montantRestantApresPaiement?: number | null;
  montantRetenue?: number | null;
  motifRetenue?: string | null;
  motifPaiementPartiel?: string | null;
  commentairePaie?: string | null;
  periodeCharge?: string | null;
  typeChargeFixe?: string | null;
  siteChargeId?: number | null;
  siteChargeNom?: string | null;
  montantChargeFixeReference?: number | null;
  montantEcartChargeFixe?: number | null;
  commentaireRapprochement?: string | null;
  salaireBase?: number | null;
  epargneCollecteeReference?: number | null;
  remboursementCollecteReference?: number | null;
  nombreCarnetsVendus?: number | null;
  primeMobilisationEpargne?: number | null;
  primeMobilisationRemboursement?: number | null;
  bonusCarnets?: number | null;
  primeMotivationManuelle?: number | null;
  modeCalculPaie?: string | null;
  statut?: string | null;
  canRattacherPaie?: boolean | null;
  canRattacherTransport?: boolean | null;
  observation?: string | null;
}

export interface ChargeParCategorieDto {
  categorie: string;
  montant: number;
}

export interface MouvementNonRevenuParAntenneDto {
  antenneId?: number | null;
  antenneNom?: string | null;
  epargneCollectee: number;
  principalCreditRembourse: number;
  garantiesDepotGarantie: number;
  approvisionnementsCaisse: number;
  retraitsEpargne: number;
  decaissementsCredit: number;
  autresMouvementsNonRevenus: number;
  totalHorsRevenus: number;
}

export interface ControleCoherenceDto {
  severite: 'ROUGE' | 'ORANGE' | string;
  type: string;
  reference?: string | null;
  message: string;
  antenneId?: number | null;
  antenne?: string | null;
}

export interface CarnetMargeDto {
  nombreCarnetsVendus: number;
  montantVentesCarnets: number;
  coutAchatUnitaireCarnet: number;
  coutTotalCarnets: number;
  margeCarnets: number;
}

export interface PositionCreditDto {
  capitalDecaisse: number;
  principalRecupere: number;
  capitalRestantDehors: number;
  capitalRestantEstime: boolean;
  interetsEncaisses: number;
  penalitesEncaisses: number;
  nombreCreditsActifs: number;
  nombreCreditsRembourses: number;
  commentairePedagogique?: string | null;
}

export interface ApportFinancementDto {
  totalApprovisionnements: number;
  apportsProprietaire: number;
  transfertsInternes: number;
  pretsRecus: number;
  remboursementsAvance: number;
  autresFinancements: number;
  approvisionnementsNonQualifies: number;
  capitalInjecteARecuperer: number;
  remboursementsApportPayes: number;
  capitalInjecteRestantARecuperer: number;
  capitalInjecteIndicatif: boolean;
  commentairePedagogique?: string | null;
}

export interface CapaciteRetraitProprietaireDto {
  capitalInjecteCumule: number;
  remboursementsApportPayes: number;
  capitalInjecteRestantARecuperer: number;
  soldeCaisseTheoriqueActif: number;
  fondsMembresAProteger: number;
  tresorerieApresProtectionMembres: number;
  engagementsCourtTerme: number;
  salairesRestantAPayer?: number;
  transportRestantAPayer?: number;
  fondsMinimumSecurite: number;
  margePrudence: number;
  tresorerieRecuperablePrudente: number;
  tresoreriePotentiellementRecuperable: number;
  montantRecuperableConseille: number;
  retraitDeconseille: boolean;
  alerte?: string | null;
  alerteTresorerie?: string | null;
  alerteCredit?: string | null;
  commentairePedagogique?: string | null;
}

export interface TresorerieDisponibleDto {
  soldeCaisseTheoriqueActif: number;
  retraitsEpargneValidesNonPayes: number;
  fondsMembresAProteger: number;
  tresorerieApresProtectionMembres: number;
  creditsApprouvesNonDecaisses: number;
  depensesValideesNonPayees: number;
  salairesRestantAPayer?: number;
  transportRestantAPayer?: number;
  fondsMinimumSecurite: number;
  margePrudence: number;
  totalEngagementsCourtTerme: number;
  tresorerieDisponibleApresEngagements: number;
  tresorerieRecuperablePrudente: number;
  soldeCaisseIndicatif: boolean;
  commentairePedagogique?: string | null;
}

export interface FondsMembresProtegesDto {
  epargneDisponibleMembres: number;
  epargneBloqueeGaranties: number;
  retraitsEpargneValidesNonPayesNonInclus: number;
  totalFondsMembres: number;
  commentairePedagogique?: string | null;
}

export interface ApportFinancementDetailDto {
  operationId?: number | null;
  date?: string | null;
  antenneId?: number | null;
  antenne?: string | null;
  categorie?: string | null;
  natureFinancement?: string | null;
  reference?: string | null;
  utilisateur?: string | null;
  montant: number;
  observation?: string | null;
}

export interface DetailMasseSalarialeDto {
  employeId?: number | null;
  matricule?: string | null;
  nomComplet?: string | null;
  poste?: string | null;
  agenceId?: number | null;
  agenceNom?: string | null;
  chargeSiege?: boolean | null;
  salaireBase?: number | null;
  primeFixe?: number | null;
  bonusVariable?: number | null;
  totalEpargneCollecteeValidee?: number | null;
  primeEpargne?: number | null;
  totalRemboursementCollecteValide?: number | null;
  primeRemboursement?: number | null;
  nombreCarnetsVendus?: number | null;
  bonusCarnets?: number | null;
  totalPrimesAgentTerrain?: number | null;
  primesBonusJustifies?: number | null;
  remunerationAttendueTotale?: number | null;
  ecartRemuneration?: number | null;
  motifRemuneration?: string | null;
  salairePrevu: number;
  montantPaye: number;
  salairesPartielsPayes: number;
  avancesPayees: number;
  retenues: number;
  primes: number;
  commissions: number;
  regularisations: number;
  resteAPayer: number;
  statutPaie?: string | null;
  referenceDepenseCaisse?: string | null;
}

export interface MasseSalarialeDto {
  masseSalarialeMensuellePrevue: number;
  salairesPayes: number;
  salairesRestantAPayer: number;
  resultatPrevisionnelApresSalairesAPayer: number;
  nombreEmployesActifs: number;
  periodePaie?: string | null;
  paiementSuperieurAuPrevu?: boolean;
  alerte?: string | null;
  detailsEmployes: DetailMasseSalarialeDto[];
}

export interface TransportFixeDetailDto {
  employeId?: number | null;
  matricule?: string | null;
  nomComplet?: string | null;
  siteId?: number | null;
  siteNom?: string | null;
  agenceId?: number | null;
  agenceNom?: string | null;
  montantJournalierParAgent?: number | null;
  nombreJoursPeriode?: number | null;
  montantPrevu: number;
  montantPaye: number;
  resteAPayer: number;
  statut?: string | null;
  referencesPaiement?: string | null;
}

export interface TransportFixeSiteDetailDto {
  siteId?: number | null;
  siteNom?: string | null;
  agenceId?: number | null;
  agenceNom?: string | null;
  montantJournalierParAgent: number;
  nombreAgentsTerrainActifs: number;
  nombreJoursPeriode: number;
  transportPrevuSite: number;
  transportPayeSite: number;
  transportRestantSite: number;
}

export interface TransportFixePrevuDto {
  totalTransportPrevu: number;
  totalTransportPaye: number;
  totalTransportRestant: number;
  nombreAgentsTerrain: number;
  nombreSitesConfigures: number;
  nombreSitesSansMontant: number;
  nombreJoursPeriode: number;
  periodeCharge?: string | null;
  commentaireCalcul?: string | null;
  detailsParSite: TransportFixeSiteDetailDto[];
  details: TransportFixeDetailDto[];
}

export interface ComparaisonAgenceDto {
  agenceId?: number | null;
  agenceNom?: string | null;
  revenusReels: number;
  chargesConnues: number;
  resultatNetEstime: number;
  margePourcentage?: number | null;
  commissionsRetrait: number;
  fraisCredit: number;
  interetsCredit: number;
  penalitesCredit: number;
  carnetsVendus: number;
  autresRevenus: number;
  salairesPayes: number;
  salairesRestantAPayer: number;
  primes: number;
  transportTerrain: number;
  transportRestantAPayer: number;
  fonctionnement: number;
  achatCarnets: number;
  autresCharges: number;
  collectesValidees: number;
  membresActifs: number;
  creditsActifs: number;
  alerteDeficit: boolean;
  alerteChargesElevees: boolean;
}

export interface SyntheseComparaisonAgencesDto {
  agences: ComparaisonAgenceDto[];
  totalRevenusAgences: number;
  totalChargesAgences: number;
  totalResultatAgences: number;
  chargesGlobalesSiege: number;
  resultatGlobalApresChargesSiege: number;
  agencePlusRevenus?: ComparaisonAgenceDto | null;
  agencePlusRentable?: ComparaisonAgenceDto | null;
  agencePlusCharges?: ComparaisonAgenceDto | null;
  agencesDeficitaires: ComparaisonAgenceDto[];
}

export interface RapportRevenusResponse {
  dateDebut: string;
  dateFin: string;
  totalRevenus: number;
  totalMouvementsNonRevenus?: number;
  totalCharges?: number;
  beneficeNetEstime?: number;
  resultatPrevisionnelApresSalairesAPayer?: number;
  resultatPrevisionnelApresChargesFixes?: number;
  chargesGlobalesSiege?: number;
  resultatApresChargesGlobalesSiege?: number;
  nombreCarnetsVendus?: number;
  montantVentesCarnets?: number;
  coutCarnetDisponible?: boolean;
  coutEstimeCarnets?: number;
  margeCarnets?: number;
  messageMargeCarnets?: string | null;
  carnetMarge?: CarnetMargeDto | null;
  positionCredit?: PositionCreditDto | null;
  apportsFinancements?: ApportFinancementDto | null;
  capaciteRetraitProprietaire?: CapaciteRetraitProprietaireDto | null;
  tresorerieDisponible?: TresorerieDisponibleDto | null;
  masseSalariale?: MasseSalarialeDto | null;
  transportFixePrevu?: TransportFixePrevuDto | null;
  fondsMembresProteges?: FondsMembresProtegesDto | null;
  comparaisonAgences?: SyntheseComparaisonAgencesDto | null;
  apportsFinancementsDetails?: ApportFinancementDetailDto[];
  kpis: RevenuKpiDto;
  parAntenne: RevenuParAntenneDto[];
  parCategorie: RevenuParCategorieDto[];
  mouvementsNonRevenus?: RevenuParCategorieDto[];
  charges?: RevenuParCategorieDto[];
  chargesParCategorie?: ChargeParCategorieDto[];
  chargeDetails?: ChargeDetailDto[];
  mouvementsNonRevenusParAntenne?: MouvementNonRevenuParAntenneDto[];
  controlesCoherence?: ControleCoherenceDto[];
  details: RevenuDetailDto[];
}

export interface RapportRevenusFilters {
  dateDebut: string;
  dateFin: string;
  agenceId?: number | null;
  categorie?: string | null;
  source?: string | null;
}
