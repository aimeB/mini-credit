package com.mini.credit.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.credit.annotation.Auditable;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * PHASE 4: AOP Aspect for automatic audit logging
 * Intercepts methods annotated with @Auditable and logs operations
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Around("@annotation(auditable)")
    public Object auditOperation(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // Extract method parameters
        Object[] args = joinPoint.getArgs();
        String[] paramNames = getParameterNames(joinPoint);

        // Build SpEL context for entity ID extraction
        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < args.length; i++) {
            if (paramNames.length > i) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        Long entityId = null;
        try {
            if (auditable.entityIdExpression() != null && !auditable.entityIdExpression().isEmpty()) {
                Object idValue = expressionParser.parseExpression(auditable.entityIdExpression())
                    .getValue(context);
                if (idValue instanceof Number) {
                    entityId = ((Number) idValue).longValue();
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract entity ID from expression: {}", auditable.entityIdExpression(), e);
        }

        // Get request info
        String ipAddress = getClientIpAddress();
        String userAgent = getUserAgent();
        String endpoint = getEndpoint();

        // Prepare old values (for updates)
        String oldValuesJson = null;
        String newValuesJson = null;

        try {
            // Execute the actual method
            Object result = joinPoint.proceed();

            // Capture result if requested
            if (auditable.captureResult() && result != null) {
                try {
                    newValuesJson = objectMapper.writeValueAsString(result);
                } catch (Exception e) {
                    log.warn("Could not serialize result for audit", e);
                }
            }

            // Log success
            long duration = System.currentTimeMillis() - startTime;
            auditService.log(
                auditable.action(),
                auditable.entityType(),
                entityId,
                true,  // success
                auditable.reason(),
                null,  // referenceNumber
                oldValuesJson,
                newValuesJson,
                null   // no error
            );

            log.debug("Audit logged for {}.{} - SUCCESS", className, methodName);
            return result;

        } catch (Throwable e) {
            // Log failure
            long duration = System.currentTimeMillis() - startTime;
            auditService.log(
                auditable.action(),
                auditable.entityType(),
                entityId,
                false, // failure
                auditable.reason(),
                null,  // referenceNumber
                oldValuesJson,
                null,  // newValuesJson (operation failed)
                e.getMessage()
            );

            log.debug("Audit logged for {}.{} - FAILURE: {}", className, methodName, e.getMessage());
            throw e;
        }
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            log.debug("Could not get client IP", e);
        }
        return "UNKNOWN";
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest().getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.debug("Could not get User-Agent", e);
        }
        return "UNKNOWN";
    }

    private String getEndpoint() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getMethod() + " " + request.getRequestURI();
            }
        } catch (Exception e) {
            log.debug("Could not get endpoint", e);
        }
        return "UNKNOWN";
    }

    private String[] getParameterNames(ProceedingJoinPoint joinPoint) {
        try {
            return ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature())
                .getParameterNames();
        } catch (Exception e) {
            log.debug("Could not get parameter names", e);
            return new String[0];
        }
    }
}
