package com.omar.beautycenter.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.common.error.BusinessRuleException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditorAware<String> auditorAware;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            AuditorAware<String> auditorAware,
            ObjectMapper objectMapper,
            Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.auditorAware = auditorAware;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void record(
            String action,
            String targetType,
            Object targetId,
            LocalDate businessDate,
            Map<String, ?> details) {
        auditLogRepository.save(new AuditLog(
                action,
                targetType,
                String.valueOf(targetId),
                businessDate,
                auditorAware.getCurrentAuditor().orElse("SYSTEM"),
                LocalDateTime.now(clock),
                toJson(details)));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('OWNER')")
    public PageResponse<AuditLogResponse> search(
            String action,
            String targetType,
            String actor,
            LocalDate businessDateFrom,
            LocalDate businessDateTo,
            LocalDateTime occurredFrom,
            LocalDateTime occurredTo,
            int page,
            int size) {
        if (businessDateFrom != null && businessDateTo != null
                && businessDateFrom.isAfter(businessDateTo)) {
            throw new BusinessRuleException("businessDateFrom must be on or before businessDateTo.");
        }
        if (occurredFrom != null && occurredTo != null && occurredFrom.isAfter(occurredTo)) {
            throw new BusinessRuleException("occurredFrom must be on or before occurredTo.");
        }
        return PageResponse.from(auditLogRepository.search(
                normalized(action), normalized(targetType), normalized(actor),
                businessDateFrom, businessDateTo, occurredFrom, occurredTo,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))))
                .map(AuditLogResponse::from));
    }

    private static String normalized(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String toJson(Map<String, ?> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(details);
            if (json.length() > 4000) {
                throw new IllegalStateException("Audit details exceed the supported size.");
            }
            return json;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize audit details.", exception);
        }
    }
}
