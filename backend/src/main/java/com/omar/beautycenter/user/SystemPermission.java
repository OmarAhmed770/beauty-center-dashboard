package com.omar.beautycenter.user;

/**
 * Independently assignable application areas for staff accounts. OWNER is deliberately
 * not stored here: it retains unconditional access through {@link PermissionAccess}.
 */
public enum SystemPermission {
    DASHBOARD,
    ADD_ORDERS,
    EXPENSES,
    SERVICES_AND_PRICES,
    EMPLOYEES,
    DAILY_CLOSING,
    REPORTS,
    INVOICES_AND_RETURNS,
    SETTINGS,
    BACKUP_MANAGEMENT;

    public String authority() {
        return "PERMISSION_" + name();
    }
}
