package com.omar.beautycenter.backup;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatabaseBackupRepository extends JpaRepository<DatabaseBackup, Long> {
    List<DatabaseBackup> findAllByOrderByStartedAtDesc();
    boolean existsByBackupTypeAndStatusAndStartedAtBetween(BackupType type, BackupRunStatus status, LocalDateTime from, LocalDateTime to);
}
