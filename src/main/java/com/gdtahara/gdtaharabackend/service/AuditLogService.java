package com.gdtahara.gdtaharabackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gdtahara.gdtaharabackend.model.AuditLog;
import com.gdtahara.gdtaharabackend.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final TransactionTemplate requiresNewTx;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           PlatformTransactionManager txManager) {
        this.auditLogRepository = auditLogRepository;
        // Use TransactionTemplate so the try/catch below covers both save() AND commit().
        // @Transactional(REQUIRES_NEW) + try/catch-inside does NOT protect the caller:
        // if save() marks the session rollback-only, Spring commits *after* the catch block
        // and throws UnexpectedRollbackException which propagates to the caller's transaction.
        this.requiresNewTx = new TransactionTemplate(txManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    public void log(String action, String entityType, Long entityId, Object before, Object after) {
        try {
            // Collect context before starting the new transaction
            // (SecurityContext and MDC are thread-local and still accessible here)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal()))
                    ? auth.getName() : "system";

            String requestId = MDC.get("requestId");
            String endpoint = null;
            String ipAddress = null;
            try {
                ServletRequestAttributes attrs =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest req = attrs.getRequest();
                    endpoint = req.getMethod() + " " + req.getRequestURI();
                    ipAddress = getClientIp(req);
                }
            } catch (Exception ignore) {}

            final String u   = username;
            final String ep  = endpoint;
            final String ip  = ipAddress;
            final String rid = requestId;
            final String beforeJson = toJson(before);
            final String afterJson  = toJson(after);

            // requiresNewTx.execute() suspends the caller's transaction, runs its own,
            // and commits/rolls-back completely before returning.
            // Any exception (including commit failure) is thrown HERE, inside our try/catch,
            // so the caller's transaction is never touched.
            requiresNewTx.execute(status -> {
                AuditLog entry = new AuditLog();
                entry.setUsername(u);
                entry.setAction(action);
                entry.setEntityType(entityType);
                entry.setEntityId(entityId);
                entry.setRequestId(rid);
                entry.setEndpoint(ep);
                entry.setIpAddress(ip);
                entry.setBeforeJson(beforeJson);
                entry.setAfterJson(afterJson);
                auditLogRepository.save(entry);
                return null;
            });

        } catch (Exception e) {
            // Audit log failure must never affect the business transaction
            logger.error("AuditLog write failed ({} {} id={}): {}",
                    action, entityType, entityId, e.getMessage());
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\":\"serialization_failed\",\"type\":\""
                    + obj.getClass().getSimpleName() + "\"}";
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfFor = request.getHeader("X-Forwarded-For");
        if (xfFor != null && !xfFor.isBlank()) {
            return xfFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
