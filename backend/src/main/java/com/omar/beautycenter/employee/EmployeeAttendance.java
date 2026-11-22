package com.omar.beautycenter.employee;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employee_attendances", uniqueConstraints = {
        @UniqueConstraint(name = "uk_employee_attendances_employee_date", columnNames = {"employee_id", "business_date"}),
        @UniqueConstraint(name = "uk_employee_attendances_daily_wage", columnNames = "daily_wage_transaction_id")
})
public class EmployeeAttendance extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 20)
    private EmployeeAttendanceStatus status;

    @Column(name = "daily_wage_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyWageAmount;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_wage_transaction_id", unique = true)
    private EmployeeTransaction dailyWageTransaction;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected EmployeeAttendance() {
    }

    public EmployeeAttendance(
            Employee employee,
            LocalDate businessDate,
            EmployeeAttendanceStatus status,
            BigDecimal dailyWageAmount) {
        this.employee = employee;
        this.businessDate = businessDate;
        this.status = status;
        this.dailyWageAmount = dailyWageAmount;
    }

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public EmployeeAttendanceStatus getStatus() {
        return status;
    }

    public BigDecimal getDailyWageAmount() {
        return dailyWageAmount;
    }

    public EmployeeTransaction getDailyWageTransaction() {
        return dailyWageTransaction;
    }

    public long getVersion() {
        return version;
    }

    public void markPresent(EmployeeTransaction transaction) {
        this.status = EmployeeAttendanceStatus.PRESENT;
        this.dailyWageTransaction = transaction;
    }

    public void markAbsent() {
        this.status = EmployeeAttendanceStatus.ABSENT;
    }
}
