package com.omar.beautycenter.catalog;

import com.omar.beautycenter.catalog.dto.PriceHistoryResponse;
import com.omar.beautycenter.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/price-history")
@Tag(name = "Price History")
@SecurityRequirement(name = "bearerAuth")
public class PriceHistoryController {

    private final CatalogItemService itemService;

    public PriceHistoryController(CatalogItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    @Operation(summary = "Search immutable catalog price history", description = "Requires services and prices permission")
    public PageResponse<PriceHistoryResponse> search(
            @RequestParam(required = false) @Positive Long catalogItemId,
            @RequestParam(required = false) PriceValueType valueType,
            @RequestParam(required = false) LocalDateTime fromDateTime,
            @RequestParam(required = false) LocalDateTime toDateTime,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return itemService.searchPriceHistory(
                catalogItemId, valueType, fromDateTime, toDateTime, page, size);
    }
}
