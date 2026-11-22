package com.omar.beautycenter.closing;

import com.omar.beautycenter.report.dto.ExpenseCategoryFinancialTotalResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "daily_closing_expense_totals")
public class DailyClosingExpenseTotal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "closing_id", nullable = false)
    private DailyClosing closing;
    @Column(name = "category_id", nullable = false)
    private Long categoryId;
    @Column(name = "category_name_arabic_snapshot", nullable = false, length = 150)
    private String nameArabic;
    @Column(name = "category_name_english_snapshot", nullable = false, length = 150)
    private String nameEnglish;
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(name = "expense_count", nullable = false)
    private long expenseCount;

    protected DailyClosingExpenseTotal() { }

    public DailyClosingExpenseTotal(DailyClosing closing, ExpenseCategoryFinancialTotalResponse total) {
        this.closing = closing; this.categoryId = total.categoryId(); this.nameArabic = total.nameArabic();
        this.nameEnglish = total.nameEnglish(); this.amount = total.amount(); this.expenseCount = total.expenseCount();
    }

    public Long getCategoryId() { return categoryId; }
    public String getNameArabic() { return nameArabic; }
    public String getNameEnglish() { return nameEnglish; }
    public BigDecimal getAmount() { return amount; }
    public long getExpenseCount() { return expenseCount; }
}
