package com.omar.beautycenter.role;

public enum Role {
    OWNER,
    /**
     * Kept only to deserialize pre-V16 data while Flyway converts every ADMIN row to
     * a staff account with explicit permissions. It is never assignable by the API.
     */
    @Deprecated(forRemoval = false)
    ADMIN,
    RECEPTIONIST;

    public String authority() {
        return "ROLE_" + name();
    }
}
