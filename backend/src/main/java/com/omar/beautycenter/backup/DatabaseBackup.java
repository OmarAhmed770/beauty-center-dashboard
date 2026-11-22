package com.omar.beautycenter.backup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "database_backups")
public class DatabaseBackup {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "file_name", nullable = false, unique = true, length = 500)
    private String fileName;
    @Enumerated(EnumType.STRING) @Column(name = "backup_type", nullable = false, length = 20)
    private BackupType backupType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private BackupRunStatus status;
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;
    @Column(length = 64)
    private String sha256;
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    protected DatabaseBackup() { }
    public DatabaseBackup(String fileName, BackupType type, LocalDateTime startedAt, String createdBy) {
        this.fileName = fileName; this.backupType = type; this.startedAt = startedAt; this.createdBy = createdBy;
        this.status = BackupRunStatus.RUNNING;
    }
    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public BackupType getBackupType() { return backupType; }
    public BackupRunStatus getStatus() { return status; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public String getSha256() { return sha256; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void succeed(long size, String checksum, LocalDateTime completedAt) { this.status = BackupRunStatus.SUCCESS; this.fileSizeBytes = size; this.sha256 = checksum; this.completedAt = completedAt; this.errorMessage = null; }
    public void fail(String message, LocalDateTime completedAt) { this.status = BackupRunStatus.FAILED; this.errorMessage = message; this.completedAt = completedAt; }
}
