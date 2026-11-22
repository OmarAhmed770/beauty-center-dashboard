package com.omar.beautycenter.catalog;

import com.omar.beautycenter.common.audit.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "catalog_categories")
public class CatalogCategory extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_arabic", nullable = false, unique = true, length = 150)
    private String nameArabic;

    @Column(name = "name_english", nullable = false, unique = true, length = 150)
    private String nameEnglish;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "description", length = 1000)
    private String description;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected CatalogCategory() {
    }

    public CatalogCategory(
            String nameArabic,
            String nameEnglish,
            boolean active,
            String description) {
        this.nameArabic = nameArabic;
        this.nameEnglish = nameEnglish;
        this.active = active;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getNameArabic() {
        return nameArabic;
    }

    public String getNameEnglish() {
        return nameEnglish;
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

    public void update(String nameArabic, String nameEnglish, String description) {
        this.nameArabic = nameArabic;
        this.nameEnglish = nameEnglish;
        this.description = description;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
