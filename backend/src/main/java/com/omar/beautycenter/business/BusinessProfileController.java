package com.omar.beautycenter.business;

import com.omar.beautycenter.business.dto.BusinessProfileResponse;
import com.omar.beautycenter.business.dto.PublicBusinessIdentityResponse;
import com.omar.beautycenter.business.dto.UpdateBusinessProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business-profile")
@Tag(name = "Business Profile")
@SecurityRequirement(name = "bearerAuth")
public class BusinessProfileController {

    private final BusinessProfileService service;

    public BusinessProfileController(BusinessProfileService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Get the business profile")
    public BusinessProfileResponse get() {
        return service.get();
    }

    @GetMapping("/public")
    @SecurityRequirements
    @Operation(summary = "Get the public business name and logo for local sign-in")
    public PublicBusinessIdentityResponse publicIdentity() {
        return service.getPublicIdentity();
    }

    @PutMapping
    @Operation(summary = "Update the business profile", description = "OWNER only")
    public BusinessProfileResponse update(@Valid @RequestBody UpdateBusinessProfileRequest request) {
        return service.update(request);
    }
}
