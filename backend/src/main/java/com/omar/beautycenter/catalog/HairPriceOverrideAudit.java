package com.omar.beautycenter.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hair_price_override_audits")
public class HairPriceOverrideAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hair_configuration_id", nullable = false, updatable = false)
    private HairConfiguration hairConfiguration;

    @Column(name = "quantity", nullable = false, updatable = false, precision = 19, scale = 3)
    private BigDecimal quantity;

    @Column(name = "original_amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "overridden_amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal overriddenAmount;

    @Column(name = "override_reason", nullable = false, updatable = false, length = 500)
    private String overrideReason;

    @Column(name = "authorized_by", nullable = false, updatable = false, length = 100)
    private String authorizedBy;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    protected HairPriceOverrideAudit() {
    }

    public HairPriceOverrideAudit(
            HairConfiguration hairConfiguration,
            BigDecimal quantity,
            BigDecimal originalAmount,
            BigDecimal overriddenAmount,
            String overrideReason,
            String authorizedBy,
            LocalDateTime occurredAt) {
        this.hairConfiguration = hairConfiguration;
        this.quantity = quantity;
        this.originalAmount = originalAmount;
        this.overriddenAmount = overriddenAmount;
        this.overrideReason = overrideReason;
        this.authorizedBy = authorizedBy;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public HairConfiguration getHairConfiguration() {
        return hairConfiguration;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public BigDecimal getOverriddenAmount() {
        return overriddenAmount;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public String getAuthorizedBy() {
        return authorizedBy;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
