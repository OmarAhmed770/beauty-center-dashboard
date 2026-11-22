package com.omar.beautycenter.expense;

import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.expense.dto.CancelExpenseRequest;
import com.omar.beautycenter.expense.dto.CreateExpenseRequest;
import com.omar.beautycenter.expense.dto.ExpenseCategoryTotalResponse;
import com.omar.beautycenter.expense.dto.ExpensePaymentMethodTotalResponse;
import com.omar.beautycenter.expense.dto.ExpenseResponse;
import com.omar.beautycenter.expense.dto.ExpenseSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/expenses")
@Tag(name = "Expenses")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create an expense",
            description = "Requires the Expenses permission; category and payment method must be active.")
    public ExpenseResponse create(@Valid @RequestBody CreateExpenseRequest request) {
        return expenseService.create(request);
    }

    @PostMapping("/{expenseId}/cancel")
    @Operation(
            summary = "Cancel an expense",
            description = "Requires the Expenses permission on a writable date. Rows are never deleted.")
    public ExpenseResponse cancel(
            @PathVariable @Positive Long expenseId,
            @Valid @RequestBody CancelExpenseRequest request) {
        return expenseService.cancel(expenseId, request);
    }

    @GetMapping("/{expenseId}")
    @Operation(summary = "Get an expense", description = "Available to all authenticated roles")
    public ExpenseResponse getById(@PathVariable @Positive Long expenseId) {
        return expenseService.getById(expenseId);
    }

    @GetMapping
    @Operation(
            summary = "Search expenses",
            description = "Filters category, payment method, status, dates, and text with bounded pagination.")
    public PageResponse<ExpenseResponse> search(
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false) @Positive Long paymentMethodId,
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return expenseService.search(
                categoryId,
                paymentMethodId,
                status,
                fromDate,
                toDate,
                query,
                page,
                size);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get expense summary", description = "ACTIVE rows only contribute to monetary totals.")
    public ExpenseSummaryResponse summary(
            @RequestParam @NotNull LocalDate fromDate,
            @RequestParam @NotNull LocalDate toDate) {
        return expenseService.summary(fromDate, toDate);
    }

    @GetMapping("/summary/by-category")
    @Operation(summary = "Group expenses by category", description = "ACTIVE rows only")
    public List<ExpenseCategoryTotalResponse> totalsByCategory(
            @RequestParam @NotNull LocalDate fromDate,
            @RequestParam @NotNull LocalDate toDate) {
        return expenseService.totalsByCategory(fromDate, toDate);
    }

    @GetMapping("/summary/by-payment-method")
    @Operation(summary = "Group expenses by payment method", description = "ACTIVE rows only")
    public List<ExpensePaymentMethodTotalResponse> totalsByPaymentMethod(
            @RequestParam @NotNull LocalDate fromDate,
            @RequestParam @NotNull LocalDate toDate) {
        return expenseService.totalsByPaymentMethod(fromDate, toDate);
    }
}
