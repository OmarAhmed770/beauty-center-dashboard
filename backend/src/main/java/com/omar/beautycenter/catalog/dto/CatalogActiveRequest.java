package com.omar.beautycenter.catalog.dto;

import jakarta.validation.constraints.NotNull;

public record CatalogActiveRequest(
        @NotNull(message = "Active status is required.")
        Boolean active) {
}
