package com.omar.beautycenter.employee;

import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.employee.dto.CreateEmployeeRequest;
import com.omar.beautycenter.employee.dto.EmployeeActiveRequest;
import com.omar.beautycenter.employee.dto.EmployeeAttendanceResponse;
import com.omar.beautycenter.employee.dto.EmployeeResponse;
import com.omar.beautycenter.employee.dto.EmployeePermissionsResponse;
import com.omar.beautycenter.employee.dto.UpdateEmployeePermissionsRequest;
import com.omar.beautycenter.employee.dto.EmployeeStatementResponse;
import com.omar.beautycenter.employee.dto.EmployeeTotalsResponse;
import com.omar.beautycenter.employee.dto.UpdateEmployeeRequest;
import com.omar.beautycenter.employee.dto.SaveEmployeeAttendanceRequest;
import com.omar.beautycenter.user.UserService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/employees")
@Tag(name = "Employees")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeTransactionService transactionService;
    private final EmployeeAttendanceService attendanceService;
    private final UserService userService;

    public EmployeeController(
            EmployeeService employeeService,
            EmployeeTransactionService transactionService,
            EmployeeAttendanceService attendanceService,
            UserService userService) {
        this.employeeService = employeeService;
        this.transactionService = transactionService;
        this.attendanceService = attendanceService;
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an employee", description = "Requires the Employees permission")
    public EmployeeResponse create(@Valid @RequestBody CreateEmployeeRequest request) {
        return employeeService.create(request);
    }

    @PutMapping("/{employeeId}")
    @Operation(summary = "Update an employee", description = "Requires the Employees permission")
    public EmployeeResponse update(
            @PathVariable @Positive Long employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return employeeService.update(employeeId, request);
    }

    @PatchMapping("/{employeeId}/active")
    @Operation(summary = "Activate or deactivate an employee", description = "Requires the Employees permission")
    public EmployeeResponse setActive(
            @PathVariable @Positive Long employeeId,
            @Valid @RequestBody EmployeeActiveRequest request) {
        return employeeService.setActive(employeeId, request.active());
    }

    @GetMapping("/{employeeId}")
    @Operation(summary = "Get an employee", description = "Available to all authenticated roles")
    public EmployeeResponse getById(@PathVariable @Positive Long employeeId) {
        return employeeService.getById(employeeId);
    }

    @GetMapping
    @Operation(summary = "Search employees", description = "Searches name, phone, and position with active-state filtering")
    public PageResponse<EmployeeResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return employeeService.search(query, active, page, size);
    }

    @GetMapping("/{employeeId}/permissions")
    @Operation(summary = "Get linked staff permissions", description = "OWNER only")
    public EmployeePermissionsResponse permissions(@PathVariable @Positive Long employeeId) {
        return userService.employeePermissions(employeeId);
    }

    @PutMapping("/{employeeId}/permissions")
    @Operation(summary = "Update linked staff permissions", description = "OWNER only")
    public EmployeePermissionsResponse updatePermissions(
            @PathVariable @Positive Long employeeId,
            @RequestBody UpdateEmployeePermissionsRequest request) {
        return userService.updateEmployeePermissions(employeeId, request);
    }

    @GetMapping("/attendance")
    @Operation(summary = "List employee attendance", description = "OWNER only. Includes active employees not yet marked for the selected date.")
    public List<EmployeeAttendanceResponse> attendance(
            @RequestParam @NotNull LocalDate businessDate) {
        return attendanceService.list(businessDate);
    }

    @PutMapping("/{employeeId}/attendance")
    @Operation(summary = "Record employee attendance", description = "OWNER only. Present attendance accrues one daily wage entitlement.")
    public EmployeeAttendanceResponse saveAttendance(
            @PathVariable @Positive Long employeeId,
            @RequestParam @NotNull LocalDate businessDate,
            @Valid @RequestBody SaveEmployeeAttendanceRequest request) {
        return attendanceService.save(employeeId, businessDate, request);
    }

    @GetMapping("/{employeeId}/statement")
    @Operation(
            summary = "Get an employee statement",
            description = "Requires the Employees permission. Lists all rows, including cancellations, while totals include ACTIVE rows only.")
    public EmployeeStatementResponse statement(
            @PathVariable @Positive Long employeeId,
            @RequestParam @NotNull LocalDate fromDate,
            @RequestParam @NotNull LocalDate toDate,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return transactionService.statement(employeeId, fromDate, toDate, page, size);
    }

    @GetMapping("/{employeeId}/totals")
    @Operation(
            summary = "Get employee financial totals",
            description = "Requires the Employees permission. Cancelled transactions are excluded.")
    public EmployeeTotalsResponse totals(
            @PathVariable @Positive Long employeeId,
            @RequestParam @NotNull LocalDate fromDate,
            @RequestParam @NotNull LocalDate toDate) {
        return transactionService.totals(employeeId, fromDate, toDate);
    }
}
