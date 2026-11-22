package com.omar.beautycenter.expense;

import com.omar.beautycenter.common.audit.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "expense_categories")
public class ExpenseCategory extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_arabic", nullable = false, unique = true, length = 150)
    private String nameArabic;

    @Column(name = "name_english", nullable = false, unique = true, length = 150)
    private String nameEnglish;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ExpenseCategory() {
    }

    public ExpenseCategory(
            String nameArabic,
            String nameEnglish,
            boolean active,
            String notes) {
        this.nameArabic = nameArabic;
        this.nameEnglish = nameEnglish;
        this.active = active;
        this.notes = notes;
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

    public String getNotes() {
        return notes;
    }

    public long getVersion() {
        return version;
    }

    public void update(String nameArabic, String nameEnglish, String notes) {
        this.nameArabic = nameArabic;
        this.nameEnglish = nameEnglish;
        this.notes = notes;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
