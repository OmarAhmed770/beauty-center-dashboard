package com.omar.beautycenter.catalog;

import com.omar.beautycenter.catalog.dto.CatalogActiveRequest;
import com.omar.beautycenter.catalog.dto.CreateHairConfigurationRequest;
import com.omar.beautycenter.catalog.dto.HairCalculationPreviewRequest;
import com.omar.beautycenter.catalog.dto.HairCalculationPreviewResponse;
import com.omar.beautycenter.catalog.dto.HairConfigurationResponse;
import com.omar.beautycenter.catalog.dto.HairPriceOverrideAuditResponse;
import com.omar.beautycenter.common.dto.PageResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/hair-configurations")
@Tag(name = "Hair Installation Configuration")
@SecurityRequirement(name = "bearerAuth")
public class HairConfigurationController {

    private final HairConfigurationService configurationService;
    private final HairInstallationCalculationService calculationService;

    public HairConfigurationController(
            HairConfigurationService configurationService,
            HairInstallationCalculationService calculationService) {
        this.configurationService = configurationService;
        this.calculationService = calculationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an effective-dated hair configuration", description = "Requires the Services and Prices permission; create a new row for changed pricing instead of rewriting prior configuration")
    public HairConfigurationResponse create(@Valid @RequestBody CreateHairConfigurationRequest request) {
        return configurationService.create(request);
    }

    @PatchMapping("/{configurationId}/active")
    @Operation(summary = "Activate or deactivate a hair configuration", description = "Requires the Services and Prices permission")
    public HairConfigurationResponse setActive(
            @PathVariable @Positive Long configurationId,
            @Valid @RequestBody CatalogActiveRequest request) {
        return configurationService.setActive(configurationId, request.active());
    }

    @GetMapping("/{configurationId}")
    @Operation(summary = "Get a hair configuration")
    public HairConfigurationResponse getById(@PathVariable @Positive Long configurationId) {
        return configurationService.getById(configurationId);
    }

    @GetMapping
    @Operation(summary = "Search hair configurations", description = "Filters item, active state, effective date, and flexible attribute text")
    public PageResponse<HairConfigurationResponse> search(
            @RequestParam(required = false) @Positive Long hairItemId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) LocalDate effectiveOn,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return configurationService.search(hairItemId, active, effectiveOn, query, page, size);
    }

    @PostMapping("/preview")
    @Operation(summary = "Preview a hair-installation calculation", description = "Formula assumption: quantity times unit selling price, plus one installation fee, then optional discount. Overrides require the Services and Prices permission and are audited.")
    public HairCalculationPreviewResponse preview(
            @Valid @RequestBody HairCalculationPreviewRequest request) {
        return calculationService.preview(request);
    }

    @GetMapping("/price-overrides")
    @Operation(summary = "Search price-override audit history", description = "Requires the Services and Prices permission")
    public PageResponse<HairPriceOverrideAuditResponse> searchOverrides(
            @RequestParam(required = false) @Positive Long configurationId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return configurationService.searchOverrides(configurationId, page, size);
    }
}
