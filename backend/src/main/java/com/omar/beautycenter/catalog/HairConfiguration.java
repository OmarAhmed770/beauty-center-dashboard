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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "hair_configurations")
public class HairConfiguration extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hair_item_id", nullable = false)
    private CatalogItem hairItem;

    @Column(name = "brand", nullable = false, length = 150)
    private String brand;

    @Column(name = "hair_length", nullable = false, length = 100)
    private String length;

    @Column(name = "color", length = 100)
    private String color;

    @Column(name = "unit_type", nullable = false, length = 50)
    private String unitType;

    @Column(name = "unit_selling_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitSellingPrice;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "installation_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal installationFee;

    @Column(name = "minimum_quantity", precision = 19, scale = 3)
    private BigDecimal minimumQuantity;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected HairConfiguration() {
    }

    public HairConfiguration(
            CatalogItem hairItem,
            String brand,
            String length,
            String color,
            String unitType,
            BigDecimal unitSellingPrice,
            BigDecimal unitCost,
            BigDecimal installationFee,
            BigDecimal minimumQuantity,
            boolean active,
            LocalDate effectiveDate) {
        this.hairItem = hairItem;
        this.brand = brand;
        this.length = length;
        this.color = color;
        this.unitType = unitType;
        this.unitSellingPrice = unitSellingPrice;
        this.unitCost = unitCost;
        this.installationFee = installationFee;
        this.minimumQuantity = minimumQuantity;
        this.active = active;
        this.effectiveDate = effectiveDate;
    }

    public Long getId() {
        return id;
    }

    public CatalogItem getHairItem() {
        return hairItem;
    }

    public String getBrand() {
        return brand;
    }

    public String getLength() {
        return length;
    }

    public String getColor() {
        return color;
    }

    public String getUnitType() {
        return unitType;
    }

    public BigDecimal getUnitSellingPrice() {
        return unitSellingPrice;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public BigDecimal getInstallationFee() {
        return installationFee;
    }

    public BigDecimal getMinimumQuantity() {
        return minimumQuantity;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public long getVersion() {
        return version;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
