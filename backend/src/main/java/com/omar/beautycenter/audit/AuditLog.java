package com.omar.beautycenter.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action", nullable = false, length = 100, updatable = false)
    private String action;

    @Column(name = "target_type", nullable = false, length = 100, updatable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false, length = 100, updatable = false)
    private String targetId;

    @Column(name = "business_date", updatable = false)
    private LocalDate businessDate;

    @Column(name = "actor", nullable = false, length = 100, updatable = false)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "details", length = 4000, updatable = false)
    private String details;

    protected AuditLog() {
    }

    public AuditLog(
            String action,
            String targetType,
            String targetId,
            LocalDate businessDate,
            String actor,
            LocalDateTime occurredAt,
            String details) {
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.businessDate = businessDate;
        this.actor = actor;
        this.occurredAt = occurredAt;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public LocalDate getBusinessDate() { return businessDate; }
    public String getActor() { return actor; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getDetails() { return details; }
}
