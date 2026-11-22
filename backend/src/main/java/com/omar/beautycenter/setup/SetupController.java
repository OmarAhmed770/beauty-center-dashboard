package com.omar.beautycenter.setup;

import com.omar.beautycenter.setup.dto.CreateInitialOwnerRequest;
import com.omar.beautycenter.setup.dto.SetupStatusResponse;
import com.omar.beautycenter.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/setup")
@Tag(name = "First-run Setup")
public class SetupController {

    private final SetupService setupService;

    public SetupController(SetupService setupService) {
        this.setupService = setupService;
    }

    @GetMapping("/status")
    @SecurityRequirements
    @Operation(summary = "Check whether initial OWNER setup is required")
    public SetupStatusResponse status() {
        return setupService.status();
    }

    @PostMapping("/owner")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Create the first OWNER account exactly once")
    public UserResponse createInitialOwner(@Valid @RequestBody CreateInitialOwnerRequest request) {
        return setupService.createInitialOwner(request);
    }
}
