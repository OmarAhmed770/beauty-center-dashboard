package com.omar.beautycenter.closing;

import com.omar.beautycenter.closing.dto.CloseDailyRequest;
import com.omar.beautycenter.closing.dto.DailyClosingPreviewResponse;
import com.omar.beautycenter.closing.dto.DailyClosingResponse;
import com.omar.beautycenter.closing.dto.ReopenDailyRequest;
import com.omar.beautycenter.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/daily-closings")
@Tag(name = "Daily Closing")
public class DailyClosingController {

    private final DailyClosingService service;

    public DailyClosingController(DailyClosingService service) {
        this.service = service;
    }

    @GetMapping("/{businessDate}/preview")
    @Operation(summary = "Preview live financial values before closing")
    public DailyClosingPreviewResponse preview(@PathVariable @NotNull LocalDate businessDate) {
        return service.preview(businessDate);
    }

    @PostMapping("/{businessDate}/close")
    @Operation(summary = "Close a business date and store its financial snapshot")
    public DailyClosingResponse close(
            @PathVariable @NotNull LocalDate businessDate,
            @Valid @RequestBody CloseDailyRequest request) {
        return service.close(businessDate, request);
    }

    @PostMapping("/{businessDate}/reopen")
    @Operation(summary = "Reopen a closed business date (OWNER only)")
    public DailyClosingResponse reopen(
            @PathVariable @NotNull LocalDate businessDate,
            @Valid @RequestBody ReopenDailyRequest request) {
        return service.reopen(businessDate, request);
    }

    @GetMapping("/{businessDate}")
    @Operation(summary = "Reprint the stored daily-closing snapshot")
    public DailyClosingResponse getStored(@PathVariable @NotNull LocalDate businessDate) {
        return service.getStored(businessDate);
    }

    @GetMapping("/{businessDate}/versions")
    @Operation(summary = "List stored daily-closing snapshot versions, newest first")
    public List<DailyClosingResponse> versions(@PathVariable @NotNull LocalDate businessDate) {
        return service.versions(businessDate);
    }

    @GetMapping
    @Operation(summary = "Search daily-closing records")
    public PageResponse<DailyClosingResponse> search(
            @RequestParam(required = false) DailyClosingStatus status,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.search(status, fromDate, toDate, page, size);
    }
}
