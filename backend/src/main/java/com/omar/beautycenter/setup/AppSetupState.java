package com.omar.beautycenter.setup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_setup_state")
public class AppSetupState {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "initial_owner_setup_completed", nullable = false)
    private boolean initialOwnerSetupCompleted;

    protected AppSetupState() {
    }

    public boolean isInitialOwnerSetupCompleted() {
        return initialOwnerSetupCompleted;
    }

    public void completeInitialOwnerSetup() {
        initialOwnerSetupCompleted = true;
    }
}
