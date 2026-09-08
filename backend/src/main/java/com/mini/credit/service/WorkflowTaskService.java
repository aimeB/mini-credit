package com.mini.credit.service;

import com.mini.credit.dto.workflow.WorkflowTaskDashboardDTO;
import com.mini.credit.dto.workflow.WorkflowTaskItemDTO;

import java.util.List;

public interface WorkflowTaskService {

    List<WorkflowTaskItemDTO> getMyActions(String statut);

    List<WorkflowTaskItemDTO> getSupervisionActions(String statut);

    long getMyActionCount();

    WorkflowTaskDashboardDTO getDashboard();

    WorkflowTaskItemDTO markAsViewed(Long taskId, String commentaire);

    WorkflowTaskItemDTO complete(Long taskId, String commentaire);

    void onSessionPreCloturee(Long sessionId, String referenceMetier, Long antenneId, Long siteId);

    void onSessionControleValide(Long sessionId, String referenceMetier, Long antenneId, Long siteId);

    void onSessionCloturee(Long sessionId, String referenceMetier, Long antenneId, Long siteId);

    void onRecetteSoumise(Long recetteId, String referenceMetier, Long antenneId, Long siteId);

    void onRecetteValidee(Long recetteId, String referenceMetier, Long antenneId, Long siteId);

    void onRecetteRejetee(Long recetteId, String referenceMetier, Long antenneId, Long siteId);

    void onRecetteEcartConstate(Long recetteId, String referenceMetier, Long antenneId, Long siteId);

    void onCollecteSoumise(Long collecteId, String referenceMetier, Long antenneId, Long siteId);

    void onCollecteBilletageConfirme(Long collecteId, String referenceMetier, Long antenneId, Long siteId);

    void onCollecteCloturee(Long collecteId, String referenceMetier, Long antenneId, Long siteId);

    void onDemandeCreditSoumise(Long demandeId, String referenceMetier, Long antenneId, Long siteId);

    void onDemandeCreditPreAnalyseValidee(Long demandeId, String referenceMetier, Long antenneId, Long siteId);

    void onDemandeCreditAnalyseTerrainValidee(Long demandeId, String referenceMetier, Long antenneId, Long siteId);

    void onDemandeCreditValidationChef(Long demandeId, String referenceMetier, Long antenneId, Long siteId);

    void onDemandeCreditApprouvee(Long demandeId, Long creditId, String referenceMetier, Long antenneId, Long siteId);

    void onCreditDecaisse(Long creditId, String referenceMetier, Long antenneId, Long siteId);

    void onDemandeCreditRejetee(Long demandeId, String referenceMetier, Long antenneId, Long siteId);

    void onRetraitDemande(Long retraitId, String referenceMetier, Long antenneId, Long siteId);

    void onRetraitApprouve(Long retraitId, String referenceMetier, Long antenneId, Long siteId);

    void onRetraitPaye(Long retraitId, String referenceMetier, Long antenneId, Long siteId);

    void onRetraitRejete(Long retraitId, String referenceMetier, Long antenneId, Long siteId);
}
