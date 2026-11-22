package com.omar.beautycenter.audit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends Repository<AuditLog, Long> {

    <S extends AuditLog> S save(S auditLog);

    @Query("""
            SELECT log FROM AuditLog log
            WHERE (:action IS NULL OR log.action = :action)
              AND (:targetType IS NULL OR log.targetType = :targetType)
              AND (:actor IS NULL OR LOWER(log.actor) = LOWER(:actor))
              AND (:businessDateFrom IS NULL OR log.businessDate >= :businessDateFrom)
              AND (:businessDateTo IS NULL OR log.businessDate <= :businessDateTo)
              AND (:occurredFrom IS NULL OR log.occurredAt >= :occurredFrom)
              AND (:occurredTo IS NULL OR log.occurredAt <= :occurredTo)
            """)
    Page<AuditLog> search(
            @Param("action") String action,
            @Param("targetType") String targetType,
            @Param("actor") String actor,
            @Param("businessDateFrom") LocalDate businessDateFrom,
            @Param("businessDateTo") LocalDate businessDateTo,
            @Param("occurredFrom") LocalDateTime occurredFrom,
            @Param("occurredTo") LocalDateTime occurredTo,
            Pageable pageable);
}
