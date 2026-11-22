package com.omar.beautycenter.catalog;

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

@Entity
@Table(name = "catalog_items")
public class CatalogItem extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, updatable = false, length = 50)
    private String code;

    @Column(name = "name_arabic", nullable = false, length = 150)
    private String nameArabic;

    @Column(name = "name_english", nullable = false, length = 150)
    private String nameEnglish;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, updatable = false, length = 30)
    private CatalogItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CatalogCategory category;

    @Column(name = "current_selling_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentSellingPrice;

    @Column(name = "current_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentCost;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "description", length = 2000)
    private String description;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected CatalogItem() {
    }

    public CatalogItem(
            String code,
            String nameArabic,
            String nameEnglish,
            CatalogItemType itemType,
            CatalogCategory category,
            BigDecimal currentSellingPrice,
            BigDecimal currentCost,
            boolean active,
            String description) {
        this.code = code;
        this.nameArabic = nameArabic;
        this.nameEnglish = nameEnglish;
        this.itemType = itemType;
        this.category = category;
        this.currentSellingPrice = currentSellingPrice;
        this.currentCost = currentCost;
        this.active = active;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getNameArabic() {
        return nameArabic;
    }

    public String getNameEnglish() {
        return nameEnglish;
    }

    public CatalogItemType getItemType() {
        return itemType;
    }

    public CatalogCategory getCategory() {
        return category;
    }

    public BigDecimal getCurrentSellingPrice() {
        return currentSellingPrice;
    }

    public BigDecimal getCurrentCost() {
        return currentCost;
    }

    public boolean isActive() {
        return active;
    }

    public String getDescription() {
        return description;
    }

    public long getVersion() {
        return version;
    }

    public void update(
            String nameArabic,
            String nameEnglish,
            CatalogCategory category,
            String description) {
        this.nameArabic = nameArabic;
        this.nameEnglish = nameEnglish;
        this.category = category;
        this.description = description;
    }

    public void updateSellingPrice(BigDecimal currentSellingPrice) {
        this.currentSellingPrice = currentSellingPrice;
    }

    public void updateCost(BigDecimal currentCost) {
        this.currentCost = currentCost;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
