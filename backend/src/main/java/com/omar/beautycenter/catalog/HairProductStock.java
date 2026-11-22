package com.omar.beautycenter.catalog;

import com.omar.beautycenter.common.audit.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;

@Entity
@Table(name = "hair_product_stocks", uniqueConstraints = @UniqueConstraint(
        name = "uk_hair_product_stocks_item", columnNames = "hair_item_id"))
public class HairProductStock extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hair_item_id", nullable = false, updatable = false)
    private CatalogItem hairItem;

    @Column(name = "available_grams", nullable = false, precision = 19, scale = 3)
    private BigDecimal availableGrams;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected HairProductStock() {
    }

    public HairProductStock(CatalogItem hairItem, BigDecimal availableGrams) {
        this.hairItem = hairItem;
        this.availableGrams = availableGrams;
    }

    public Long getId() { return id; }
    public CatalogItem getHairItem() { return hairItem; }
    public BigDecimal getAvailableGrams() { return availableGrams; }
    public long getVersion() { return version; }

    public void adjust(BigDecimal adjustmentGrams) {
        this.availableGrams = this.availableGrams.add(adjustmentGrams);
    }
}
