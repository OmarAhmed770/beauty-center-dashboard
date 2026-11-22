package com.omar.beautycenter.discount;

import com.omar.beautycenter.catalog.CatalogCategory;
import com.omar.beautycenter.catalog.CatalogItem;
import com.omar.beautycenter.common.audit.BaseAuditEntity;
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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "discounts")
public class Discount extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType type;

    @Column(name = "discount_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal value;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "active", nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id")
    private CatalogItem catalogItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CatalogCategory category;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Discount() {
    }

    public Discount(
            String name,
            DiscountType type,
            BigDecimal value,
            LocalDate startDate,
            LocalDate endDate,
            boolean active,
            CatalogItem catalogItem,
            CatalogCategory category) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
        this.catalogItem = catalogItem;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DiscountType getType() {
        return type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isActive() {
        return active;
    }

    public CatalogItem getCatalogItem() {
        return catalogItem;
    }

    public CatalogCategory getCategory() {
        return category;
    }

    public long getVersion() {
        return version;
    }

    public void update(
            String name,
            DiscountType type,
            BigDecimal value,
            LocalDate startDate,
            LocalDate endDate,
            CatalogItem catalogItem,
            CatalogCategory category) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.startDate = startDate;
        this.endDate = endDate;
        this.catalogItem = catalogItem;
        this.category = category;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
