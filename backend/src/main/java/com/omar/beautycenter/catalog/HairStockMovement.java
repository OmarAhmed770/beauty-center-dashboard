package com.omar.beautycenter.catalog;

import com.omar.beautycenter.sales.InvoiceItem;
import com.omar.beautycenter.sales.InvoiceRefund;
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
@Table(name = "hair_stock_movements")
public class HairStockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hair_item_id", nullable = false, updatable = false)
    private CatalogItem hairItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_item_id", updatable = false)
    private InvoiceItem invoiceItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id", updatable = false)
    private InvoiceRefund refund;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30, updatable = false)
    private HairStockMovementType movementType;

    @Column(name = "event_key", nullable = false, unique = true, length = 160, updatable = false)
    private String eventKey;

    @Column(name = "previous_grams", nullable = false, precision = 19, scale = 3, updatable = false)
    private BigDecimal previousGrams;

    @Column(name = "adjustment_grams", nullable = false, precision = 19, scale = 3, updatable = false)
    private BigDecimal adjustmentGrams;

    @Column(name = "new_grams", nullable = false, precision = 19, scale = 3, updatable = false)
    private BigDecimal newGrams;

    @Column(name = "reason", nullable = false, length = 500, updatable = false)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 100, updatable = false)
    private String createdBy;

    protected HairStockMovement() {
    }

    public HairStockMovement(
            CatalogItem hairItem,
            InvoiceItem invoiceItem,
            InvoiceRefund refund,
            HairStockMovementType movementType,
            String eventKey,
            BigDecimal previousGrams,
            BigDecimal adjustmentGrams,
            BigDecimal newGrams,
            String reason,
            LocalDateTime createdAt,
            String createdBy) {
        this.hairItem = hairItem;
        this.invoiceItem = invoiceItem;
        this.refund = refund;
        this.movementType = movementType;
        this.eventKey = eventKey;
        this.previousGrams = previousGrams;
        this.adjustmentGrams = adjustmentGrams;
        this.newGrams = newGrams;
        this.reason = reason;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public CatalogItem getHairItem() { return hairItem; }
    public InvoiceItem getInvoiceItem() { return invoiceItem; }
    public InvoiceRefund getRefund() { return refund; }
    public HairStockMovementType getMovementType() { return movementType; }
    public String getEventKey() { return eventKey; }
    public BigDecimal getPreviousGrams() { return previousGrams; }
    public BigDecimal getAdjustmentGrams() { return adjustmentGrams; }
    public BigDecimal getNewGrams() { return newGrams; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
}
