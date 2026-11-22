package com.omar.beautycenter.closing;

import com.omar.beautycenter.closing.dto.CancelCashAdjustmentRequest;
import com.omar.beautycenter.closing.dto.CashAdjustmentResponse;
import com.omar.beautycenter.closing.dto.CreateCashAdjustmentRequest;
import com.omar.beautycenter.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/cash-adjustments")
@Tag(name = "Cash Adjustments")
public class CashAdjustmentController {

    private final CashAdjustmentService service;

    public CashAdjustmentController(CashAdjustmentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a cash-in or cash-out adjustment")
    public CashAdjustmentResponse create(@Valid @RequestBody CreateCashAdjustmentRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a cash adjustment without deleting its history")
    public CashAdjustmentResponse cancel(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CancelCashAdjustmentRequest request) {
        return service.cancel(id, request);
    }

    @GetMapping
    @Operation(summary = "Search cash adjustments")
    public PageResponse<CashAdjustmentResponse> search(
            @RequestParam(required = false) CashAdjustmentType type,
            @RequestParam(required = false) CashAdjustmentStatus status,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.search(type, status, fromDate, toDate, page, size);
    }
}
