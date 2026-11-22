package com.omar.beautycenter.employee;

import com.omar.beautycenter.common.audit.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;

@Entity
@Table(name = "employees")
public class Employee extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "position", nullable = false, length = 100)
    private String position;

    @Column(name = "default_daily_wage", nullable = false, precision = 19, scale = 2)
    private BigDecimal defaultDailyWage;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Employee() {
    }

    public Employee(
            String name,
            String phone,
            String position,
            BigDecimal defaultDailyWage,
            String notes) {
        this.name = name;
        this.phone = phone;
        this.position = position;
        this.defaultDailyWage = defaultDailyWage;
        this.notes = notes;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getPosition() {
        return position;
    }

    public BigDecimal getDefaultDailyWage() {
        return defaultDailyWage;
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

    public void update(
            String name,
            String phone,
            String position,
            BigDecimal defaultDailyWage,
            String notes) {
        this.name = name;
        this.phone = phone;
        this.position = position;
        this.defaultDailyWage = defaultDailyWage;
        this.notes = notes;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
