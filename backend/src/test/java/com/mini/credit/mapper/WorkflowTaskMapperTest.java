package com.mini.credit.mapper;

import com.mini.credit.dto.workflow.WorkflowTaskItemDTO;
import com.mini.credit.entity.workflow.WorkflowTask;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.enums.workflow.WorkflowTaskModule;
import com.mini.credit.enums.workflow.WorkflowTaskStatus;
import com.mini.credit.enums.workflow.WorkflowTaskTypeAction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowTaskMapperTest {

    private final WorkflowTaskMapper mapper = new WorkflowTaskMapper();

    @Test
    void collecteTerrainTaskShouldMapToCollecteControlActionAndRoute() {
        WorkflowTask task = WorkflowTask.builder()
                .typeAction(WorkflowTaskTypeAction.CONTROLER_RECETTE_TERRAIN)
                .module(WorkflowTaskModule.RECETTE)
                .entityType("COLLECTE_TERRAIN")
                .entityId(1401L)
                .roleDestinataire(RoleCode.CONTROLEUR)
                .statut(WorkflowTaskStatus.A_FAIRE)
                .build();

        WorkflowTaskItemDTO dto = mapper.toDto(task);

        assertThat(dto.getActionAttendue()).isEqualTo("Contrôler la collecte après billetage");
        assertThat(dto.getRoute()).isEqualTo("/collectes/a-controler");
        assertThat(dto.getRoleAttendu()).isEqualTo("CONTROLEUR");
    }

    @Test
    void collecteTerrainBilletageTaskShouldMapToBilletageActionAndRoute() {
        WorkflowTask task = WorkflowTask.builder()
                .typeAction(WorkflowTaskTypeAction.EFFECTUER_BILLETAGE)
                .module(WorkflowTaskModule.RECETTE)
                .entityType("COLLECTE_TERRAIN")
                .entityId(1401L)
                .roleDestinataire(RoleCode.CAISSIER)
                .statut(WorkflowTaskStatus.A_FAIRE)
                .build();

        WorkflowTaskItemDTO dto = mapper.toDto(task);

        assertThat(dto.getActionAttendue()).isEqualTo("Effectuer le billetage de la collecte soumise");
        assertThat(dto.getRoute()).isEqualTo("/collectes/billetage");
        assertThat(dto.getRoleAttendu()).isEqualTo("CAISSIER");
    }

    @Test
    void decaisserCreditTaskShouldMapToDecaissementRoute() {
        WorkflowTask task = WorkflowTask.builder()
                .typeAction(WorkflowTaskTypeAction.DECAISSER_CREDIT)
                .module(WorkflowTaskModule.CREDIT)
                .entityType("CREDIT")
                .entityId(5L)
                .roleDestinataire(RoleCode.CAISSIER)
                .statut(WorkflowTaskStatus.A_FAIRE)
                .build();

        WorkflowTaskItemDTO dto = mapper.toDto(task);

        assertThat(dto.getActionAttendue()).isEqualTo("Décaisser le crédit");
        assertThat(dto.getRoute()).isEqualTo("/credits/5/decaissement");
        assertThat(dto.getRoleAttendu()).isEqualTo("CAISSIER");
    }
}