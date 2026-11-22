package com.omar.beautycenter.employee;

import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.employee.dto.CancelEmployeeTransactionRequest;
import com.omar.beautycenter.employee.dto.CreateEmployeeTransactionRequest;
import com.omar.beautycenter.employee.dto.EmployeeTransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
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
@RequestMapping("/api/v1/employee-transactions")
@Tag(name = "Employee Transactions")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeTransactionController {

    private final EmployeeTransactionService transactionService;

    public EmployeeTransactionController(EmployeeTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create an employee transaction",
            description = "Requires the Employees permission.")
    public EmployeeTransactionResponse create(
            @Valid @RequestBody CreateEmployeeTransactionRequest request) {
        return transactionService.create(request);
    }

    @PostMapping("/{transactionId}/cancel")
    @Operation(
            summary = "Cancel an employee transaction",
            description = "Requires the Employees permission. Financial rows are never physically deleted.")
    public EmployeeTransactionResponse cancel(
            @PathVariable @Positive Long transactionId,
            @Valid @RequestBody CancelEmployeeTransactionRequest request) {
        return transactionService.cancel(transactionId, request);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get an employee transaction", description = "Requires the Employees permission")
    public EmployeeTransactionResponse getById(@PathVariable @Positive Long transactionId) {
        return transactionService.getById(transactionId);
    }

    @GetMapping
    @Operation(
            summary = "List employee transactions",
            description = "Requires the Employees permission. Filters may be combined; omitted status returns active and cancelled rows.")
    public PageResponse<EmployeeTransactionResponse> search(
            @RequestParam(required = false) @Positive Long employeeId,
            @RequestParam(required = false) EmployeeTransactionType type,
            @RequestParam(required = false) EmployeeTransactionStatus status,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return transactionService.search(
                employeeId, type, status, fromDate, toDate, page, size);
    }
}
