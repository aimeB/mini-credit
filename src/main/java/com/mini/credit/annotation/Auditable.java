package com.mini.credit.annotation;

import com.mini.credit.enums.security.AuditAction;
import java.lang.annotation.*;

/**
 * PHASE 4: Annotation to mark methods for automatic audit logging
 * Applied to controller endpoints or service methods that should be audited
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * The audit action to log
     */
    AuditAction action();

    /**
     * Type of entity being affected (e.g., "DemandeCredit", "Credit", "Membre")
     */
    String entityType() default "";

    /**
     * SpEL expression to extract entity ID from method parameters
     * Example: "#demandeId" or "#request.membreId"
     */
    String entityIdExpression() default "";

    /**
     * Whether to include method parameters in audit log
     */
    boolean captureParameters() default false;

    /**
     * Whether to include method return value in audit log
     */
    boolean captureResult() default false;

    /**
     * Custom reason/description (can use SpEL)
     */
    String reason() default "";
}
