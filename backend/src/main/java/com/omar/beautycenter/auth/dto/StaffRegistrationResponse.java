package com.omar.beautycenter.auth.dto;

import com.omar.beautycenter.user.RegistrationStatus;

public record StaffRegistrationResponse(
        String username,
        RegistrationStatus registrationStatus) {
}
