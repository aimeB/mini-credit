CREATE TABLE workflow_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_action VARCHAR(50) NOT NULL,
    module VARCHAR(30) NOT NULL,
    reference_metier VARCHAR(120) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id BIGINT NOT NULL,
    titre VARCHAR(180) NOT NULL,
    description VARCHAR(1000) NULL,
    role_destinataire VARCHAR(50) NOT NULL,
    utilisateur_destinataire_id BIGINT NULL,
    antenne_id BIGINT NOT NULL,
    site_id BIGINT NULL,
    priorite VARCHAR(20) NOT NULL,
    statut VARCHAR(20) NOT NULL,
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification DATETIME NULL,
    date_echeance DATETIME NULL,
    created_by BIGINT NULL,
    completed_by BIGINT NULL,
    completed_at DATETIME NULL,
    commentaire VARCHAR(1000) NULL,
    active_key VARCHAR(260) NULL,
    CONSTRAINT fk_workflow_task_user_dest FOREIGN KEY (utilisateur_destinataire_id) REFERENCES utilisateur(id),
    CONSTRAINT fk_workflow_task_created_by FOREIGN KEY (created_by) REFERENCES utilisateur(id),
    CONSTRAINT fk_workflow_task_completed_by FOREIGN KEY (completed_by) REFERENCES utilisateur(id),
    CONSTRAINT uk_workflow_task_active_key UNIQUE (active_key)
);

CREATE INDEX idx_workflow_task_status_date ON workflow_task(statut, date_creation);
CREATE INDEX idx_workflow_task_role_antenne ON workflow_task(role_destinataire, antenne_id, statut);
CREATE INDEX idx_workflow_task_user_dest ON workflow_task(utilisateur_destinataire_id, statut);
CREATE INDEX idx_workflow_task_module ON workflow_task(module, statut);
CREATE INDEX idx_workflow_task_entity ON workflow_task(module, entity_type, entity_id);
