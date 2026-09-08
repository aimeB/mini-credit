package com.mini.credit.repository.projection;

public interface OperationCaisseDiagnosticProjection {

    Long getOperationId();

    Long getSessionId();

    Long getCaisseId();

    Long getSiteId();
}