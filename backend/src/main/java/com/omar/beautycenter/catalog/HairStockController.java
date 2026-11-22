package com.omar.beautycenter.catalog;

import com.omar.beautycenter.catalog.dto.AdjustHairStockRequest;
import com.omar.beautycenter.catalog.dto.HairStockMovementResponse;
import com.omar.beautycenter.catalog.dto.HairStockResponse;
import com.omar.beautycenter.catalog.dto.InitializeHairStockRequest;
import com.omar.beautycenter.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
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
@RequestMapping("/api/v1/hair-stock")
@Tag(name = "Hair Product Stock")
@SecurityRequirement(name = "bearerAuth")
public class HairStockController {

    private final HairStockService stockService;

    public HairStockController(HairStockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/{hairItemId}")
    @Operation(summary = "Get hair-product stock", description = "Missing stock means the OWNER has not entered an opening balance yet.")
    public HairStockResponse get(@PathVariable @Positive Long hairItemId) {
        return stockService.getStock(hairItemId);
    }

    @PostMapping("/{hairItemId}/initialize")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enter the first trusted hair stock balance", description = "OWNER only; this is intentionally allowed once per hair product.")
    public HairStockResponse initialize(
            @PathVariable @Positive Long hairItemId,
            @Valid @RequestBody InitializeHairStockRequest request) {
        return stockService.initialize(hairItemId, request);
    }

    @PostMapping("/{hairItemId}/adjustments")
    @Operation(summary = "Adjust hair stock in grams", description = "OWNER only; positive adds stock and negative removes stock.")
    public HairStockResponse adjust(
            @PathVariable @Positive Long hairItemId,
            @Valid @RequestBody AdjustHairStockRequest request) {
        return stockService.adjust(hairItemId, request);
    }

    @GetMapping("/{hairItemId}/movements")
    @Operation(summary = "View auditable hair stock movements", description = "OWNER only")
    public PageResponse<HairStockMovementResponse> movements(
            @PathVariable @Positive Long hairItemId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return stockService.movements(hairItemId, page, size);
    }
}
