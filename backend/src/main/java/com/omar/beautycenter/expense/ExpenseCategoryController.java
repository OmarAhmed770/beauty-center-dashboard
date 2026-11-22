package com.omar.beautycenter.expense;

import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.expense.dto.CreateExpenseCategoryRequest;
import com.omar.beautycenter.expense.dto.ExpenseCategoryActiveRequest;
import com.omar.beautycenter.expense.dto.ExpenseCategoryResponse;
import com.omar.beautycenter.expense.dto.UpdateExpenseCategoryRequest;
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
@RequestMapping("/api/v1/expense-categories")
@Tag(name = "Expense Categories")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseCategoryController {

    private final ExpenseCategoryService categoryService;

    public ExpenseCategoryController(ExpenseCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an expense category", description = "Requires the Settings permission")
    public ExpenseCategoryResponse create(
            @Valid @RequestBody CreateExpenseCategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "Update an expense category", description = "Requires the Settings permission")
    public ExpenseCategoryResponse update(
            @PathVariable @Positive Long categoryId,
            @Valid @RequestBody UpdateExpenseCategoryRequest request) {
        return categoryService.update(categoryId, request);
    }

    @PatchMapping("/{categoryId}/active")
    @Operation(summary = "Activate or deactivate an expense category", description = "Requires the Settings permission")
    public ExpenseCategoryResponse setActive(
            @PathVariable @Positive Long categoryId,
            @Valid @RequestBody ExpenseCategoryActiveRequest request) {
        return categoryService.setActive(categoryId, request.active());
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Get an expense category", description = "Available to all authenticated roles")
    public ExpenseCategoryResponse getById(@PathVariable @Positive Long categoryId) {
        return categoryService.getById(categoryId);
    }

    @GetMapping
    @Operation(summary = "Search expense categories", description = "Available to all authenticated roles")
    public PageResponse<ExpenseCategoryResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return categoryService.search(query, active, page, size);
    }
}
