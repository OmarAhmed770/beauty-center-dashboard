package com.omar.beautycenter.report;

import com.omar.beautycenter.catalog.CatalogItemType;
import com.omar.beautycenter.payment.PaymentMethodType;
import com.omar.beautycenter.report.dto.DailySalesTrendResponse;
import com.omar.beautycenter.report.dto.ExpenseCategoryFinancialTotalResponse;
import com.omar.beautycenter.report.dto.PaymentMethodFinancialTotalResponse;
import com.omar.beautycenter.report.dto.TopCatalogItemResponse;
import com.omar.beautycenter.report.dto.TopEmployeeResponse;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class FinancialDataQueryStore {

    private static final String FINANCIAL_INVOICE_STATUSES =
            "'COMPLETED', 'PARTIALLY_REFUNDED', 'REFUNDED'";

    private final JdbcTemplate jdbcTemplate;

    FinancialDataQueryStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    FinancialInputs loadInputs(LocalDate fromDate, LocalDate toDate) {
        InvoiceFigures invoices = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(subtotal), 0),
                       COALESCE(SUM(discount_total), 0),
                       COUNT(*)
                FROM invoices
                WHERE status IN (""" + FINANCIAL_INVOICE_STATUSES + ")"
                + " AND business_date BETWEEN ? AND ?",
                (result, row) -> new InvoiceFigures(
                        result.getBigDecimal(1), result.getBigDecimal(2), result.getLong(3)),
                fromDate, toDate);
        EmployeeFigures employees = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(CASE WHEN transaction_type = 'DAILY_WAGE' THEN amount ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN transaction_type = 'BONUS' THEN amount ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN transaction_type = 'COMMISSION' THEN amount ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN transaction_type = 'ADVANCE' THEN amount ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN transaction_type = 'PAYMENT' THEN amount ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN payment_method_type = 'CASH'
                            AND transaction_type IN ('DAILY_WAGE', 'ADVANCE', 'BONUS', 'COMMISSION', 'PAYMENT')
                            THEN amount ELSE 0 END), 0)
                FROM employee_transactions
                WHERE status = 'ACTIVE' AND business_date BETWEEN ? AND ?
                """, (result, row) -> new EmployeeFigures(
                        result.getBigDecimal(1), result.getBigDecimal(2), result.getBigDecimal(3),
                        result.getBigDecimal(4), result.getBigDecimal(5), result.getBigDecimal(6)),
                fromDate, toDate);

        return new FinancialInputs(
                invoices.grossSales(),
                invoices.discounts(),
                scalar("SELECT COALESCE(SUM(amount), 0) FROM invoice_refunds "
                        + "WHERE status = 'COMPLETED' AND business_date BETWEEN ? AND ?", fromDate, toDate),
                invoices.count(),
                scalar("SELECT COALESCE(SUM(amount), 0) FROM expenses "
                        + "WHERE status = 'ACTIVE' AND business_date BETWEEN ? AND ?", fromDate, toDate),
                employees.dailyWages(), employees.bonuses(), employees.commissions(),
                employees.advances(), employees.payments(),
                scalar("""
                        SELECT COALESCE(SUM(item.quantity * item.unit_cost_snapshot), 0)
                        FROM invoice_items item
                        JOIN invoices invoice ON invoice.id = item.invoice_id
                        WHERE invoice.status IN (""" + FINANCIAL_INVOICE_STATUSES + ")"
                        + " AND invoice.business_date BETWEEN ? AND ?"
                        + " AND item.item_type_snapshot IN ('PRODUCT', 'HAIR_PRODUCT', 'HAIR_INSTALLATION')",
                        fromDate, toDate),
                scalar("""
                        SELECT COALESCE(SUM(payment.amount), 0)
                        FROM invoice_payments payment
                        JOIN invoices invoice ON invoice.id = payment.invoice_id
                        WHERE invoice.status IN (""" + FINANCIAL_INVOICE_STATUSES + ")"
                        + " AND invoice.business_date BETWEEN ? AND ?"
                        + " AND payment.payment_method_type_snapshot = 'CASH'", fromDate, toDate),
                scalar("SELECT COALESCE(SUM(amount), 0) FROM expenses "
                        + "WHERE status = 'ACTIVE' AND payment_method_type = 'CASH' "
                        + "AND business_date BETWEEN ? AND ?", fromDate, toDate),
                employees.cashOutflows(),
                scalar("""
                        SELECT COALESCE(SUM(payment.amount), 0)
                        FROM invoice_refund_payments payment
                        JOIN invoice_refunds refund ON refund.id = payment.refund_id
                        WHERE refund.status = 'COMPLETED'
                          AND refund.business_date BETWEEN ? AND ?
                          AND payment.payment_method_type_snapshot = 'CASH'
                        """, fromDate, toDate),
                scalar("""
                        SELECT COALESCE(SUM(CASE WHEN adjustment_type = 'CASH_IN'
                            THEN amount ELSE -amount END), 0)
                        FROM cash_adjustments
                        WHERE status = 'ACTIVE' AND business_date BETWEEN ? AND ?
                        """, fromDate, toDate));
    }

    List<PaymentMethodFinancialTotalResponse> paymentTotals(LocalDate fromDate, LocalDate toDate) {
        return jdbcTemplate.query("""
                SELECT totals.payment_method_id,
                       MAX(totals.code), MAX(totals.name_ar), MAX(totals.name_en), MAX(totals.method_type),
                       SUM(totals.received), SUM(totals.refunded)
                FROM (
                    SELECT payment.payment_method_id,
                           payment.payment_method_code_snapshot AS code,
                           payment.payment_method_name_arabic_snapshot AS name_ar,
                           payment.payment_method_name_english_snapshot AS name_en,
                           payment.payment_method_type_snapshot AS method_type,
                           payment.amount AS received, CAST(0 AS NUMERIC(19,2)) AS refunded
                    FROM invoice_payments payment
                    JOIN invoices invoice ON invoice.id = payment.invoice_id
                    WHERE invoice.status IN (""" + FINANCIAL_INVOICE_STATUSES + ")"
                + " AND invoice.business_date BETWEEN ? AND ?"
                + " UNION ALL " + """
                    SELECT payment.payment_method_id,
                           payment.payment_method_code_snapshot,
                           payment.payment_method_name_arabic_snapshot,
                           payment.payment_method_name_english_snapshot,
                           payment.payment_method_type_snapshot,
                           CAST(0 AS NUMERIC(19,2)), payment.amount
                    FROM invoice_refund_payments payment
                    JOIN invoice_refunds refund ON refund.id = payment.refund_id
                    WHERE refund.status = 'COMPLETED' AND refund.business_date BETWEEN ? AND ?
                ) totals
                GROUP BY totals.payment_method_id
                ORDER BY MAX(totals.code)
                """, (result, row) -> {
                    BigDecimal received = FinancialFormulaService.money(result.getBigDecimal(6));
                    BigDecimal refunded = FinancialFormulaService.money(result.getBigDecimal(7));
                    return new PaymentMethodFinancialTotalResponse(
                            result.getLong(1), result.getString(2), result.getString(3), result.getString(4),
                            PaymentMethodType.valueOf(result.getString(5)), received, refunded,
                            FinancialFormulaService.money(received.subtract(refunded)));
                }, fromDate, toDate, fromDate, toDate);
    }

    List<ExpenseCategoryFinancialTotalResponse> expenseTotals(LocalDate fromDate, LocalDate toDate) {
        return jdbcTemplate.query("""
                SELECT category_id, MAX(category_name_arabic), MAX(category_name_english),
                       SUM(amount), COUNT(*)
                FROM expenses
                WHERE status = 'ACTIVE' AND business_date BETWEEN ? AND ?
                GROUP BY category_id
                ORDER BY SUM(amount) DESC, MAX(category_name_english)
                """, (result, row) -> new ExpenseCategoryFinancialTotalResponse(
                        result.getLong(1), result.getString(2), result.getString(3),
                        FinancialFormulaService.money(result.getBigDecimal(4)), result.getLong(5)),
                fromDate, toDate);
    }

    List<TopCatalogItemResponse> topItems(LocalDate fromDate, LocalDate toDate, int limit) {
        return jdbcTemplate.query("""
                SELECT item.catalog_item_id, item.item_code_snapshot,
                       MAX(item.item_name_arabic_snapshot), MAX(item.item_name_english_snapshot),
                       item.item_type_snapshot, SUM(item.quantity), SUM(item.final_line_total),
                       COUNT(DISTINCT invoice.id)
                FROM invoice_items item
                JOIN invoices invoice ON invoice.id = item.invoice_id
                WHERE invoice.status IN (""" + FINANCIAL_INVOICE_STATUSES + ")"
                + " AND invoice.business_date BETWEEN ? AND ?"
                + " GROUP BY item.catalog_item_id, item.item_code_snapshot, item.item_type_snapshot"
                + " ORDER BY SUM(item.final_line_total) DESC, item.item_code_snapshot LIMIT ?",
                (result, row) -> new TopCatalogItemResponse(
                        result.getObject(1, Long.class), result.getString(2), result.getString(3),
                        result.getString(4), CatalogItemType.valueOf(result.getString(5)),
                        result.getBigDecimal(6), FinancialFormulaService.money(result.getBigDecimal(7)),
                        result.getLong(8)), fromDate, toDate, limit);
    }

    List<TopEmployeeResponse> topEmployees(LocalDate fromDate, LocalDate toDate, int limit) {
        return jdbcTemplate.query("""
                SELECT item.employee_id, MAX(item.employee_name_snapshot), SUM(item.quantity),
                       SUM(item.final_line_total), COUNT(DISTINCT invoice.id)
                FROM invoice_items item
                JOIN invoices invoice ON invoice.id = item.invoice_id
                WHERE invoice.status IN (""" + FINANCIAL_INVOICE_STATUSES + ")"
                + " AND invoice.business_date BETWEEN ? AND ? AND item.employee_id IS NOT NULL"
                + " GROUP BY item.employee_id"
                + " ORDER BY SUM(item.final_line_total) DESC, MAX(item.employee_name_snapshot) LIMIT ?",
                (result, row) -> new TopEmployeeResponse(
                        result.getLong(1), result.getString(2), result.getBigDecimal(3),
                        FinancialFormulaService.money(result.getBigDecimal(4)), result.getLong(5)),
                fromDate, toDate, limit);
    }

    List<DailySalesTrendResponse> salesTrend(LocalDate fromDate, LocalDate toDate) {
        return jdbcTemplate.query("""
                SELECT dates.business_date, SUM(dates.gross), SUM(dates.discounts),
                       SUM(dates.refunds), SUM(dates.invoice_count)
                FROM (
                    SELECT invoice.business_date, SUM(invoice.subtotal) AS gross,
                           SUM(invoice.discount_total) AS discounts,
                           CAST(0 AS NUMERIC(19,2)) AS refunds, COUNT(*) AS invoice_count
                    FROM invoices invoice
                    WHERE invoice.status IN (""" + FINANCIAL_INVOICE_STATUSES + ")"
                + " AND invoice.business_date BETWEEN ? AND ? GROUP BY invoice.business_date"
                + " UNION ALL " + """
                    SELECT refund.business_date, CAST(0 AS NUMERIC(19,2)), CAST(0 AS NUMERIC(19,2)),
                           SUM(refund.amount), CAST(0 AS BIGINT)
                    FROM invoice_refunds refund
                    WHERE refund.status = 'COMPLETED' AND refund.business_date BETWEEN ? AND ?
                    GROUP BY refund.business_date
                ) dates
                GROUP BY dates.business_date ORDER BY dates.business_date
                """, (result, row) -> {
                    BigDecimal gross = FinancialFormulaService.money(result.getBigDecimal(2));
                    BigDecimal discounts = FinancialFormulaService.money(result.getBigDecimal(3));
                    BigDecimal refunds = FinancialFormulaService.money(result.getBigDecimal(4));
                    return new DailySalesTrendResponse(result.getObject(1, LocalDate.class), gross, discounts,
                            refunds, FinancialFormulaService.money(gross.subtract(discounts).subtract(refunds)),
                            result.getLong(5));
                }, fromDate, toDate, fromDate, toDate);
    }

    private BigDecimal scalar(String sql, LocalDate fromDate, LocalDate toDate) {
        return FinancialFormulaService.money(jdbcTemplate.queryForObject(
                sql, BigDecimal.class, Date.valueOf(fromDate), Date.valueOf(toDate)));
    }

    private record InvoiceFigures(BigDecimal grossSales, BigDecimal discounts, long count) { }
    private record EmployeeFigures(
            BigDecimal dailyWages, BigDecimal bonuses, BigDecimal commissions,
            BigDecimal advances, BigDecimal payments, BigDecimal cashOutflows) { }
}
