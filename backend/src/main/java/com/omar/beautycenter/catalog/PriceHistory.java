package com.omar.beautycenter.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "price_history")
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_item_id", nullable = false, updatable = false)
    private CatalogItem catalogItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, updatable = false, length = 20)
    private PriceValueType valueType;

    @Column(name = "old_value", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal oldValue;

    @Column(name = "new_value", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal newValue;

    @Column(name = "effective_at", nullable = false, updatable = false)
    private LocalDateTime effectiveAt;

    @Column(name = "change_reason", nullable = false, updatable = false, length = 500)
    private String changeReason;

    @Column(name = "changed_by", nullable = false, updatable = false, length = 100)
    private String changedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PriceHistory() {
    }

    public PriceHistory(
            CatalogItem catalogItem,
            PriceValueType valueType,
            BigDecimal oldValue,
            BigDecimal newValue,
            LocalDateTime effectiveAt,
            String changeReason,
            String changedBy) {
        this.catalogItem = catalogItem;
        this.valueType = valueType;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.effectiveAt = effectiveAt;
        this.changeReason = changeReason;
        this.changedBy = changedBy;
        this.createdAt = effectiveAt;
    }

    public Long getId() {
        return id;
    }

    public CatalogItem getCatalogItem() {
        return catalogItem;
    }

    public PriceValueType getValueType() {
        return valueType;
    }

    public BigDecimal getOldValue() {
        return oldValue;
    }

    public BigDecimal getNewValue() {
        return newValue;
    }

    public LocalDateTime getEffectiveAt() {
        return effectiveAt;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
