package com.omar.beautycenter.catalog;

import com.omar.beautycenter.catalog.dto.CatalogActiveRequest;
import com.omar.beautycenter.catalog.dto.CatalogCategoryResponse;
import com.omar.beautycenter.catalog.dto.CreateCatalogCategoryRequest;
import com.omar.beautycenter.catalog.dto.UpdateCatalogCategoryRequest;
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
@RequestMapping("/api/v1/catalog/categories")
@Tag(name = "Catalog Categories")
@SecurityRequirement(name = "bearerAuth")
public class CatalogCategoryController {

    private final CatalogCategoryService categoryService;

    public CatalogCategoryController(CatalogCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a catalog category", description = "Requires the Services and Prices permission")
    public CatalogCategoryResponse create(@Valid @RequestBody CreateCatalogCategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "Update a catalog category", description = "Requires the Services and Prices permission")
    public CatalogCategoryResponse update(
            @PathVariable @Positive Long categoryId,
            @Valid @RequestBody UpdateCatalogCategoryRequest request) {
        return categoryService.update(categoryId, request);
    }

    @PatchMapping("/{categoryId}/active")
    @Operation(summary = "Activate or deactivate a catalog category", description = "Requires the Services and Prices permission")
    public CatalogCategoryResponse setActive(
            @PathVariable @Positive Long categoryId,
            @Valid @RequestBody CatalogActiveRequest request) {
        return categoryService.setActive(categoryId, request.active());
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Get a catalog category")
    public CatalogCategoryResponse getById(@PathVariable @Positive Long categoryId) {
        return categoryService.getById(categoryId);
    }

    @GetMapping
    @Operation(summary = "Search catalog categories")
    public PageResponse<CatalogCategoryResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return categoryService.search(query, active, page, size);
    }
}
