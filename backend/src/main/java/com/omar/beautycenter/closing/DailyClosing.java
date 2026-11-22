package com.omar.beautycenter.closing;

import com.omar.beautycenter.common.audit.BaseAuditEntity;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.report.dto.FinancialSummaryResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_closings")
public class DailyClosing extends BaseAuditEntity {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "business_date", nullable = false, updatable = false)
    private LocalDate businessDate;
    @Column(name = "closing_version", nullable = false, updatable = false)
    private int closingVersion;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 20)
    private DailyClosingStatus status;
    @Column(name = "gross_sales", nullable = false, precision = 19, scale = 2)
    private BigDecimal grossSales;
    @Column(name = "discounts", nullable = false, precision = 19, scale = 2)
    private BigDecimal discounts;
    @Column(name = "refunds", nullable = false, precision = 19, scale = 2)
    private BigDecimal refunds;
    @Column(name = "net_sales", nullable = false, precision = 19, scale = 2)
    private BigDecimal netSales;
    @Column(name = "completed_invoice_count", nullable = false)
    private long completedInvoiceCount;
    @Column(name = "expense_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal expenseTotal;
    @Column(name = "daily_wages", nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyWages;
    @Column(name = "bonuses", nullable = false, precision = 19, scale = 2)
    private BigDecimal bonuses;
    @Column(name = "commissions", nullable = false, precision = 19, scale = 2)
    private BigDecimal commissions;
    @Column(name = "employee_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal employeeCost;
    @Column(name = "advances", nullable = false, precision = 19, scale = 2)
    private BigDecimal advances;
    @Column(name = "employee_payments", nullable = false, precision = 19, scale = 2)
    private BigDecimal employeePayments;
    @Column(name = "cost_of_sold_items", nullable = false, precision = 19, scale = 2)
    private BigDecimal costOfSoldItems;
    @Column(name = "net_profit", nullable = false, precision = 19, scale = 2)
    private BigDecimal netProfit;
    @Column(name = "cash_received", nullable = false, precision = 19, scale = 2)
    private BigDecimal cashReceived;
    @Column(name = "cash_expenses", nullable = false, precision = 19, scale = 2)
    private BigDecimal cashExpenses;
    @Column(name = "cash_employee_outflows", nullable = false, precision = 19, scale = 2)
    private BigDecimal cashEmployeeOutflows;
    @Column(name = "cash_refunds", nullable = false, precision = 19, scale = 2)
    private BigDecimal cashRefunds;
    @Column(name = "cash_adjustments", nullable = false, precision = 19, scale = 2)
    private BigDecimal cashAdjustments;
    @Column(name = "expected_cash", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedCash;
    @Column(name = "actual_cash", nullable = false, precision = 19, scale = 2)
    private BigDecimal actualCash;
    @Column(name = "cash_difference", nullable = false, precision = 19, scale = 2)
    private BigDecimal cashDifference;
    @Column(name = "notes", length = 2000)
    private String notes;
    @Column(name = "closed_by", length = 100)
    private String closedBy;
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    @Column(name = "reopened_by", length = 100)
    private String reopenedBy;
    @Column(name = "reopened_at")
    private LocalDateTime reopenedAt;
    @Column(name = "reopening_reason", length = 500)
    private String reopeningReason;
    @Column(name = "reopen_count", nullable = false)
    private int reopenCount;
    @Version @Column(name = "version", nullable = false)
    private long version;

    protected DailyClosing() { }

    public DailyClosing(LocalDate businessDate, int closingVersion) {
        this.businessDate = businessDate;
        this.closingVersion = closingVersion;
        this.status = DailyClosingStatus.OPEN;
        this.grossSales = ZERO; this.discounts = ZERO; this.refunds = ZERO; this.netSales = ZERO;
        this.expenseTotal = ZERO; this.dailyWages = ZERO; this.bonuses = ZERO; this.commissions = ZERO;
        this.employeeCost = ZERO; this.advances = ZERO; this.employeePayments = ZERO;
        this.costOfSoldItems = ZERO; this.netProfit = ZERO; this.cashReceived = ZERO;
        this.cashExpenses = ZERO; this.cashEmployeeOutflows = ZERO; this.cashRefunds = ZERO;
        this.cashAdjustments = ZERO; this.expectedCash = ZERO; this.actualCash = ZERO;
        this.cashDifference = ZERO;
    }

    public void close(
            FinancialSummaryResponse summary, BigDecimal actualCash, String notes,
            String actor, LocalDateTime at) {
        if (id != null) {
            throw new BusinessRuleException("Daily closing snapshots are immutable.");
        }
        if (status == DailyClosingStatus.CLOSED) {
            throw new BusinessRuleException("The business date is already closed.");
        }
        this.status = DailyClosingStatus.CLOSED;
        this.grossSales = summary.grossSales();
        this.discounts = summary.discounts();
        this.refunds = summary.refunds();
        this.netSales = summary.netSales();
        this.completedInvoiceCount = summary.completedInvoiceCount();
        this.expenseTotal = summary.expenses();
        this.dailyWages = summary.dailyWages();
        this.bonuses = summary.bonuses();
        this.commissions = summary.commissions();
        this.employeeCost = summary.employeeCost();
        this.advances = summary.advances();
        this.employeePayments = summary.employeePayments();
        this.costOfSoldItems = summary.costOfSoldItemsAndHair();
        this.netProfit = summary.netProfit();
        this.cashReceived = summary.cashPaymentsReceived();
        this.cashExpenses = summary.cashExpenses();
        this.cashEmployeeOutflows = summary.cashEmployeePayments();
        this.cashRefunds = summary.cashRefunds();
        this.cashAdjustments = summary.cashAdjustments();
        this.expectedCash = summary.expectedCashBalance();
        this.actualCash = actualCash;
        this.cashDifference = actualCash.subtract(expectedCash);
        this.notes = notes;
        this.closedBy = actor;
        this.closedAt = at;
    }

    public void reopen(String reason, String actor, LocalDateTime at) {
        if (status != DailyClosingStatus.CLOSED) {
            throw new BusinessRuleException("Only a closed business date may be reopened.");
        }
        this.status = DailyClosingStatus.REOPENED;
        this.reopeningReason = reason;
        this.reopenedBy = actor;
        this.reopenedAt = at;
        this.reopenCount++;
    }

    public Long getId() { return id; }
    public LocalDate getBusinessDate() { return businessDate; }
    public int getClosingVersion() { return closingVersion; }
    public DailyClosingStatus getStatus() { return status; }
    public BigDecimal getGrossSales() { return grossSales; }
    public BigDecimal getDiscounts() { return discounts; }
    public BigDecimal getRefunds() { return refunds; }
    public BigDecimal getNetSales() { return netSales; }
    public long getCompletedInvoiceCount() { return completedInvoiceCount; }
    public BigDecimal getExpenseTotal() { return expenseTotal; }
    public BigDecimal getDailyWages() { return dailyWages; }
    public BigDecimal getBonuses() { return bonuses; }
    public BigDecimal getCommissions() { return commissions; }
    public BigDecimal getEmployeeCost() { return employeeCost; }
    public BigDecimal getAdvances() { return advances; }
    public BigDecimal getEmployeePayments() { return employeePayments; }
    public BigDecimal getCostOfSoldItems() { return costOfSoldItems; }
    public BigDecimal getNetProfit() { return netProfit; }
    public BigDecimal getCashReceived() { return cashReceived; }
    public BigDecimal getCashExpenses() { return cashExpenses; }
    public BigDecimal getCashEmployeeOutflows() { return cashEmployeeOutflows; }
    public BigDecimal getCashRefunds() { return cashRefunds; }
    public BigDecimal getCashAdjustments() { return cashAdjustments; }
    public BigDecimal getExpectedCash() { return expectedCash; }
    public BigDecimal getActualCash() { return actualCash; }
    public BigDecimal getCashDifference() { return cashDifference; }
    public String getNotes() { return notes; }
    public String getClosedBy() { return closedBy; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public String getReopenedBy() { return reopenedBy; }
    public LocalDateTime getReopenedAt() { return reopenedAt; }
    public String getReopeningReason() { return reopeningReason; }
    public int getReopenCount() { return reopenCount; }
    public long getVersion() { return version; }
}
