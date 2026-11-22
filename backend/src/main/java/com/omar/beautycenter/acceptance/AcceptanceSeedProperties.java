package com.omar.beautycenter.acceptance;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.acceptance-seed")
public record AcceptanceSeedProperties(
        boolean enabled,
        String ownerUsername,
        String ownerPassword,
        String fullStaffUsername,
        String fullStaffPassword,
        String receptionistUsername,
        String receptionistPassword) {
}
