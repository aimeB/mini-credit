package com.mini.credit.dto.rapport.revenus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RapportRevenusResponse {
    private LocalDate dateDebut;
    private LocalDate dateFin;
    @Builder.Default
    private BigDecimal totalRevenus = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalMouvementsNonRevenus = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalCharges = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal beneficeNetEstime = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal resultatPrevisionnelApresSalairesAPayer = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal resultatPrevisionnelApresChargesFixes = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal chargesGlobalesSiege = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal resultatApresChargesGlobalesSiege = BigDecimal.ZERO;
    @Builder.Default
    private Integer nombreCarnetsVendus = 0;
    @Builder.Default
    private BigDecimal montantVentesCarnets = BigDecimal.ZERO;
    @Builder.Default
    private Boolean coutCarnetDisponible = false;
    @Builder.Default
    private BigDecimal coutEstimeCarnets = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal margeCarnets = BigDecimal.ZERO;
    private String messageMargeCarnets;
    @Builder.Default
    private CarnetMargeDto carnetMarge = CarnetMargeDto.builder().build();
    @Builder.Default
    private PositionCreditDto positionCredit = PositionCreditDto.builder().build();
    @Builder.Default
    private ApportFinancementDto apportsFinancements = ApportFinancementDto.builder().build();
    @Builder.Default
    private CapaciteRetraitProprietaireDto capaciteRetraitProprietaire = CapaciteRetraitProprietaireDto.builder().build();
    @Builder.Default
    private TresorerieDisponibleDto tresorerieDisponible = TresorerieDisponibleDto.builder().build();
    @Builder.Default
    private MasseSalarialeDto masseSalariale = MasseSalarialeDto.builder().build();
    @Builder.Default
    private TransportFixePrevuDto transportFixePrevu = TransportFixePrevuDto.builder().build();
    @Builder.Default
    private FondsMembresProtegesDto fondsMembresProteges = FondsMembresProtegesDto.builder().build();
    @Builder.Default
    private SyntheseComparaisonAgencesDto comparaisonAgences = SyntheseComparaisonAgencesDto.builder().build();
    @Builder.Default
    private List<ApportFinancementDetailDto> apportsFinancementsDetails = new ArrayList<>();
    @Builder.Default
    private RevenuKpiDto kpis = RevenuKpiDto.builder().build();
    @Builder.Default
    private List<RevenuParAntenneDto> parAntenne = new ArrayList<>();
    @Builder.Default
    private List<RevenuParCategorieDto> parCategorie = new ArrayList<>();
    @Builder.Default
    private List<RevenuParCategorieDto> mouvementsNonRevenus = new ArrayList<>();
    @Builder.Default
    private List<RevenuParCategorieDto> charges = new ArrayList<>();
    @Builder.Default
    private List<ChargeParCategorieDto> chargesParCategorie = new ArrayList<>();
    @Builder.Default
    private List<ChargeDetailDto> chargeDetails = new ArrayList<>();
    @Builder.Default
    private List<MouvementNonRevenuParAntenneDto> mouvementsNonRevenusParAntenne = new ArrayList<>();
    @Builder.Default
    private List<ControleCoherenceDto> controlesCoherence = new ArrayList<>();
    @Builder.Default
    private List<RevenuDetailDto> details = new ArrayList<>();
}
