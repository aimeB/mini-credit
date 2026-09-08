package com.mini.credit.dto.caisse;

import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.TypeOperationCaisse;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class JournalCaisseFilterRequest {
    private Long sessionCaisseId;
    private Long caisseId;
    private Long siteId;
    private Long utilisateurId;
    private TypeOperationCaisse typeOperation;
    private CategorieOperationCaisse categorie;
    private SourceOperationCaisse source;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String referenceMetier;
    private Long recetteId;
    private Long depenseCaisseId;
    private Long creditId;
    private Long retraitEpargneId;
    private Long operationEpargneId;
}