package com.omar.beautycenter.catalog;

import com.omar.beautycenter.catalog.dto.CatalogActiveRequest;
import com.omar.beautycenter.catalog.dto.CatalogItemResponse;
import com.omar.beautycenter.catalog.dto.CreateCatalogItemRequest;
import com.omar.beautycenter.catalog.dto.UpdateCatalogItemRequest;
import com.omar.beautycenter.catalog.dto.UpdateCatalogPriceRequest;
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
@RequestMapping("/api/v1/catalog/items")
@Tag(name = "Catalog Items")
@SecurityRequirement(name = "bearerAuth")
public class CatalogItemController {

    private final CatalogItemService itemService;

    public CatalogItemController(CatalogItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a catalog item", description = "Requires the Services and Prices permission")
    public CatalogItemResponse create(@Valid @RequestBody CreateCatalogItemRequest request) {
        return itemService.create(request);
    }

    @PutMapping("/{itemId}")
    @Operation(summary = "Update catalog item metadata", description = "Code, type, prices, and cost are excluded; use the price endpoint for financial changes")
    public CatalogItemResponse update(
            @PathVariable @Positive Long itemId,
            @Valid @RequestBody UpdateCatalogItemRequest request) {
        return itemService.update(itemId, request);
    }

    @PostMapping("/{itemId}/prices")
    @Operation(summary = "Update selling price and/or cost", description = "Requires the Services and Prices permission; every changed value creates immutable history")
    public CatalogItemResponse updatePrices(
            @PathVariable @Positive Long itemId,
            @Valid @RequestBody UpdateCatalogPriceRequest request) {
        return itemService.updatePrices(itemId, request);
    }

    @PatchMapping("/{itemId}/active")
    @Operation(summary = "Activate or deactivate a catalog item", description = "Requires the Services and Prices permission")
    public CatalogItemResponse setActive(
            @PathVariable @Positive Long itemId,
            @Valid @RequestBody CatalogActiveRequest request) {
        return itemService.setActive(itemId, request.active());
    }

    @GetMapping("/{itemId}")
    @Operation(summary = "Get a catalog item")
    public CatalogItemResponse getById(@PathVariable @Positive Long itemId) {
        return itemService.getById(itemId);
    }

    @GetMapping
    @Operation(summary = "Search catalog items", description = "Filters category, type, active state, and bilingual text")
    public PageResponse<CatalogItemResponse> search(
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false) CatalogItemType itemType,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return itemService.search(categoryId, itemType, active, query, page, size);
    }
}
