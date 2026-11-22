package com.omar.beautycenter.discount;

import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.discount.dto.CreateDiscountRequest;
import com.omar.beautycenter.discount.dto.DiscountActiveRequest;
import com.omar.beautycenter.discount.dto.DiscountResponse;
import com.omar.beautycenter.discount.dto.UpdateDiscountRequest;
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
@RequestMapping("/api/v1/discounts")
@Tag(name = "Discounts")
@SecurityRequirement(name = "bearerAuth")
public class DiscountController {

    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a scheduled discount", description = "Requires the Settings permission; item and category scopes are mutually exclusive")
    public DiscountResponse create(@Valid @RequestBody CreateDiscountRequest request) {
        return discountService.create(request);
    }

    @PutMapping("/{discountId}")
    @Operation(summary = "Update a discount", description = "Requires the Settings permission")
    public DiscountResponse update(
            @PathVariable @Positive Long discountId,
            @Valid @RequestBody UpdateDiscountRequest request) {
        return discountService.update(discountId, request);
    }

    @PatchMapping("/{discountId}/active")
    @Operation(summary = "Activate or deactivate a discount", description = "Requires the Settings permission")
    public DiscountResponse setActive(
            @PathVariable @Positive Long discountId,
            @Valid @RequestBody DiscountActiveRequest request) {
        return discountService.setActive(discountId, request.active());
    }

    @GetMapping("/{discountId}")
    @Operation(summary = "Get a discount")
    public DiscountResponse getById(@PathVariable @Positive Long discountId) {
        return discountService.getById(discountId);
    }

    @GetMapping
    @Operation(summary = "Search discounts", description = "Filters schedule, scope, type, active state, and name")
    public PageResponse<DiscountResponse> search(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) DiscountType type,
            @RequestParam(required = false) @Positive Long catalogItemId,
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false) LocalDate applicableDate,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return discountService.search(
                active,
                type,
                catalogItemId,
                categoryId,
                applicableDate,
                query,
                page,
                size);
    }
}
