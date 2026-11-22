package com.omar.beautycenter.report;

import com.omar.beautycenter.report.dto.FinancialSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final FinancialQueryService financialQueryService;

    public DashboardController(FinancialQueryService financialQueryService) {
        this.financialQueryService = financialQueryService;
    }

    @GetMapping
    @PreAuthorize("@permissionAccess.has('DASHBOARD')")
    @Operation(summary = "Get the financial dashboard for a preset or custom date range")
    public FinancialSummaryResponse dashboard(
            @RequestParam(defaultValue = "TODAY") DashboardFilter filter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return financialQueryService.dashboard(filter, fromDate, toDate);
    }
}
