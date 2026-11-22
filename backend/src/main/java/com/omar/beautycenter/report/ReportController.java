package com.omar.beautycenter.report;

import com.omar.beautycenter.catalog.CatalogItemService;
import com.omar.beautycenter.catalog.PriceValueType;
import com.omar.beautycenter.catalog.dto.PriceHistoryResponse;
import com.omar.beautycenter.closing.DailyClosingService;
import com.omar.beautycenter.closing.dto.DailyClosingResponse;
import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.employee.EmployeeTransactionService;
import com.omar.beautycenter.employee.dto.EmployeeStatementResponse;
import com.omar.beautycenter.expense.ExpenseService;
import com.omar.beautycenter.expense.ExpenseStatus;
import com.omar.beautycenter.expense.dto.ExpenseResponse;
import com.omar.beautycenter.report.dto.FinancialSummaryResponse;
import com.omar.beautycenter.report.dto.PaymentMethodFinancialTotalResponse;
import com.omar.beautycenter.report.dto.ProfitReportResponse;
import com.omar.beautycenter.sales.InvoiceService;
import com.omar.beautycenter.sales.InvoiceStatus;
import com.omar.beautycenter.sales.dto.InvoiceSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/reports")
@PreAuthorize("@permissionAccess.has('REPORTS')")
@Tag(name = "Reports")
public class ReportController {

    private final FinancialQueryService financialQueryService;
    private final DailyClosingService dailyClosingService;
    private final EmployeeTransactionService employeeTransactionService;
    private final ExpenseService expenseService;
    private final InvoiceService invoiceService;
    private final CatalogItemService catalogItemService;

    public ReportController(
            FinancialQueryService financialQueryService,
            DailyClosingService dailyClosingService,
            EmployeeTransactionService employeeTransactionService,
            ExpenseService expenseService,
            InvoiceService invoiceService,
            CatalogItemService catalogItemService) {
        this.financialQueryService = financialQueryService;
        this.dailyClosingService = dailyClosingService;
        this.employeeTransactionService = employeeTransactionService;
        this.expenseService = expenseService;
        this.invoiceService = invoiceService;
        this.catalogItemService = catalogItemService;
    }

    @GetMapping("/daily-closing/{businessDate}")
    @Operation(summary = "Printable/front-end-ready stored daily closing report")
    public DailyClosingResponse dailyClosing(@PathVariable LocalDate businessDate) {
        return dailyClosingService.getStored(businessDate);
    }

    @GetMapping("/financial")
    @Operation(summary = "Date-range financial report")
    public FinancialSummaryResponse financial(
            @RequestParam @NotNull LocalDate fromDate,
            @RequestParam @NotNull LocalDate toDate) {
        return financialQueryService.summary(fromDate, toDate);
    }

    @GetMapping("/employees/{employeeId}/statement")
    @Operation(summary = "Employee statement report")
    public EmployeeStatementResponse employeeStatement(
            @PathVariable @Positive Long employeeId,
            @RequestParam @NotNull LocalDate fromDate,
            @RequestParam @NotNull LocalDate toDate,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int size) {
        return employeeTransactionService.statement(employeeId, fromDate, toDate, page, size);
    }

    @GetMapping("/expenses")
    @Operation(summary = "Expense report with filters and pagination")
    public PageResponse<ExpenseResponse> expenses(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long paymentMethodId,
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam @NotNull LocalDate fromDate,
            @RequestParam @NotNull LocalDate toDate,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int size) {
        return expenseService.search(categoryId, paymentMethodId, status, fromDate, toDate, query, page, size);
    }

    @GetMapping("/sales")
    @Operation(summary = "Sales report with invoice filters and pagination")
    public PageResponse<InvoiceSummaryResponse> sales(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam @NotNull LocalDate fromDate,
            @RequestParam @NotNull LocalDate toDate,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int size) {
        return invoiceService.search(status, fromDate, toDate, query, page, size);
    }

    @GetMapping("/payment-methods")
    @Operation(summary = "Payment-method report including refunds and net received")
    public List<PaymentMethodFinancialTotalResponse> paymentMethods(
            @RequestParam @NotNull LocalDate fromDate,
            @RequestParam @NotNull LocalDate toDate) {
        return financialQueryService.summary(fromDate, toDate).paymentTotalsByMethod();
    }

    @GetMapping("/profit")
    @Operation(summary = "Profit formula report")
    public ProfitReportResponse profit(
            @RequestParam @NotNull LocalDate fromDate,
            @RequestParam @NotNull LocalDate toDate) {
        return financialQueryService.profit(fromDate, toDate);
    }

    @GetMapping("/price-history")
    @Operation(summary = "Catalog price-history report")
    public PageResponse<PriceHistoryResponse> priceHistory(
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) PriceValueType valueType,
            @RequestParam(required = false) LocalDateTime fromDateTime,
            @RequestParam(required = false) LocalDateTime toDateTime,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int size) {
        return catalogItemService.searchPriceHistory(
                itemId, valueType, fromDateTime, toDateTime, page, size);
    }
}
