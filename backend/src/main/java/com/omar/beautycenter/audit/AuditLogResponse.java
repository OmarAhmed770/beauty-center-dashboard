package com.omar.beautycenter.audit;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String action,
        String targetType,
        String targetId,
        LocalDate businessDate,
        String actor,
        LocalDateTime occurredAt,
        String details) {

    static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getAction(), log.getTargetType(), log.getTargetId(),
                log.getBusinessDate(), log.getActor(), log.getOccurredAt(), log.getDetails());
    }
}
