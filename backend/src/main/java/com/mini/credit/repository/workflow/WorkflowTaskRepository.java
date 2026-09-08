package com.mini.credit.repository.workflow;

import com.mini.credit.entity.workflow.WorkflowTask;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.enums.workflow.WorkflowTaskModule;
import com.mini.credit.enums.workflow.WorkflowTaskStatus;
import com.mini.credit.enums.workflow.WorkflowTaskTypeAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, Long> {

    Optional<WorkflowTask> findByActiveKey(String activeKey);

    List<WorkflowTask> findByStatutInOrderByDateCreationDesc(Collection<WorkflowTaskStatus> statuts);

    List<WorkflowTask> findByRoleDestinataireAndAntenneIdAndStatutInOrderByDateCreationDesc(
            RoleCode roleDestinataire,
            Long antenneId,
            Collection<WorkflowTaskStatus> statuts
    );

    List<WorkflowTask> findByRoleDestinataireAndStatutInOrderByDateCreationDesc(
            RoleCode roleDestinataire,
            Collection<WorkflowTaskStatus> statuts
    );

    List<WorkflowTask> findByUtilisateurDestinataireIdAndStatutInOrderByDateCreationDesc(
            Long utilisateurDestinataireId,
            Collection<WorkflowTaskStatus> statuts
    );

    long countByRoleDestinataireAndAntenneIdAndStatutIn(
            RoleCode roleDestinataire,
            Long antenneId,
            Collection<WorkflowTaskStatus> statuts
    );

    long countByUtilisateurDestinataireIdAndStatutIn(Long utilisateurDestinataireId, Collection<WorkflowTaskStatus> statuts);

    List<WorkflowTask> findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            WorkflowTaskModule module,
            String entityType,
            Long entityId,
            RoleCode roleDestinataire,
            Collection<WorkflowTaskStatus> statuts
    );

    List<WorkflowTask> findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireOrderByDateCreationDesc(
            WorkflowTaskModule module,
            String entityType,
            Long entityId,
            RoleCode roleDestinataire
    );

    List<WorkflowTask> findByModuleAndEntityTypeAndEntityIdAndStatutIn(
            WorkflowTaskModule module,
            String entityType,
            Long entityId,
            Collection<WorkflowTaskStatus> statuts
    );

    List<WorkflowTask> findTop5ByStatutInOrderByDateCreationDesc(Collection<WorkflowTaskStatus> statuts);
}
