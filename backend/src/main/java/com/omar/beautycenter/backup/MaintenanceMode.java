package com.omar.beautycenter.backup;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceMode {
    private final AtomicBoolean writeBlocked = new AtomicBoolean();
    public boolean isWriteBlocked() { return writeBlocked.get(); }
    public void runWithWritesBlocked(CheckedRunnable action) throws Exception {
        if (!writeBlocked.compareAndSet(false, true)) throw new IllegalStateException("A database restore is already running.");
        try { action.run(); } finally { writeBlocked.set(false); }
    }
    @FunctionalInterface public interface CheckedRunnable { void run() throws Exception; }
}
