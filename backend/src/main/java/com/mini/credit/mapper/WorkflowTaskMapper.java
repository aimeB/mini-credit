package com.mini.credit.mapper;

import com.mini.credit.dto.workflow.WorkflowTaskItemDTO;
import com.mini.credit.entity.workflow.WorkflowTask;
import org.springframework.stereotype.Component;

@Component
public class WorkflowTaskMapper {

    public WorkflowTaskItemDTO toDto(WorkflowTask task) {
        if (task == null) {
            return null;
        }

        return WorkflowTaskItemDTO.builder()
                .id(task.getId())
                .typeAction(task.getTypeAction())
                .module(task.getModule())
                .referenceMetier(task.getReferenceMetier())
                .entityType(task.getEntityType())
                .entityId(task.getEntityId())
                .titre(task.getTitre())
                .description(task.getDescription())
                .type(task.getTypeAction() != null ? task.getTypeAction().name() : null)
                .roleAttendu(task.getRoleDestinataire() != null ? task.getRoleDestinataire().name() : null)
                .actionAttendue(resolveActionAttendue(task))
                .route(resolveRoute(task))
                .roleDestinataire(task.getRoleDestinataire())
                .utilisateurDestinataireId(task.getUtilisateurDestinataire() != null ? task.getUtilisateurDestinataire().getId() : null)
                .antenneId(task.getAntenneId())
                .siteId(task.getSiteId())
                .priorite(task.getPriorite())
                .statut(task.getStatut())
                .dateCreation(task.getDateCreation())
                .dateEcheance(task.getDateEcheance())
                .createdById(task.getCreatedBy() != null ? task.getCreatedBy().getId() : null)
                .completedById(task.getCompletedBy() != null ? task.getCompletedBy().getId() : null)
                .completedAt(task.getCompletedAt())
                .commentaire(task.getCommentaire())
                .build();
    }

    private String resolveActionAttendue(WorkflowTask task) {
        if (task.getTypeAction() == null) {
            return null;
        }
        return switch (task.getTypeAction()) {
            case PRE_CLOTURER_SESSION_CAISSE -> "Pré-clôturer / clôturer la session caisse";
            case DEPENSE_CAISSE_VALIDATE -> "Valider / autoriser la dépense";
            case DEPENSE_CAISSE_PAY -> "Payer la dépense";
            case CONTROLER_SESSION_CAISSE -> "Contrôler la session caisse";
            case CLOTURER_SESSION_CAISSE -> "Clôturer définitivement la session";
            case EFFECTUER_BILLETAGE -> "COLLECTE_TERRAIN".equals(task.getEntityType()) ? "Effectuer le billetage de la collecte soumise" : "Effectuer le billetage";
            case CONTROLER_RECETTE_TERRAIN -> "COLLECTE_TERRAIN".equals(task.getEntityType()) ? "Contrôler la collecte après billetage" : "Contrôler la recette terrain";
            case VALIDER_RETRAIT_EPARGNE -> "Valider la demande de retrait";
            case PAYER_RETRAIT_EPARGNE -> "Payer le retrait épargne";
            case TRAITER_DEMANDE_CREDIT -> "Pré-analyser la demande de crédit";
            case CONTROLER_DEMANDE_CREDIT -> "Contrôler la demande de crédit";
            case APPROUVER_DEMANDE_CREDIT -> "Approuver la demande de crédit";
            case DECAISSER_CREDIT -> "Décaisser le crédit";
        };
    }

    private String resolveRoute(WorkflowTask task) {
        if (task.getTypeAction() == null) {
            return null;
        }
        return switch (task.getTypeAction()) {
            case PRE_CLOTURER_SESSION_CAISSE -> task.getEntityId() != null ? "/caisses/session/" + task.getEntityId() : "/caisses";
            case DEPENSE_CAISSE_VALIDATE, DEPENSE_CAISSE_PAY -> "/caisses/depenses";
            case CONTROLER_SESSION_CAISSE, CLOTURER_SESSION_CAISSE -> task.getEntityId() != null ? "/caisses/sessions/" + task.getEntityId() : "/caisses/controle";
            case EFFECTUER_BILLETAGE -> "COLLECTE_TERRAIN".equals(task.getEntityType()) ? "/collectes/billetage" : "/collectes/soumises-billetage";
            case CONTROLER_RECETTE_TERRAIN -> "COLLECTE_TERRAIN".equals(task.getEntityType()) ? "/collectes/a-controler" : (task.getEntityId() != null ? "/recettes/" + task.getEntityId() + "/valider" : "/recettes");
            case VALIDER_RETRAIT_EPARGNE, PAYER_RETRAIT_EPARGNE -> task.getEntityId() != null ? "/epargne/demandes-retrait/" + task.getEntityId() : "/epargne";
            case TRAITER_DEMANDE_CREDIT -> "/credits/demandes";
            case CONTROLER_DEMANDE_CREDIT -> task.getEntityId() != null ? "/credits/demandes/" + task.getEntityId() : "/credits/demandes";
            case APPROUVER_DEMANDE_CREDIT -> task.getEntityId() != null ? "/credits/demandes?demandeId=" + task.getEntityId() : "/credits/demandes";
            case DECAISSER_CREDIT -> task.getEntityId() != null ? "/credits/" + task.getEntityId() + "/decaissement" : "/credits/liste";
        };
    }
}
